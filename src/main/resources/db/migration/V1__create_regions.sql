CREATE TABLE region (
    regionid BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    createdby VARCHAR(255),
    updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updatedby VARCHAR(255)
);

INSERT INTO region (name, city, state, country, createdby) VALUES
('Asia', 'Hyderabad', 'Telangana', 'India', 'admin'),
('Asia', 'Salem', 'Tamil Nadu', 'India', 'admin'),
('Africa', 'Addis Ababa', 'Oromia', 'Ethiopia', 'admin');
