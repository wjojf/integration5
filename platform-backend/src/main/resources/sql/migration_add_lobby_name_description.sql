-- Migration: Add name and description columns to lobbies table
-- This script adds the name and description columns to the existing lobbies table
-- Run this script if your database already exists and doesn't have these columns
-- Uses IF NOT EXISTS syntax compatible with Spring Boot's SQL parser

-- Add description column (nullable) - IF NOT EXISTS prevents error if column exists
ALTER TABLE lobbies ADD COLUMN IF NOT EXISTS description VARCHAR(500);

-- Add name column (nullable first, then we'll populate and make NOT NULL)
ALTER TABLE lobbies ADD COLUMN IF NOT EXISTS name VARCHAR(100);

-- Update any existing rows that have NULL name with a default value
UPDATE lobbies SET name = 'Untitled Lobby' WHERE name IS NULL;

-- Ensure name column is NOT NULL (safe to run multiple times)
ALTER TABLE lobbies ALTER COLUMN name SET NOT NULL;

