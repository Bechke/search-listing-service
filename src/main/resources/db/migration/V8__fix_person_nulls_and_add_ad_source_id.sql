-- Make person columns nullable so Keycloak-only users (no profile yet) can be created
ALTER TABLE person
    MODIFY COLUMN full_name VARCHAR(50) NULL,
    MODIFY COLUMN mobile_number VARCHAR(12) NULL;

-- Allow upsert on advertisement by vehicle source id
ALTER TABLE advertisement
    ADD COLUMN vehicle_source_id VARCHAR(255) NULL UNIQUE;
