-- ─────────────────────────────────────────────────────────────────
-- V12 : Plan enforcement + listing boost support
-- ─────────────────────────────────────────────────────────────────

-- Stores the active subscription plan for each seller.
-- Upserted by the PaymentEventConsumer when PAYMENT_CAPTURED arrives
-- with purpose=SUBSCRIPTION_UPGRADE.
-- New sellers who have never paid start on the FREE plan (5 listings).
CREATE TABLE IF NOT EXISTS user_plans (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    keycloak_id     VARCHAR(255) NOT NULL UNIQUE,   -- Keycloak sub
    plan_name       VARCHAR(50)  NOT NULL DEFAULT 'FREE', -- FREE | BASIC | PREMIUM
    listing_limit   INT          NOT NULL DEFAULT 5,
    boost_enabled   TINYINT(1)   NOT NULL DEFAULT 0,
    valid_until     DATE,                            -- NULL = no expiry (FREE plan)
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Boost columns on the advertisement table.
-- boosted       = true while the listing should appear at the top.
-- boosted_until = when the boost expires; a scheduled job / query can clear it.
ALTER TABLE advertisement
    ADD COLUMN boosted       TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN boosted_until DATETIME   NULL;
