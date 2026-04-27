-- Rename vehicle_category names to match mobile app subCategory values (CAR, BIKE, etc.)
UPDATE vehicle_category SET name = 'CAR'     WHERE name = 'Cars';
UPDATE vehicle_category SET name = 'BIKE'    WHERE name = 'Bikes';
UPDATE vehicle_category SET name = 'SCOOTER' WHERE name = 'Scooters';
UPDATE vehicle_category SET name = 'BICYCLE' WHERE name = 'Bicycles';
UPDATE vehicle_category SET name = 'OTHER'   WHERE name = 'Other';
