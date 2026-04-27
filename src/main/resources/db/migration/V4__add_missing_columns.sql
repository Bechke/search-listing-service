-- V4: Add columns that exist in the JPA entities but were missing from V3 DDL.
-- Flyway runs each migration exactly once, so no IF NOT EXISTS needed.

ALTER TABLE person
    ADD COLUMN keycloak_id VARCHAR(255) NULL UNIQUE;

ALTER TABLE vehicle
    ADD COLUMN vehicle_source_id VARCHAR(255) NULL UNIQUE,
    ADD COLUMN image_urls_json TEXT NULL;
