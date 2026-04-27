-- V5: Seed a demo seller and 5 sample vehicle listings that are in sync with
--     the MongoDB documents in vehicle-service and the images in image-storage-service.
--
-- MongoDB IDs:       seed-vehicle-001 … seed-vehicle-005
-- Keycloak seller:   seed-seller-001
-- Image paths:       vehicles/seed-001.jpg … vehicles/seed-005.jpg
-- Location:          All listings in Hyderabad / Banjara Hills
--
-- INSERT IGNORE skips a row silently if the UNIQUE constraint already holds,
-- so re-running this script manually is safe (Flyway will never re-run it anyway).

-- ── 1. Seed seller ────────────────────────────────────────────────────────────
INSERT IGNORE INTO person (full_name, keycloak_id, email, mobile_number, created_at, updated_at)
VALUES ('Bechke Demo Seller', 'seed-seller-001', 'demo.seller@bechke.com', '9000000001',
        '2024-01-01 00:00:00', '2024-01-01 00:00:00');

-- ── 2. Resolve the seeded seller's auto-incremented PK ───────────────────────
SET @pid = (SELECT person_id FROM person WHERE keycloak_id = 'seed-seller-001');

-- ── 3. Seed vehicles (all in Hyderabad / Banjara Hills) ───────────────────────
INSERT IGNORE INTO vehicle
    (person_id, vehicle_source_id, ad_subcategory, brand, year, fuel_type, transmission,
     odometer_reading, num_owners, title, description, price,
     default_img_path, image_urls_json,
     country, state, city, neighbourhood, status, created_at, updated_at)
VALUES
    -- 1 · Toyota Camry
    (@pid, 'seed-vehicle-001', 'CAR', 'Toyota', 2022, 'PETROL', 'AUTOMATIC',
     25000, 1,
     '2022 Toyota Camry',
     'Well-maintained 2022 Toyota Camry V6 in pearl white. Single owner, full service history.',
     1800000.00,
     'vehicles/seed-001.jpg', '["vehicles/seed-001.jpg"]',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills', 'ACTIVE',
     '2024-01-02 10:00:00', '2024-01-02 10:00:00'),

    -- 2 · Honda Activa 6G
    (@pid, 'seed-vehicle-002', 'SCOOTER', 'Honda', 2021, 'PETROL', 'CVT',
     12000, 1,
     '2021 Honda Activa 6G',
     'Honda Activa 6G, single owner, OBD compliant BS6 engine, excellent mileage.',
     85000.00,
     'vehicles/seed-002.jpg', '["vehicles/seed-002.jpg"]',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills', 'ACTIVE',
     '2024-01-03 11:00:00', '2024-01-03 11:00:00'),

    -- 3 · Royal Enfield Classic 350
    (@pid, 'seed-vehicle-003', 'BIKE', 'Royal Enfield', 2020, 'PETROL', 'MANUAL',
     30000, 1,
     '2020 Royal Enfield Classic 350',
     'Classic 350 in Thunder Black. Fully serviced, all original parts. Great tourer.',
     170000.00,
     'vehicles/seed-003.jpg', '["vehicles/seed-003.jpg"]',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills', 'ACTIVE',
     '2024-01-04 09:30:00', '2024-01-04 09:30:00'),

    -- 4 · Hyundai Creta
    (@pid, 'seed-vehicle-004', 'CAR', 'Hyundai', 2023, 'PETROL', 'AUTOMATIC',
     8000, 0,
     '2023 Hyundai Creta',
     '2023 Hyundai Creta SX(O) with sunroof, ADAS, wireless charging. As good as new.',
     1500000.00,
     'vehicles/seed-004.jpg', '["vehicles/seed-004.jpg"]',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills', 'ACTIVE',
     '2024-01-05 14:00:00', '2024-01-05 14:00:00'),

    -- 5 · Hero HF Deluxe
    (@pid, 'seed-vehicle-005', 'BIKE', 'Hero MotoCorp', 2019, 'PETROL', 'MANUAL',
     45000, 2,
     '2019 Hero HF Deluxe',
     'Hero HF Deluxe in good running condition. Regularly serviced at authorised centre.',
     65000.00,
     'vehicles/seed-005.jpg', '["vehicles/seed-005.jpg"]',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills', 'ACTIVE',
     '2024-01-06 08:00:00', '2024-01-06 08:00:00');
