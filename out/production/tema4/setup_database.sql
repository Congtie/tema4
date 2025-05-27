-- PostgreSQL Database Setup Script for Exercise 3
-- Run this script as the postgres user

-- Create database if it doesn't exist
-- Note: You might need to create this manually if running from psql
-- CREATE DATABASE testdb;

-- Connect to the testdb database and run the following:
-- \c testdb

-- The application will create the Log table automatically
-- But here's the structure for reference:
/*
CREATE TABLE IF NOT EXISTS Log (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message VARCHAR(255) NOT NULL,
    worker_id INTEGER NOT NULL
);
*/

-- The application will also create this stored procedure:
/*
CREATE OR REPLACE FUNCTION cleanup_old_logs()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM Log 
    WHERE timestamp < (CURRENT_TIMESTAMP - INTERVAL '1 hour');
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;
*/

-- Example queries to check the data after running the application:
-- SELECT COUNT(*) FROM Log;
-- SELECT worker_id, COUNT(*) as message_count FROM Log GROUP BY worker_id;
-- SELECT * FROM Log ORDER BY timestamp DESC LIMIT 10;
