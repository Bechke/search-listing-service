-- Update seed vehicles with 3-image galleries.
-- Paths match SeedImageLoader (image-storage-service) and AddSeedVehicleImages (vehicle-service).

UPDATE vehicle
SET image_urls_json = '["vehicles/seed-001.jpg","vehicles/seed-001-side.jpg","vehicles/seed-001-interior.jpg"]'
WHERE vehicle_source_id = 'seed-vehicle-001';

UPDATE vehicle
SET image_urls_json = '["vehicles/seed-002.jpg","vehicles/seed-002-side.jpg","vehicles/seed-002-detail.jpg"]'
WHERE vehicle_source_id = 'seed-vehicle-002';

UPDATE vehicle
SET image_urls_json = '["vehicles/seed-003.jpg","vehicles/seed-003-side.jpg","vehicles/seed-003-detail.jpg"]'
WHERE vehicle_source_id = 'seed-vehicle-003';

UPDATE vehicle
SET image_urls_json = '["vehicles/seed-004.jpg","vehicles/seed-004-side.jpg","vehicles/seed-004-interior.jpg"]'
WHERE vehicle_source_id = 'seed-vehicle-004';

UPDATE vehicle
SET image_urls_json = '["vehicles/seed-005.jpg","vehicles/seed-005-side.jpg","vehicles/seed-005-detail.jpg"]'
WHERE vehicle_source_id = 'seed-vehicle-005';
