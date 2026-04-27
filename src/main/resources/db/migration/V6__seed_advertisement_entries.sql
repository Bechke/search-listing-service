-- V6: Seed advertisement entries for the 5 vehicles seeded in V5.
--     All listings are in Hyderabad / Banjara Hills.
--
-- INSERT IGNORE is idempotent — safe to re-run.

SET @pid = (SELECT person_id FROM person WHERE keycloak_id = 'seed-seller-001');

INSERT IGNORE INTO advertisement
    (person_id, ad_category, ad_subcategory,
     country, state, city, neighbourhood,
     title, default_img_path, status,
     created_at, updated_at)
VALUES
    (@pid, 'VEHICLE', 'CAR',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills',
     '2022 Toyota Camry',              'vehicles/seed-001.jpg', 'ACTIVE',
     '2024-01-02 10:00:00', '2024-01-02 10:00:00'),

    (@pid, 'VEHICLE', 'SCOOTER',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills',
     '2021 Honda Activa 6G',           'vehicles/seed-002.jpg', 'ACTIVE',
     '2024-01-03 11:00:00', '2024-01-03 11:00:00'),

    (@pid, 'VEHICLE', 'BIKE',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills',
     '2020 Royal Enfield Classic 350', 'vehicles/seed-003.jpg', 'ACTIVE',
     '2024-01-04 09:30:00', '2024-01-04 09:30:00'),

    (@pid, 'VEHICLE', 'CAR',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills',
     '2023 Hyundai Creta',             'vehicles/seed-004.jpg', 'ACTIVE',
     '2024-01-05 14:00:00', '2024-01-05 14:00:00'),

    (@pid, 'VEHICLE', 'BIKE',
     'India', 'Telangana', 'Hyderabad', 'Banjara Hills',
     '2019 Hero HF Deluxe',            'vehicles/seed-005.jpg', 'ACTIVE',
     '2024-01-06 08:00:00', '2024-01-06 08:00:00');
