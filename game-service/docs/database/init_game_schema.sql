-- Game Service Schema Initialization
-- This script creates a separate schema for game service tables
-- to avoid conflicts with platform-backend tables

-- Create game service schema
CREATE SCHEMA IF NOT EXISTS game_service;

-- Grant all privileges to postgres user
GRANT ALL PRIVILEGES ON SCHEMA game_service TO postgres;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA game_service
GRANT ALL PRIVILEGES ON TABLES TO postgres;

ALTER DEFAULT PRIVILEGES IN SCHEMA game_service
GRANT ALL PRIVILEGES ON SEQUENCES TO postgres;

-- Comment on schema
COMMENT ON SCHEMA game_service IS 'Schema for AI/ML game service tables (game logs, sessions, ML data)';

-- You can also set the search path if needed
-- This makes game_service the default schema for this database
-- ALTER DATABASE banditgames SET search_path TO game_service, public;

-- Verify schema creation
SELECT schema_name
FROM information_schema.schemata
WHERE schema_name = 'game_service';
