-- Migration: Fix lobbies status check constraint to include IN_PROGRESS
-- This script updates the check constraint to allow all valid LobbyStatus enum values
-- This script is idempotent and safe to run multiple times

-- Drop the existing constraint if it exists
ALTER TABLE lobbies DROP CONSTRAINT IF EXISTS lobbies_status_check;

-- Add the updated constraint with all valid status values
-- Note: This will fail if the constraint already exists with the same definition,
-- but that's okay - it means the migration already ran successfully
ALTER TABLE lobbies ADD CONSTRAINT lobbies_status_check 
    CHECK (status IN ('WAITING', 'READY', 'STARTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));



