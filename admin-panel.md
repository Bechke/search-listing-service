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
