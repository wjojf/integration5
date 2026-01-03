-- Migration: Fix lobbies status check constraint to include IN_PROGRESS
-- This script updates the check constraint to allow all valid LobbyStatus enum values

-- Drop the existing constraint if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'lobbies_status_check' 
        AND conrelid = 'lobbies'::regclass
    ) THEN
        ALTER TABLE lobbies DROP CONSTRAINT lobbies_status_check;
    END IF;
END $$;

-- Add the updated constraint with all valid status values
ALTER TABLE lobbies ADD CONSTRAINT lobbies_status_check 
    CHECK (status IN ('WAITING', 'READY', 'STARTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

