-- vehicle.default_img_path was VARCHAR(50), too small for real GCS storage paths
-- (e.g. "users/{keycloakId}/vehicles/{vehicleId}/{filename}" routinely exceeds 50 chars),
-- causing MySQL error 1406 (Data truncation) on every insert with a real image and silently
-- dropping the Kafka vehicle-ads CREATE/UPDATE event after retries were exhausted.
-- advertisement.default_img_path was already VARCHAR(255); align vehicle to match.
ALTER TABLE vehicle MODIFY COLUMN default_img_path VARCHAR(255);
