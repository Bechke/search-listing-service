-- ─────────────────────────────────────────────────────────────────
-- V15 : Track each seller's featured-slot entitlement
--
-- Mirrors payment-service's subscription_plans.featured_slots (see
-- PlanLimits.featuredSlotsFor). This is a concurrency cap, not a
-- periodic allowance: at most featured_slots of a seller's listings
-- may be boosted (advertisement.boosted=true AND boosted_until in the
-- future) at any one time. A slot self-frees once boosted_until
-- passes — there is no counter to reset and no scheduled job.
-- ─────────────────────────────────────────────────────────────────
ALTER TABLE user_plans
    ADD COLUMN featured_slots INT NOT NULL DEFAULT 0;
