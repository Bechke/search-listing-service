# Admin Panel

Documents the admin-web feature end to end: what shipped, which service owns which piece, and what's
still open. The feature spans three repos — `gateway-service` (identity/account ops),
`search-listing-service` (this repo — listing moderation + user/plan management), and `payment-service`
(refunds) — fronted by `admin-web`.

---

## Roles

Two new Keycloak realm roles (`gateway-service/keycloak/realm-export.json`):

| Role | Grants |
|------|--------|
| `ADMIN` | Everything in this document — listing moderation, seller/plan management, refunds |
| `SUPER_ADMIN` | Singleton role. Only purpose: provisioning new `ADMIN` accounts via `POST /admin/accounts/super`. Enforced server-side (one holder at a time, granting to a second account returns 409) — Keycloak itself doesn't know it's a singleton. |

**Bootstrap problem:** the very first `SUPER_ADMIN` can't be created through the API, since creating
one requires already holding `SUPER_ADMIN`. It has to be granted directly via Keycloak's own Admin API
(master realm `admin`/`admin` credentials) or the Keycloak admin console, once, by hand. Every
`ADMIN` account after that can be self-service provisioned by whoever holds `SUPER_ADMIN`.

---

## Who owns what

| Concern | Owner | Why |
|---|---|---|
| Create/suspend/reactivate admin accounts, grant/revoke realm roles | `gateway-service` — `AdminAccountController` | These are Keycloak Admin API operations; the gateway already has the service-account credentials and `realm-management/manage-users` grant for registration. No other service touches Keycloak directly. |
| Listing moderation queue (pending/approve/reject) | `search-listing-service` — `AdminController` | This repo already owns the `advertisement`/`vehicle` tables that back the review queue. |
| Seller + plan view, manual plan override | `search-listing-service` — `AdminUserController` | Same reasoning — `person`/`user_plans` tables live here. |
| Refunds | `payment-service` — `PaymentController`'s refund endpoints | Owns `payment_transaction`/`refund` and the Razorpay integration. |

All four are gated at the gateway with `hasRole("ADMIN")` (see `SecurityConfig`), and every controller
here additionally re-checks the forwarded `X-User-Roles` header itself — two independent layers, not
just "trust the gateway did it."

---

## Gateway routing

```
/admin/accounts/**  → gateway-service itself (no rewrite)         — AdminAccountController
/admin/**            → search-listing-service, /api/v1/admin/**   — AdminController + AdminUserController
```

`/admin/accounts/**` is registered *before* the `/admin/**` catch-all in
`gateway-service/src/main/resources/application.yml` — Spring Cloud Gateway matches routes in
declaration order, so if that ordering were ever flipped, every admin-accounts call would silently
get swallowed by the search-listing-service route instead (404, not an auth error — easy to miss).
`POST /admin/accounts/super` additionally requires `SUPER_ADMIN`, checked before the general `ADMIN`
rule in `SecurityConfig`.

