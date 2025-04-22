-- =========================
-- Schema Definitions
-- =========================
CREATE TABLE vehicle_category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE vehicle_brand (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES vehicle_category(id)
);

-- =========================
-- Data Inserts
-- =========================
-- Insert vehicle categories
INSERT INTO vehicle_category (name, created_at, updated_at) VALUES
('Cars', NOW(), NOW()),
('Bikes', NOW(), NOW()),
('Scooters', NOW(), NOW()),
('Bicycles', NOW(), NOW()),
('Other', NOW(), NOW())

;


-- Vehicle Brands
INSERT INTO vehicle_brand (name, category_id) VALUES
-- Cars (category_id = 1)
('Maruti Suzuki', 1),
('Hyundai', 1),
('Tata Motors', 1),
('Mahindra', 1),
('Honda', 1),
('Toyota', 1),
('Kia', 1),
('Renault', 1),
('Volkswagen', 1),
('Skoda', 1),
('Nissan', 1),
('MG Motor', 1),
('Jeep', 1),
('Ford', 1),
('Fiat', 1),
('Mercedes-Benz', 1),
('BMW', 1),
('Audi', 1),
('Jaguar', 1),
('Porsche', 1),
('Volvo', 1),
('Lexus', 1),
('Mini', 1),
('Land Rover', 1),

-- Bikes (category_id = 2)
('Royal Enfield', 2),
('Bajaj', 2),
('TVS', 2),
('Hero MotoCorp', 2),
('Yamaha', 2),
('Honda', 2),
('KTM', 2),
('Suzuki', 2),
('Harley-Davidson', 2),
('Jawa', 2),
('BMW Motorrad', 2),
('Benelli', 2),
('Triumph', 2),
('Aprilia', 2),
('Ducati', 2),
('MV Agusta', 2),

-- Scooters (category_id = 3)
('Honda', 3),
('TVS', 3),
('Suzuki', 3),
('Hero', 3),
('Yamaha', 3),
('Vespa', 3),
('Aprilia', 3),
('Bajaj', 3),
('Okinawa', 3),
('Ather', 3),
('Pure EV', 3),
('Bounce', 3),

-- Bicycles (category_id = 4)
('Hero Cycles', 4),
('Atlas', 4),
('Hercules', 4),
('BSA', 4),
('Firefox', 4),
('Montra', 4),
('Avon', 4),
('Btwin', 4),
('Mach City', 4),
('Ninety One', 4),
('Urban Terrain', 4),
('Leader', 4),
('Cradiac', 4),
('Kross', 4),
('Giant', 4),
('Scott', 4),
('Trek', 4),
('Specialized', 4),
('Cannondale', 4);
