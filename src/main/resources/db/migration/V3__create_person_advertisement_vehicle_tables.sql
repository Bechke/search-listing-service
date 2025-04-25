-- V3__create_person_advertisement_vehicle_tables.sql

-- 1. Person Table
CREATE TABLE person (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(50) NOT NULL,
    company VARCHAR(100) UNIQUE,
    mobile_number VARCHAR(12) NOT NULL,
    email VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_activity TIMESTAMP NULL
);

-- 2. Advertisement Table
CREATE TABLE advertisement (
    advertisement_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    ad_category VARCHAR(50),
    ad_subcategory VARCHAR(100),
    country VARCHAR(50),
    state VARCHAR(50),
    city VARCHAR(50),
    neighbourhood VARCHAR(100),
    title VARCHAR(100),
    default_img_path VARCHAR(255),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (person_id) REFERENCES person(person_id)
);

-- 3. Vehicle Table
CREATE TABLE vehicle (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    person_id INT NOT NULL,
    mobile VARCHAR(12),
    ad_subcategory VARCHAR(100),
    brand VARCHAR(100),
    year INT,
    fuel_type VARCHAR(50),
    transmission VARCHAR(50),
    odometer_reading INT,
    num_owners INT,
    title VARCHAR(100),
    description VARCHAR(500),
    price DECIMAL(10,2),
    default_img_path VARCHAR(50),
    country VARCHAR(50),
    state VARCHAR(50),
    city VARCHAR(50),
    neighbourhood VARCHAR(100),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (person_id) REFERENCES person(person_id)
);
