-- Migration: Create player_statistics table for achievement evaluation
-- This table stores player statistics needed to evaluate achievement criteria

CREATE TABLE IF NOT EXISTS player_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id VARCHAR(255) NOT NULL,
    game_id UUID NOT NULL,
    total_wins INTEGER NOT NULL DEFAULT 0,
    total_losses INTEGER NOT NULL DEFAULT 0,
    total_draws INTEGER NOT NULL DEFAULT 0,
    current_win_streak INTEGER NOT NULL DEFAULT 0,
    longest_win_streak INTEGER NOT NULL DEFAULT 0,
    total_play_time_seconds BIGINT,
    fastest_win_seconds BIGINT,
    unique_opponents TEXT,
    total_moves INTEGER NOT NULL DEFAULT 0,
    total_games INTEGER NOT NULL DEFAULT 0,
    UNIQUE(player_id, game_id),
    CONSTRAINT fk_player_statistics_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_player_statistics_player_game ON player_statistics(player_id, game_id);

-- Migration: Add criteria and triggeringEventType columns to achievements table
-- These columns were missing from the original schema

ALTER TABLE achievements 
ADD COLUMN IF NOT EXISTS trigger_condition_string TEXT,
ADD COLUMN IF NOT EXISTS third_party_achievement BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS code VARCHAR(255),
ADD COLUMN IF NOT EXISTS criteria VARCHAR(50),
ADD COLUMN IF NOT EXISTS triggering_event_type VARCHAR(50);

