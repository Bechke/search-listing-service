# Listing Status Lifecycle & Payment Flow

How a listing's status changes over its life, and how payment-service's Kafka
events drive plan/quota/boost state in this repo. Scope: this is
search-listing-service's view — the transaction-level statuses
(`CREATED`/`CAPTURED`/`FAILED`/...) belong to payment-service and are only
summarized here for cross-reference (see the bottom section).

---

## Listing status values

| Status | Set by | Meaning |
|---|---|---|
| `PENDING_REVIEW` | `VehicleConsumerListener` (intake), `AdminService.releaseFromPendingPayment`/`releaseOrgFromPendingPayment` | Waiting in the admin queue (`GET /admin/listings/pending`) |
| `PENDING_PAYMENT` | `VehicleConsumerListener` (intake, quota exceeded) | Blocked — seller/org is at their plan's listing limit |
| `ACTIVE` | `AdminService.bulkApprove` | Live, publicly visible |
| `REJECTED` | `AdminService.bulkReject` | Rejected with a reason; resubmitting (edit + re-post) sends it back through intake as `PENDING_REVIEW` |
| `DELETED` | `VehicleConsumerListener.handleDelete` | Soft-deleted (owner deleted the listing) |
| `SOLD`, `EXPIRED` | *(recognized, not automated)* | Excluded from active-listing-count everywhere (`AdvertisementService`/`AdminUserService`'s `INACTIVE_STATUSES`), but nothing in this repo or vehicle-service actually transitions a listing into either state — an owner would have to `PUT /vehicle/{id}` with the status set directly. No expiry job, no "mark as sold" endpoint exist today. |

## Intake: how a new/resubmitted listing gets its real status

vehicle-service always sends `PENDING_REVIEW` on create or resubmit — it has
no visibility into plan quotas. `VehicleConsumerListener` (topic
`vehicle-ads`, group `vehicle-ads-consumer-group`) is what actually decides:

```
VehicleConsumerListener.consumeVehicleAd(dto)
  if dto.status == "PENDING_REVIEW":
      resolvedStatus = AdvertisementService.resolveIntakeStatus(keycloakId, orgId, orgTier)
        → counts active listings (excludes REJECTED/SOLD/EXPIRED) against the plan limit
          (UserPlan.listingLimit for personal, PlanLimits.orgLimitFor(tier) for org)
        → "PENDING_PAYMENT" if at/over the limit, else "PENDING_REVIEW"
  else:
      resolvedStatus = dto.status   # ACTIVE/REJECTED/etc. from AdminService pass through unchanged —
                                     # an explicit state transition, not an intake
  → upserts Vehicle + Advertisement with resolvedStatus
```

A resubmit or fresh approval also clears any prior `rejectionReason`
(`VehicleConsumerListener`, `AdminService.updateStatus`) — it's no longer
relevant once the listing moves on.

## Admin review: PENDING_REVIEW → ACTIVE / REJECTED

`AdminService.bulkApprove`/`bulkReject` (backing `POST /admin/listings/approve`
and `/reject`) both go through the same private `updateStatus()`:

1. Update `Advertisement` and `Vehicle` rows directly in this DB.
2. Republish the **full** listing (not just the status field) as a `vehicle-ads`
   `UPDATE` event, deferred to `TransactionSynchronization.afterCommit()` —
   publishing mid-transaction let `VehicleConsumerListener`'s re-consume race
   ahead of the commit and read stale plan data under `READ_COMMITTED`
   (concretely: a just-released `PENDING_PAYMENT` → `PENDING_REVIEW` write
   getting silently flipped back by the listener re-running the quota check
   against the pre-commit plan). See `AdminService.publishAfterCommit`'s
   javadoc for the full incident.
3. `notification-service`'s `VehicleAdConsumer` reacts to the republished
   event: `ACTIVE` → `LISTING_APPROVED`, `REJECTED` → `LISTING_REJECTED`
   (reason appended to the message body).

The full listing is republished (not a bare status patch) specifically so
`VehicleConsumerListener`'s upsert doesn't null out brand/year/price/etc.
when it re-consumes the event.

## Payment events: PAYMENT_CAPTURED → plan activation / boost / release

Published by payment-service to topic `payment-events`, consumed here by
`PaymentEventConsumer` (group `search-listing-payment-group` — separate from
notification-service's group so both services see every message
independently).

```
PaymentEventConsumer.consume(event)
  case eventType == "PAYMENT_CAPTURED":
    case purpose == "SUBSCRIPTION_UPGRADE":
      if event.organizationId set:
          activateOrgPlan(orgId, planName)       # Organization.subscriptionTier = planName
      else:
          activatePersonalPlan(keycloakId, planName)  # upsert UserPlan: listingLimit,
                                                          boostEnabled, featuredSlots
                                                          (all from PlanLimits, mirroring
                                                          payment-service's subscription_plans)
      → AdminService.releaseFromPendingPayment / releaseOrgFromPendingPayment
        (PENDING_PAYMENT listings → PENDING_REVIEW — back in the admin queue,
        not auto-approved)
      if event also carries listingId + boostDurationDays (add-on purchase):
          activateBoost(...)   # see below — runs AFTER plan activation, so its
                                # capacity check sees the NEW (higher) featuredSlots
    case purpose == "LISTING_BOOST":
      activateBoost(sellerId, listingId, boostDurationDays)
  case eventType == "PAYMENT_SUCCESSFUL" (legacy):
      Advertisement.status = "PENDING_REVIEW"   # kept for backwards compatibility
  default: ignored (PAYMENT_FAILED, REFUND_* — handled by notification-service only)
```

**`activateBoost`** — sets `Advertisement.boosted=true` and
`boostedUntil = now + boostDurationDays` (3/7/30, priced and validated
server-side by payment-service — never client-supplied), but only if the
seller (or their org, if the listing has one) has a free slot: counts other
currently-boosted, non-expired ads and compares against `featuredSlots`. If
already at capacity, the payment stays captured but the boost silently
doesn't apply (logged as a warning) — there's no synchronous pre-payment
check across payment-service and this service, only the mobile client's
`GET /ads/my/quota` pre-flight. "Currently featured" is always computed as
`boosted=true AND boostedUntil>NOW()` at query time — no scheduled job resets
or expires it.

---

## Cross-reference: payment-service's transaction/refund statuses

Not owned by this repo, but referenced by the flow above:

- `PaymentTransaction.status`: `CREATED` → `CAPTURED` | `FAILED` →
  `REFUND_INITIATED` → `REFUNDED`. `CAPTURED` is what triggers everything in
  this document via the `PAYMENT_CAPTURED` Kafka event.
- `RefundEntity.status`: `INITIATED` → `PROCESSED` | `REJECTED`. Refund
  events (`REFUND_INITIATED`/`REFUND_PROCESSED`/`REFUND_REJECTED`) are
  consumed only by notification-service (email) — this repo ignores them
  entirely; a refund never reverses a plan upgrade or un-boosts a listing.
- Order creation in `PENDING`/simulation mode, real Razorpay Checkout vs.
  `POST /payments/simulate/{orderId}/complete`, and the documented
  `/payments/webhook` auth gap all live in payment-service — see the
  workspace `CLAUDE.md`'s payment-service section.