Payment refund endpoints are reached at `/payments/**` → payment-service, and are `ADMIN`-gated with
`@PreAuthorize` *inside* `PaymentController` itself — the gateway has no role rule for `/payments/**`
at all (it's just `anyExchange().authenticated()`), so that check exists nowhere else.

---

## Listing lifecycle (this repo's `AdminController`)

```
POST /listing (mobile)
  → status = PENDING_REVIEW                      (or PENDING_PAYMENT if the seller is over their plan limit)
  → notification-service sends LISTING_SUBMITTED / LISTING_PENDING_PAYMENT

  ── admin reviews via GET /api/v1/admin/listings/pending ──

  → POST /api/v1/admin/listings/approve   { listingIds }             → status = ACTIVE
  → POST /api/v1/admin/listings/reject    { listingIds, reason? }    → status = REJECTED, reason stored
```

Both approve and reject go through `AdminService.updateStatus()`, which:
1. Updates `Advertisement` and `Vehicle` rows in this DB.
2. Republishes the **full** listing (not just the status field) as a `vehicle-ads` `UPDATE` event —
   deliberately, so `VehicleConsumerListener`'s upsert on the other end doesn't null out
   brand/year/price/etc. when it re-consumes the event.

`notification-service`'s `VehicleAdConsumer` is already fully wired for this — `ACTIVE` →
`LISTING_APPROVED`, `REJECTED` → `LISTING_REJECTED` (with the reason appended to the message), plus
`LISTING_EXPIRED` / `LISTING_SOLD` / the `PENDING_PAYMENT` → `PENDING_REVIEW` release path
(`AdminService.releaseFromPendingPayment`, called by `PaymentEventConsumer` once a blocked seller's
plan upgrade lands). This is one of the *working* Kafka pipelines in the workspace — contrast with
`property-ad-events`/`electronics-ad-events`, which nothing consumes yet.

---

## Endpoints

**Listings** (`AdminController`, `/api/v1/admin`)
```
GET  /listings/pending?page=&size=       paginated PENDING_REVIEW queue — AdminListingSummary rows
GET  /listings/{vehicleSourceId}          full vehicle detail — AdminListingDetail
POST /listings/approve  { listingIds }
POST /listings/reject   { listingIds, reason? }
```
`Advertisement.person` / `Vehicle.person` are `@JsonIgnore`'d (LAZY, not meant for general API
consumers), so the raw entities can't surface who posted a listing — `AdminController` maps into
`AdminListingSummary`/`AdminListingDetail` instead, adding `sellerKeycloakId`/`sellerName`/
`sellerEmail`/`sellerMobile` (falls back the same way the rest of the app does when `fullName` is
empty — Kafka-stub sellers, see the search-listing-service section of the workspace `CLAUDE.md`).
`AdminListingDetail` also parses `Vehicle.imageUrlsJson` into a real `imageUrls: string[]` — the raw
entity only exposes it as an opaque JSON string. admin-web's `ListingsPage` shows the seller in a
"Posted By" column and renders the full gallery as a horizontal-scrolling thumbnail strip in the
detail panel (falls back to just `defaultImgPath` for older listings saved before this endpoint
carried the array). Clicking a thumbnail opens a full-size lightbox popout — dark overlay, prev/next
arrows, an N/total counter, wraps around at the ends, closes on the ✕ button, backdrop click, or
Escape, and Left/Right arrow keys step through the gallery while it's open. Same pattern
bechke-mobile already uses for its vehicle-detail gallery (peek carousel + lightbox), just built for
the web with plain CSS scroll-snap instead of a native `ScrollView`.

**Users & plans** (`AdminUserController`, `/api/v1/admin/users`)
```
GET /                                     paginated sellers + plan + active listing count
PUT /{keycloakId}/plan                    { planName, listingLimit?, boostEnabled?, validUntil? }
                                           manual override, bypasses payment (comps/corrections)
```

**Accounts** (gateway-service's `AdminAccountController`, `/admin/accounts` — see that repo for the
full writeup; listed here only for cross-reference since admin-web calls all of these from one screen)
```
POST   /super                             SUPER_ADMIN only — create a new ADMIN account
PUT    /{keycloakId}/status               suspend / reactivate
GET    /{keycloakId}/roles
POST   /{keycloakId}/roles                grant
DELETE /{keycloakId}/roles/{role}         revoke
```

---

## Tested end to end (2026-08-22)

Walked the full flow against a live local stack: `SUPER_ADMIN` login → create a new `ADMIN` account via
`POST /admin/accounts/super` → login as the new account → suspend it (confirmed the suspended account's
own login then fails with `invalid_grant`) → reactivate → grant/revoke `business_user` → confirmed
granting a second `SUPER_ADMIN` returns 409 → confirmed a non-`SUPER_ADMIN` `ADMIN` gets 403 from
`POST /admin/accounts/super` → `GET /admin/listings/pending` and `GET /admin/users` both return real
data through the gateway's `/admin/**` catch-all into this service.

Also walked the plans/quota/boost path this same session: created 3 listings (fills the FREE quota of
3) → 4th listing correctly resolved to `PENDING_PAYMENT` instead of `PENDING_REVIEW` → paid for a
`BASIC` upgrade → confirmed the blocked listing was released back to `PENDING_REVIEW` → paid for a
`LISTING_BOOST` on a listing → confirmed `boosted=true` with `boostedUntil` 7 days out → rejected a
listing with a reason via the admin queue → confirmed `rejectionReason` is surfaced back on the
seller's own `GET /ads/my`.

