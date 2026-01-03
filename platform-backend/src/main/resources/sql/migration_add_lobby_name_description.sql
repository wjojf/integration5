-- Migration: Add name and description columns to lobbies table
-- This script adds the name and description columns to the existing lobbies table
-- Run this script if your database already exists and doesn't have these columns

-- Add description column (nullable)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'lobbies' AND column_name = 'description'
    ) THEN
        ALTER TABLE lobbies ADD COLUMN description VARCHAR(500);
    END IF;
END $$;

-- Add name column (NOT NULL with default for existing rows)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'lobbies' AND column_name = 'name'
    ) THEN
        -- First add the column as nullable
        ALTER TABLE lobbies ADD COLUMN name VARCHAR(100);
        
        -- Update existing rows with a default name
        UPDATE lobbies SET name = 'Untitled Lobby' WHERE name IS NULL;
        
        -- Now make it NOT NULL
        ALTER TABLE lobbies ALTER COLUMN name SET NOT NULL;
    END IF;
END $$;

