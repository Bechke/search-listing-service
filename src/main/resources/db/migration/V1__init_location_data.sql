-- Flyway-compatible SQL migration script
-- Filename suggestion: V1__init_location_data.sql

-- =========================
-- Schema Definitions
-- =========================

CREATE TABLE country (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE state (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    country_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (country_id) REFERENCES country(id)
);

CREATE TABLE city (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (state_id) REFERENCES state(id)
);

CREATE TABLE neighbourhood (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    city_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (city_id) REFERENCES city(id)
);

-- =========================
-- Data Inserts
-- =========================

-- Country
INSERT INTO country (name) VALUES ('India');

-- States
INSERT INTO state (name, country_id) VALUES
('Telangana', 1),
('Maharashtra', 1),
('Delhi', 1),
('Tamil Nadu', 1),
('West Bengal', 1),
('Karnataka', 1);

-- Cities
INSERT INTO city (name, state_id) VALUES
('Hyderabad', 1),
('Mumbai', 2),
('New Delhi', 3),
('Chennai', 4),
('Kolkata', 5),
('Bengaluru', 6);

-- Neighbourhoods for Hyderabad (city_id = 1)
INSERT INTO neighbourhood (name, city_id) VALUES
('A. C. Guards', 1),
('A. S. Rao Nagar', 1),
('Abids', 1),
('Adikmet', 1),
('Afzal Gunj', 1),
('Aghapura', 1),
('Aliabad', 1),
('Alwal', 1),
('Amberpet', 1),
('Ameerpet', 1),
('Anandbagh', 1),
('Ashok Nagar', 1),
('Asif Nagar', 1),
('Attapur', 1),
('Azampura', 1),
('Badichowdi', 1),
('Bagh Lingampally', 1),
('Bairamalguda', 1),
('Balkampet', 1),
('Banjara Hills', 1),
('Barkas', 1),
('Barkatpura', 1),
('Basheerbagh', 1),
('Begum Bazaar', 1),
('Begumpet', 1),
('Bharat Nagar', 1),
('BHEL Township', 1),
('Boggulkunta', 1),
('Bolarum', 1),
('Borabanda', 1),
('Bowenpally', 1),
('Chaderghat', 1),
('Chanda Nagar', 1),
('Charminar', 1),
('Chikkadpally', 1),
('Chintal', 1),
('Dilsukhnagar', 1),
('Domalguda', 1),
('Dundigal', 1),
('ECIL', 1),
('Erragadda', 1),
('Falaknuma', 1),
('Gachibowli', 1),
('Gaganpahad', 1),
('Gandhinagar', 1),
('Ghatkesar', 1),
('Golconda', 1),
('Gowlipura', 1),
('Habsiguda', 1),
('Hafeezpet', 1),
('Hakeempet', 1),
('Hakimpet', 1),
('Hayathnagar', 1),
('Himayatnagar', 1),
('HITEC City', 1),
('Hyderguda', 1),
('Jahanuma', 1),
('Jambagh', 1),
('Jubilee Hills', 1),
('Kachiguda', 1),
('Kalasiguda', 1),
('Kanchanbagh', 1),
('Kapra', 1),
('Karwan', 1),
('Kattedan', 1),
('Khairatabad', 1),
('Kishan Bagh', 1),
('Koti', 1),
('KPHB Colony', 1),
('Kukatpally', 1),
('Kulsumpura', 1),
('Lalapet', 1),
('Lal Darwaza', 1),
('Langar Houz', 1),
('LB Nagar', 1),
('Lothkunta', 1),
('Madhapur', 1),
('Malkajgiri', 1),
('Malakpet', 1),
('Manikonda', 1),
('Masab Tank', 1),
('Mehdipatnam', 1),
('Miyapur', 1),
('Moosarambagh', 1),
('Moosapet', 1),
('Moula Ali', 1),
('Musi Nagar', 1),
('Musheerabad', 1),
('Nallakunta', 1),
('Nampally', 1),
('Nanakramguda', 1),
('Narsingi', 1),
('Neredmet', 1),
('New Bowenpally', 1),
('New Malakpet', 1),
('Nizampet', 1),
('Old Bowenpally', 1),
('Old Malakpet', 1),
('Osman Nagar', 1),
('Padmarao Nagar', 1),
('Panjagutta', 1),
('Paradise', 1),
('Patancheru', 1),
('Petbasheerabad', 1),
('Puranapul', 1),
('Quthbullapur', 1),
('Ramanthapur', 1),
('Ramnagar', 1),
('Rani Gunj', 1),
('RTC Cross Road', 1),
('Safilguda', 1),
('Sainikpuri', 1),
('Sanath Nagar', 1),
('Santoshnagar', 1),
('Saroor Nagar', 1),
('Secunderabad', 1),
('Shaikpet', 1),
('Shalibanda', 1);

-- Neighbourhoods for Mumbai (city_id = 2)
INSERT INTO neighbourhood (name, city_id) VALUES
('Andheri East', 2), ('Andheri West', 2), ('Antop Hill', 2), ('Bandra East', 2), ('Bandra West', 2),
('Borivali East', 2), ('Borivali West', 2), ('Charni Road', 2), ('Chembur East', 2), ('Chembur West', 2),
('Chinchpokli', 2), ('Churchgate', 2), ('Colaba', 2), ('Cuffe Parade', 2), ('Dadar East', 2),
('Dadar West', 2), ('Dahisar East', 2), ('Dahisar West', 2), ('Dongri', 2), ('Ghatkopar East', 2),
('Ghatkopar West', 2), ('Girgaon', 2), ('Goregaon East', 2), ('Goregaon West', 2), ('Grant Road', 2),
('Jogeshwari East', 2), ('Jogeshwari West', 2), ('Juhu', 2), ('Kalbadevi', 2), ('Kandivali East', 2),
('Kandivali West', 2), ('Kanjurmarg East', 2), ('Kanjurmarg West', 2), ('Khar East', 2), ('Khar West', 2),
('Kurla East', 2), ('Kurla West', 2), ('Lower Parel', 2), ('Mahim', 2), ('Malad East', 2),
('Malad West', 2), ('Marine Lines', 2), ('Mazgaon', 2), ('Mulund East', 2), ('Mulund West', 2),
('Naigaon', 2), ('Nalasopara', 2), ('Nariman Point', 2), ('Powai', 2), ('Santacruz East', 2),
('Santacruz West', 2), ('Sion', 2), ('Vikhroli East', 2), ('Vikhroli West', 2), ('Vile Parle East', 2),
('Vile Parle West', 2), ('Wadala', 2), ('Worli', 2);

-- Neighbourhoods for New Delhi (city_id = 3)
INSERT INTO neighbourhood (name, city_id) VALUES
('Chanakyapuri', 3), ('Connaught Place', 3), ('Dwarka', 3), ('Greater Kailash', 3), ('Hauz Khas', 3),
('Janakpuri', 3), ('Karol Bagh', 3), ('Lajpat Nagar', 3), ('Malviya Nagar', 3), ('Mayur Vihar', 3),
('Model Town', 3), ('Munirka', 3), ('Nehru Place', 3), ('New Friends Colony', 3), ('Okhla', 3),
('Patel Nagar', 3), ('Rajouri Garden', 3), ('Rohini', 3), ('Safdarjung', 3), ('Saket', 3),
('Sarita Vihar', 3), ('South Extension', 3), ('Tilak Nagar', 3), ('Uttam Nagar', 3), ('Vasant Kunj', 3),
('Vasant Vihar', 3), ('Pitampura', 3), ('Paschim Vihar', 3), ('Ashok Vihar', 3), ('Punjabi Bagh', 3);

-- Neighbourhoods for Chennai (city_id = 4)
INSERT INTO neighbourhood (name, city_id) VALUES
('Adyar', 4), ('Alwarpet', 4), ('Ambattur', 4), ('Anna Nagar', 4), ('Ashok Nagar', 4),
('Besant Nagar', 4), ('Chromepet', 4), ('Egmore', 4), ('Guindy', 4), ('K.K. Nagar', 4),
('Kilpauk', 4), ('Kodambakkam', 4), ('Korattur', 4), ('Mambalam', 4), ('Mandaveli', 4),
('Mogappair', 4), ('Mylapore', 4), ('Nandanam', 4), ('Nungambakkam', 4), ('Perambur', 4),
('Porur', 4), ('Purasaiwakkam', 4), ('Saidapet', 4), ('Tambaram', 4), ('Teynampet', 4),
('Thiruvanmiyur', 4), ('T Nagar', 4), ('Vadapalani', 4), ('Velachery', 4), ('Villivakkam', 4),
('Washermanpet', 4), ('Royapettah', 4), ('Pallavaram', 4), ('Thirumangalam', 4), ('Kotturpuram', 4);

-- Neighbourhoods for Kolkata (city_id = 5)
INSERT INTO neighbourhood (name, city_id) VALUES
('Alipore', 5), ('Ballygunge', 5), ('Baranagar', 5), ('Behala', 5), ('Bhowanipore', 5),
('Dum Dum', 5), ('Garia', 5), ('Girish Park', 5), ('Hatibagan', 5), ('Howrah', 5),
('Jadavpur', 5), ('Kalighat', 5), ('Lake Gardens', 5), ('Park Circus', 5), ('Park Street', 5),
('Rajarhat', 5), ('Salt Lake', 5), ('Shyambazar', 5), ('Tollygunge', 5), ('Ultadanga', 5),
('Baguihati', 5), ('Bansdroni', 5), ('Belgachia', 5), ('Cossipore', 5), ('Kasba', 5);

-- Neighbourhoods for Bengaluru (city_id = 6)
INSERT INTO neighbourhood (name, city_id) VALUES
('Indiranagar', 6), ('Koramangala', 6), ('Whitefield', 6), ('Marathahalli', 6), ('MG Road', 6),
('Jayanagar', 6), ('Basavanagudi', 6), ('Rajajinagar', 6), ('HSR Layout', 6), ('BTM Layout', 6),
('Banashankari', 6), ('Malleshwaram', 6), ('Yelahanka', 6), ('Hebbal', 6), ('Electronic City', 6),
('Sarjapur Road', 6), ('Bannerghatta Road', 6), ('Hennur', 6), ('Kalyan Nagar', 6), ('Ulsoor', 6),
('JP Nagar', 6), ('Frazer Town', 6), ('Kumaraswamy Layout', 6), ('Nagarbhavi', 6), ('RT Nagar', 6),
('Varthur', 6), ('Kengeri', 6), ('KR Puram', 6), ('Domlur', 6), ('Magadi Road', 6);