Separately re-walked the same plan-upgrade/quota-release flow through **org context** (personal-only
the first time): created a business account + org through the real signup/org-setup UI, posted 5 org
listings (fills `PlanLimits.ORG_LISTING_LIMITS.FREE = 5`), confirmed the 6th resolved to
`PENDING_PAYMENT`, then upgraded the org to `BASIC` (₹2,999/75 listings — the org price/limit list is
genuinely different from personal's ₹499/50, confirmed end-to-end from `UpgradePlanScreen`'s
`context=org` down through `PlanLimits.orgLimitFor`) via the real "Organization → Subscription" UI
flow. `Organization.subscriptionTier` updated to `BASIC` and the blocked listing released and stuck —
confirming the transaction-ordering fix above also covers `activateOrgPlan` /
`releaseOrgFromPendingPayment`, not just the personal path it was originally caught and fixed on.

**Bug found and fixed during this pass:** `releaseFromPendingPayment`/`releaseOrgFromPendingPayment`
(via `AdminService.updateStatus`) published its `vehicle-ads` Kafka event synchronously, before the
surrounding `@Transactional` committed. `VehicleConsumerListener` re-consumes that same topic and,
for a `PENDING_REVIEW` status, re-runs the quota check — which could race ahead of the commit and read
the seller's *pre-upgrade* plan under `READ_COMMITTED`, silently flipping the just-released listing
straight back to `PENDING_PAYMENT`. Reproduced consistently in logs (including with a correct plan
name — this wasn't just the entityId mixup below, see the two log excerpts a few hundred ms apart).
Fixed by deferring the Kafka publish to `TransactionSynchronization.afterCommit()` in
`AdminService.publishAfterCommit()`; re-tested and the release now sticks. This means: **before this
fix, a seller who paid to upgrade their plan would not actually see their blocked listings released**
— worth knowing if this ever needs to be reasoned about historically (e.g. "why did support get
reports of stuck listings despite an upgrade").

**Also worth knowing (not a bug, but a footgun):** `OrderRequest.entityId` for a `SUBSCRIPTION_UPGRADE`
must be the plan's **`name`** (`"FREE"` / `"BASIC"` / `"PREMIUM"`), not its numeric `id` from
`GET /payments/plans` (which returns `{"id":2,"name":"BASIC",...}`). `PaymentEventConsumer` uppercases
`entityId` and looks it up directly in `PlanLimits`'s name-keyed maps — a numeric id like `"2"` doesn't
match anything and silently falls back to the FREE-tier default (limit 3, no boost) instead of erroring.
Confirmed this by sending `entityId: "2"` deliberately: the seller's plan got set to a literal
`planName: "2"` with FREE-tier limits, no error anywhere. A field named `entityId`/documented as
"planId" strongly invites sending the numeric id — this is exactly the mistake a real API consumer
(or a first draft of admin-web / mobile's upgrade screen) is likely to make.

---

## Featured slots + listing boost (added after the pass above)

`SubscriptionPlan.featuredSlots` (payment-service) had existed since the original plan schema but was
dead — nothing tracked it here, and `Advertisement.boosted`/`boostedUntil` (set by `PaymentEventConsumer`
on a captured `LISTING_BOOST` payment) was itself dead in the other direction: nothing capped how many
boosts a seller could hold, and nothing read `boosted` back out for display. Both are now wired up.

**Slot model: concurrent capacity, not a periodic allowance.** `featured_slots` = max listings a seller
may have boosted *at once*. A boost occupies a slot for its purchased duration and the slot self-frees
once `boosted_until` passes — "currently featured" is always computed as
`boosted = true AND boosted_until > NOW()` at query time, never a separately-maintained counter. No
scheduled job exists or is needed for this.

**New in this pass:**
- `user_plans.featured_slots` (`V15__add_featured_slots.sql`) — mirrors payment-service's
  `subscription_plans.featured_slots` per tier via `PlanLimits.personalFeaturedSlotsFor`/
  `orgFeaturedSlotsFor`, set alongside `listingLimit`/`boostEnabled` in
  `PaymentEventConsumer.activatePersonalPlan`/`activateOrgPlan`. **Not yet exposed on the admin manual
  plan-override endpoint** (`PUT /admin/users/{keycloakId}/plan`) — an admin comping a plan today can't
  set `featuredSlots` directly, it stays at whatever the seller's last real upgrade set (or 0). Same
  shape as the existing `PlanOverrideRequest`/`overridePlan` pattern if this needs closing later.
- `PaymentEventConsumer.activateBoost` now gates on remaining capacity before setting `boosted=true` —
  counts the seller's (or their org's, if the listing has one) other currently-boosted ads and skips
  activation if already at `featuredSlots`. **Known limitation:** this check only runs at
  `PAYMENT_CAPTURED` time — there's no synchronous pre-payment check across payment-service and this
  service (they only talk via Kafka), so a payment can in principle succeed and still not apply if a
  slot filled in the race. Mobile mitigates the common case with a `GET /ads/my/quota` pre-flight before
  showing the boost button; a real fix (refund-on-reject, or a synchronous cross-service check) is a
  follow-up.
- Boost duration/pricing is fixed server-side, not client-settable: payment-service's
  `PaymentService.BOOST_PRICE_PAISE_BY_DAYS` maps `{3: ₹99, 7: ₹199, 30: ₹599}` — `OrderRequest`'s
  `amountPaise` is ignored for `LISTING_BOOST`. The chosen duration rides through on `PaymentEvent`
  (`boostDurationDays`) and `PaymentEventConsumer.activateBoost` uses it for `boostedUntil`, falling
  back to the historical 7-day `BOOST_DAYS` constant only if the field is absent (older in-flight
  messages).
- **Boost-as-add-on:** a `SUBSCRIPTION_UPGRADE` order can now carry `addonBoostListingId` +
  `boostDurationDays` to also boost one listing in the same payment (one Razorpay order, amount = plan
  price + boost price). `PaymentEventConsumer.handleCaptured` activates the plan first, then attempts
  the add-on boost — deliberately in that order, so the capacity check sees the *new* plan's
  (higher) `featuredSlots`, not the pre-upgrade one.
- `GET /ads/my/quota` (`AdvertisementService.getQuota`) now also returns `featuredSlots`,
  `featuredSlotsUsed`, `featuredSlotsAvailable` — same self-service pattern as the existing
  `listingLimit`/`atLimit` fields, personal listings only (org quota still isn't surfaced here, same
  pre-existing gap as the listing-limit side).
- New read endpoint for the mobile "Featured" rail: `GET /api/v1/vehicles/featured?subCategory=&limit=`
  (gateway: `GET /listings/featured`) — joins `Advertisement` (`boosted=true`, not expired) to `Vehicle`
  by `vehicleSourceId` (the two aren't JPA-mapped to each other, just share that string key), ordered by
  `boostedUntil DESC`. Scoped to vehicle listings only — property/electronics don't reach this service
  yet (see the workspace `CLAUDE.md`'s Kafka topics gap), so they have no boost/featured path either.
- Fixed a stale doc comment while touching this: `PaymentEvent.java`'s javadoc (both here and in
  payment-service/notification-service's copies) referenced a `seller-admin-service` that doesn't exist
  anywhere in this workspace — it's this service's `PaymentEventConsumer` that actually does the
  activation. Corrected to say so.

Mobile-side: `MyListingsScreen` gained a per-listing "Boost" action (duration picker, blocked when
`featuredSlotsAvailable` is 0) and `UpgradePlanScreen` gained the add-on picker described above; a new
"Featured" horizontal rail sits above the vertical listings on the home screen's per-sub-category view.

---

## Known gaps / next steps

- **No audit trail.** Approve/reject/refund/role-grant are all logged via `log.info(...)` only —
  nothing durable records *who* took an admin action and *when*. Fine for a single-admin dev setup;
  worth a real `admin_action_log` table before there's more than one admin.
- **No way to fully remove an admin account**, only suspend it (`enabled=false`). If that's
  insufficient later, it'd be a new `DELETE /admin/accounts/{keycloakId}` on the gateway, calling
  Keycloak's user-delete Admin API endpoint.
- **`SUPER_ADMIN` bootstrap is manual and undocumented in any script** — see the callout above. If this
  needs to be reproducible (e.g. a fresh environment, CI), it should become a step in
  `run-bechke-stack`'s driver rather than tribal knowledge.
- **No pagination/filtering on the admin-accounts side** (`GET /{keycloakId}/roles` is single-user
  only) — fine while the admin count is small; would need a `GET /admin/accounts` list endpoint if
  that grows.
- **Payment refunds aren't cross-linked from this repo's admin views** — `AdminUserController`'s seller
  view shows plan/listing data but not their transaction/refund history; an admin currently has to
  jump to a separate payments admin screen. Worth a join if admin-web's refunds page and users page
  end up wanting the same seller context.
