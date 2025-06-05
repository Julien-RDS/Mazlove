-- Create database
CREATE DATABASE mazlove_db;

-- Create tables
CREATE TABLE providers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    rating FLOAT DEFAULT 0,
    verified BOOLEAN DEFAULT FALSE,
    availability JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service_items (
    id SERIAL PRIMARY KEY,
    provider_id INTEGER REFERENCES providers(id),
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price FLOAT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sessions (
    id SERIAL PRIMARY KEY,
    provider_id INTEGER REFERENCES providers(id),
    start_time TIMESTAMP NOT NULL,
    duration INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO providers (name, description, rating, verified, availability) VALUES
('Sergio Services', 'Professional cleaning services', 4.7, TRUE, '[{"start": "09:00", "end": "12:00"}, {"start": "14:00", "end": "18:00"}]'),
('Handyman Express', 'General repairs and maintenance', 4.5, TRUE, '[{"start": "08:00", "end": "13:00"}, {"start": "15:00", "end": "19:00"}]');

INSERT INTO service_items (provider_id, name, description, price) VALUES
(1, 'Deep Cleaning', 'Full house cleaning service', 50.0),
(1, 'Garden Maintenance', 'Weekly garden care', 30.0),
(2, 'Plumbing Repair', 'Professional plumbing services', 45.0),
(2, 'Electrical Work', 'Electrical installation and repair', 60.0);
