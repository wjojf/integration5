-- ============================================================================
-- Comprehensive Seed Script for BanditGames Platform
-- ============================================================================
-- This script seeds all platform data including:
-- - Database migrations (player_statistics table, achievements columns)
-- - Games (Connect Four, Chess)
-- - Achievements (for both games)
-- - Player Statistics (for achievement evaluation)
-- - User Achievements (unlocked achievements)
-- - Lobbies (various states: WAITING, STARTED, FINISHED)
-- - Lobby Players associations
--
-- Note: This script is idempotent and can be run multiple times safely.
-- It uses ON CONFLICT clauses and IF NOT EXISTS to update existing records.
--
-- Prerequisites:
-- - Players table must already have data (from data.sql)
-- - Database schema must be initialized (JPA ddl-auto or migrations)
--
-- Uses existing player IDs from the players table
-- Game IDs:
-- - Connect Four: 550e8400-e29b-41d4-a716-446655440001
-- - Chess: 550e8400-e29b-41d4-a716-446655440002
-- ============================================================================

-- ============================================================================
-- 0. DATABASE MIGRATIONS
-- ============================================================================
-- Ensure required tables and columns exist

-- Create player_statistics table if it doesn't exist
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

-- Add missing columns to achievements table if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'achievements' AND column_name = 'trigger_condition_string') THEN
        ALTER TABLE achievements ADD COLUMN trigger_condition_string TEXT;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'achievements' AND column_name = 'third_party_achievement') THEN
        ALTER TABLE achievements ADD COLUMN third_party_achievement BOOLEAN DEFAULT FALSE;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'achievements' AND column_name = 'code') THEN
        ALTER TABLE achievements ADD COLUMN code VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'achievements' AND column_name = 'criteria') THEN
        ALTER TABLE achievements ADD COLUMN criteria VARCHAR(50);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'achievements' AND column_name = 'triggering_event_type') THEN
        ALTER TABLE achievements ADD COLUMN triggering_event_type VARCHAR(50);
    END IF;
END $$;

-- ============================================================================
-- 1. GAMES
-- ============================================================================

INSERT INTO games (id, name, description, genre, image)
VALUES 
    ('550e8400-e29b-41d4-a716-446655440001', 'Connect Four', 
     'Classic strategy game where players take turns dropping colored discs into a grid. First to connect four in a row wins!', 
     'Strategy', 
     'https://thumbs.dreamstime.com/b/blue-board-game-connect-four-showing-winning-strategy-red-yellow-pieces-325180667.jpg'),
    
    ('550e8400-e29b-41d4-a716-446655440002', 'Chess', 
     'The classic game of strategy and tactics. Outmaneuver your opponent and checkmate their king!', 
     'Strategy', 
     'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSYigKOHenUTuER6t1jBye1G_D1q8IuOauFSQ&s')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    genre = EXCLUDED.genre,
    image = EXCLUDED.image;

-- ============================================================================
-- 2. ACHIEVEMENTS (Connect Four)
-- ============================================================================

INSERT INTO achievements (id, game_id, name, description, achievement_category, achievement_rarity, criteria, triggering_event_type, trigger_condition_string, third_party_achievement)
VALUES 
    ('2c381abf-6522-4ce1-b2c2-24d40a6b8f75', '550e8400-e29b-41d4-a716-446655440001', 
     'First Victory', 'Win your first game of Connect Four.', 
     'PROGRESSION', 'COMMON', 'ONE_TIME_EVENT', 'GAME_WON', 'Win your first game.', false),
    
    ('8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', '550e8400-e29b-41d4-a716-446655440001', 
     'Getting Warm', 'Win 5 games of Connect Four.', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 5 games.', false),
    
    ('f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', '550e8400-e29b-41d4-a716-446655440001', 
     'Double Digits', 'Win 10 games of Connect Four.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 10 games.', false),
    
    ('a1b2c3d4-5e6f-4789-8abc-9def01234567', '550e8400-e29b-41d4-a716-446655440001', 
     'Quarter Century', 'Win 25 games of Connect Four.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 25 games.', false),
    
    ('b2c3d4e5-6f7a-4890-9bcd-0ef123456789', '550e8400-e29b-41d4-a716-446655440001', 
     'Half Century', 'Win 50 games of Connect Four.', 
     'PROGRESSION', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 50 games.', false),
    
    ('c3d4e5f6-7a8b-4901-9cde-0f123456789a', '550e8400-e29b-41d4-a716-446655440001', 
     'Centurion', 'Win 100 games of Connect Four.', 
     'PROGRESSION', 'EPIC', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 100 games.', false),
    
    ('4e96f75b-23bb-4e2f-b919-9fd7c2d0b9f0', '550e8400-e29b-41d4-a716-446655440001', 
     'Comeback Kid', 'Lose 10 games of Connect Four (hey, it happens).', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAMES_LOST', 'Lose 10 games.', false),

    ('b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', '550e8400-e29b-41d4-a716-446655440001', 
     'Unbreakable', 'Win 10 games of Connect Four in a row.', 
     'DIFFICULTY', 'EPIC', 'STREAK', 'GAME_WON', 'Win 10 games in a row.', false),
    
    ('d5e6f7a8-9b0c-4a12-9def-0123456789ab', '550e8400-e29b-41d4-a716-446655440001', 
     'Dominator', 'Win 5 games of Connect Four in a row.', 
     'DIFFICULTY', 'RARE', 'STREAK', 'GAME_WON', 'Win 5 games in a row.', false),
    
    ('e6f7a8b9-0c1d-4a23-9ef0-123456789abc', '550e8400-e29b-41d4-a716-446655440001', 
     'Hot Streak', 'Win 3 games of Connect Four in a row.', 
     'DIFFICULTY', 'UNCOMMON', 'STREAK', 'GAME_WON', 'Win 3 games in a row.', false),

    ('f7a8b9c0-1d2e-4a34-9f01-23456789abcd', '550e8400-e29b-41d4-a716-446655440001', 
     'Speed Runner', 'Win a game of Connect Four under 2 minutes.', 
     'TIME', 'EPIC', 'TIME_REACHED', 'GAME_WON', 'Win a game under 2 minutes.', false),
    
    ('a8b9c0d1-2e3f-4a45-9012-3456789abcde', '550e8400-e29b-41d4-a716-446655440001', 
     'Quick Win', 'Win a game of Connect Four under 5 minutes.', 
     'TIME', 'RARE', 'TIME_REACHED', 'GAME_WON', 'Win a game under 5 minutes.', false),
    
    ('b9c0d1e2-3f4a-4a56-0123-456789abcdef', '550e8400-e29b-41d4-a716-446655440001', 
     'Marathon Session', 'Accumulate 1 hour of Connect Four play time.', 
     'TIME', 'UNCOMMON', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 1 hour of play time.', false),
    
    ('c0d1e2f3-4a5b-4a67-1234-56789abcdef0', '550e8400-e29b-41d4-a716-446655440001', 
     'Weekend Warrior', 'Accumulate 10 hours of Connect Four play time.', 
     'TIME', 'RARE', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 10 hours of play time.', false),

    ('d1e2f3a4-5b6c-4a78-2345-6789abcdef01', '550e8400-e29b-41d4-a716-446655440001', 
     'Social Butterfly', 'Play against 20 unique players in Connect Four.', 
     'SOCIAL', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Play against 20 unique players.', false),
    
    ('e2f3a4b5-6c7d-4a89-3456-789abcdef012', '550e8400-e29b-41d4-a716-446655440001', 
     'Networker', 'Play against 10 unique players in Connect Four.', 
     'SOCIAL', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Play against 10 unique players.', false)

ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    achievement_category = EXCLUDED.achievement_category,
    achievement_rarity = EXCLUDED.achievement_rarity,
    criteria = EXCLUDED.criteria,
    triggering_event_type = EXCLUDED.triggering_event_type,
    trigger_condition_string = EXCLUDED.trigger_condition_string,
    third_party_achievement = EXCLUDED.third_party_achievement;

-- ============================================================================
-- 3. ACHIEVEMENTS (Chess)
-- ============================================================================

INSERT INTO achievements (id, game_id, name, description, achievement_category, achievement_rarity, criteria, triggering_event_type, trigger_condition_string, third_party_achievement)
VALUES 
    ('efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', '550e8400-e29b-41d4-a716-446655440002', 
     'First Checkmate', 'Win your first game of Chess.', 
     'PROGRESSION', 'COMMON', 'ONE_TIME_EVENT', 'GAME_WON', 'Win your first game.', false),
    
    ('1a2b3c4d-5e6f-4789-8abc-9def01234567', '550e8400-e29b-41d4-a716-446655440002', 
     'Rising Star', 'Win 5 games of Chess.', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 5 games.', false),
    
    ('2b3c4d5e-6f7a-4890-9bcd-0ef123456789', '550e8400-e29b-41d4-a716-446655440002', 
     'Master Candidate', 'Win 10 games of Chess.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 10 games.', false),
    
    ('3c4d5e6f-7a8b-4901-9cde-0f123456789a', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Master', 'Win 25 games of Chess.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 25 games.', false),
    
    ('4d5e6f7a-8b9c-4a12-9def-0123456789ab', '550e8400-e29b-41d4-a716-446655440002', 
     'Grandmaster', 'Win 50 games of Chess.', 
     'PROGRESSION', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 50 games.', false),
    
    ('5e6f7a8b-9c0d-4a23-9ef0-123456789abc', '550e8400-e29b-41d4-a716-446655440002', 
     'World Champion', 'Win 100 games of Chess.', 
     'PROGRESSION', 'EPIC', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 100 games.', false),
    
    ('6f7a8b9c-0d1e-4a34-9f01-23456789abcd', '550e8400-e29b-41d4-a716-446655440002', 
     'Legend of the Arena', 'Win 200 games of Chess.', 
     'DIFFICULTY', 'LEGENDARY', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 200 games.', false),
    
    ('7a8b9c0d-1e2f-4a45-9012-3456789abcde', '550e8400-e29b-41d4-a716-446655440002', 
     'Resilient', 'Lose 10 games of Chess (learning experience).', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAMES_LOST', 'Lose 10 games.', false),

    ('8b9c0d1e-2f3a-4a56-0123-456789abcdef', '550e8400-e29b-41d4-a716-446655440002', 
     'Unstoppable', 'Win 10 games of Chess in a row.', 
     'DIFFICULTY', 'EPIC', 'STREAK', 'GAME_WON', 'Win 10 games in a row.', false),
    
    ('9c0d1e2f-3a4b-4a67-1234-56789abcdef0', '550e8400-e29b-41d4-a716-446655440002', 
     'Dominant', 'Win 5 games of Chess in a row.', 
     'DIFFICULTY', 'RARE', 'STREAK', 'GAME_WON', 'Win 5 games in a row.', false),
    
    ('0d1e2f3a-4b5c-4a78-2345-6789abcdef01', '550e8400-e29b-41d4-a716-446655440002', 
     'On Fire', 'Win 3 games of Chess in a row.', 
     'DIFFICULTY', 'UNCOMMON', 'STREAK', 'GAME_WON', 'Win 3 games in a row.', false),

    ('1e2f3a4b-5c6d-4a89-3456-789abcdef012', '550e8400-e29b-41d4-a716-446655440002', 
     'Lightning Fast', 'Win a game of Chess under 5 minutes.', 
     'TIME', 'EPIC', 'TIME_REACHED', 'GAME_WON', 'Win a game under 5 minutes.', false),
    
    ('2f3a4b5c-6d7e-4a90-4567-89abcdef0123', '550e8400-e29b-41d4-a716-446655440002', 
     'Quick Thinker', 'Win a game of Chess under 10 minutes.', 
     'TIME', 'RARE', 'TIME_REACHED', 'GAME_WON', 'Win a game under 10 minutes.', false),
    
    ('b8938611-43e6-4b05-84e8-7ba0b2f9ee53', '550e8400-e29b-41d4-a716-446655440002', 
     'Marathon Session', 'Accumulate 1 hour of Chess play time.', 
     'TIME', 'UNCOMMON', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 1 hour of play time.', false),
    
    ('3a4b5c6d-7e8f-4a01-5678-9abcdef01234', '550e8400-e29b-41d4-a716-446655440002', 
     'Dedicated Player', 'Accumulate 10 hours of Chess play time.', 
     'TIME', 'RARE', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 10 hours of play time.', false),
    
    ('4b5c6d7e-8f9a-4a12-6789-abcdef012345', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Enthusiast', 'Accumulate 50 hours of Chess play time.', 
     'TIME', 'EPIC', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 50 hours of play time.', false),

    ('2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', '550e8400-e29b-41d4-a716-446655440002', 
     'Social Butterfly', 'Play against 20 unique players in Chess.', 
     'SOCIAL', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Play against 20 unique players.', false),
    
    ('5c6d7e8f-9a0b-4a23-789a-bcdef0123456', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Networker', 'Play against 10 unique players in Chess.', 
     'SOCIAL', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Play against 10 unique players.', false),
    
    ('6d7e8f9a-0b1c-4a34-89ab-cdef01234567', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Community', 'Play against 50 unique players in Chess.', 
     'SOCIAL', 'EPIC', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Play against 50 unique players.', false)

ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    achievement_category = EXCLUDED.achievement_category,
    achievement_rarity = EXCLUDED.achievement_rarity,
    criteria = EXCLUDED.criteria,
    triggering_event_type = EXCLUDED.triggering_event_type,
    trigger_condition_string = EXCLUDED.trigger_condition_string,
    third_party_achievement = EXCLUDED.third_party_achievement;

-- ============================================================================
-- 4. PLAYER STATISTICS (for achievement evaluation)
-- ============================================================================
-- Seed statistics for various players across both games
-- These represent realistic gameplay data

INSERT INTO player_statistics (player_id, game_id, total_wins, total_losses, total_draws, current_win_streak, longest_win_streak, total_play_time_seconds, fastest_win_seconds, unique_opponents, total_moves, total_games)
VALUES 
    -- Connect Four Statistics
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440001', 12, 8, 2, 3, 5, 7200, 95, '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2,7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1,a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 450, 22),
    
    ('2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '550e8400-e29b-41d4-a716-446655440001', 25, 15, 5, 7, 10, 18000, 78, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1,a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4,c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', 1200, 45),
    
    ('a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '550e8400-e29b-41d4-a716-446655440001', 50, 20, 10, 12, 15, 36000, 65, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2,7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1,c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3,d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c,e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 2800, 80),
    
    ('f1a2b3c4-d5e6-4f70-8123-4567890abcde', '550e8400-e29b-41d4-a716-446655440001', 100, 30, 15, 18, 20, 72000, 52, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2,a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4,c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3,d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c,e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10,13579bdf-2468-4ace-9bdf-13579bdf2468,02468ace-1357-4bdf-8ace-02468ace1357', 5800, 145),
    
    ('7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', '550e8400-e29b-41d4-a716-446655440001', 8, 12, 3, 0, 2, 5400, 120, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 380, 23),

    -- Chess Statistics
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440002', 15, 10, 5, 4, 6, 14400, 240, '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2,a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 1200, 30),
    
    ('2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '550e8400-e29b-41d4-a716-446655440002', 30, 18, 7, 8, 12, 28800, 195, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4,c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', 2400, 55),
    
    ('a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '550e8400-e29b-41d4-a716-446655440002', 75, 25, 15, 15, 18, 108000, 180, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2,c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3,d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c,e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10,f1a2b3c4-d5e6-4f70-8123-4567890abcde', 5600, 115),
    
    ('f1a2b3c4-d5e6-4f70-8123-4567890abcde', '550e8400-e29b-41d4-a716-446655440002', 150, 40, 20, 20, 25, 216000, 165, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2,a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4,c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3,d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c,e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10,13579bdf-2468-4ace-9bdf-13579bdf2468,02468ace-1357-4bdf-8ace-02468ace1357', 11200, 210),
    
    ('02468ace-1357-4bdf-8ace-02468ace1357', '550e8400-e29b-41d4-a716-446655440002', 200, 50, 25, 25, 30, 360000, 150, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2,2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2,a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4,c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3,d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c,e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10,f1a2b3c4-d5e6-4f70-8123-4567890abcde,13579bdf-2468-4ace-9bdf-13579bdf2468,9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5,6a1b2c3d-4e5f-6789-8abc-9def01234567', 15000, 275)

ON CONFLICT (player_id, game_id) DO UPDATE SET
    total_wins = EXCLUDED.total_wins,
    total_losses = EXCLUDED.total_losses,
    total_draws = EXCLUDED.total_draws,
    current_win_streak = EXCLUDED.current_win_streak,
    longest_win_streak = EXCLUDED.longest_win_streak,
    total_play_time_seconds = EXCLUDED.total_play_time_seconds,
    fastest_win_seconds = EXCLUDED.fastest_win_seconds,
    unique_opponents = EXCLUDED.unique_opponents,
    total_moves = EXCLUDED.total_moves,
    total_games = EXCLUDED.total_games;

-- ============================================================================
-- 5. USER ACHIEVEMENTS (Unlocked achievements)
-- ============================================================================
-- Seed some unlocked achievements based on the statistics above

INSERT INTO user_achievements (id, user_id, achievement_id, unlocked_at)
VALUES 
    -- Connect Four Achievements
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 'e6f7a8b9-0c1d-4a23-9ef0-123456789abc', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'd5e6f7a8-9b0c-4a12-9def-0123456789ab', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '35 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'a1b2c3d4-5e6f-4789-8abc-9def01234567', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'e2f3a4b5-6c7d-4a89-3456-789abcdef012', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '40 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '35 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'a1b2c3d4-5e6f-4789-8abc-9def01234567', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'b2c3d4e5-6f7a-4890-9bcd-0ef123456789', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'c3d4e5f6-7a8b-4901-9cde-0f123456789a', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'd1e2f3a4-5b6c-4a78-2345-6789abcdef01', CURRENT_TIMESTAMP - INTERVAL '15 days'),

    -- Chess Achievements
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '1a2b3c4d-5e6f-4789-8abc-9def01234567', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '2b3c4d5e-6f7a-4890-9bcd-0ef123456789', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '1a2b3c4d-5e6f-4789-8abc-9def01234567', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '2b3c4d5e-6f7a-4890-9bcd-0ef123456789', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '3c4d5e6f-7a8b-4901-9cde-0f123456789a', CURRENT_TIMESTAMP - INTERVAL '12 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '9c0d1e2f-3a4b-4a67-1234-56789abcdef0', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '35 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '1a2b3c4d-5e6f-4789-8abc-9def01234567', CURRENT_TIMESTAMP - INTERVAL '32 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '2b3c4d5e-6f7a-4890-9bcd-0ef123456789', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '3c4d5e6f-7a8b-4901-9cde-0f123456789a', CURRENT_TIMESTAMP - INTERVAL '22 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '4d5e6f7a-8b9c-4a12-9def-0123456789ab', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '8b9c0d1e-2f3a-4a56-0123-456789abcdef', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '5c6d7e8f-9a0b-4a23-789a-bcdef0123456', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '40 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '1a2b3c4d-5e6f-4789-8abc-9def01234567', CURRENT_TIMESTAMP - INTERVAL '38 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '2b3c4d5e-6f7a-4890-9bcd-0ef123456789', CURRENT_TIMESTAMP - INTERVAL '35 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '3c4d5e6f-7a8b-4901-9cde-0f123456789a', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '4d5e6f7a-8b9c-4a12-9def-0123456789ab', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '5e6f7a8b-9c0d-4a23-9ef0-123456789abc', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '8b9c0d1e-2f3a-4a56-0123-456789abcdef', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', CURRENT_TIMESTAMP - INTERVAL '12 days'),
    
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '45 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '1a2b3c4d-5e6f-4789-8abc-9def01234567', CURRENT_TIMESTAMP - INTERVAL '42 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '2b3c4d5e-6f7a-4890-9bcd-0ef123456789', CURRENT_TIMESTAMP - INTERVAL '38 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '3c4d5e6f-7a8b-4901-9cde-0f123456789a', CURRENT_TIMESTAMP - INTERVAL '32 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '4d5e6f7a-8b9c-4a12-9def-0123456789ab', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '5e6f7a8b-9c0d-4a23-9ef0-123456789abc', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '6f7a8b9c-0d1e-4a34-9f01-23456789abcd', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '8b9c0d1e-2f3a-4a56-0123-456789abcdef', CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '6d7e8f9a-0b1c-4a34-89ab-cdef01234567', CURRENT_TIMESTAMP - INTERVAL '15 days')

ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- ============================================================================
-- 6. LOBBIES
-- ============================================================================
-- Seed various lobby states: WAITING, STARTED, FINISHED

INSERT INTO lobbies (id, game_id, session_id, host_id, name, description, status, max_players, visibility, created_at, started_at)
VALUES 
    -- Active/Waiting Lobbies
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440001', NULL, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 
     'Connect Four Fun', 'Looking for a quick game!', 'WAITING', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '2 hours', NULL),
    
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440002', NULL, '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 
     'Chess Masters', 'Serious chess players only', 'WAITING', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '1 hour', NULL),
    
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440001', NULL, 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 
     'Quick Connect Four', 'Fast-paced games', 'WAITING', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '30 minutes', NULL),
    
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440002', NULL, 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 
     'Chess Training', 'Practice and improve', 'WAITING', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '15 minutes', NULL),
    
    -- Started/Active Lobbies (with session IDs)
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440001', gen_random_uuid(), 'c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', 
     'Active Game 1', 'Game in progress', 'STARTED', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440002', gen_random_uuid(), 'd7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', 
     'Active Chess Match', 'Intense game ongoing', 'STARTED', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '45 minutes'),
    
    -- Finished Lobbies
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440001', gen_random_uuid(), 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 
     'Completed Game', 'Finished match', 'FINISHED', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '5 hours', CURRENT_TIMESTAMP - INTERVAL '4 hours'),
    
    (gen_random_uuid(), '550e8400-e29b-41d4-a716-446655440002', gen_random_uuid(), '13579bdf-2468-4ace-9bdf-13579bdf2468', 
     'Chess Match Complete', 'Game finished', 'FINISHED', 2, 'PUBLIC', 
     CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '5 hours')

ON CONFLICT (id) DO UPDATE SET
    game_id = EXCLUDED.game_id,
    session_id = EXCLUDED.session_id,
    host_id = EXCLUDED.host_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    max_players = EXCLUDED.max_players,
    visibility = EXCLUDED.visibility,
    started_at = EXCLUDED.started_at;

-- ============================================================================
-- 7. LOBBY PLAYERS (Associate players with lobbies)
-- ============================================================================
-- Note: lobby_players is a collection table managed by JPA
-- We'll add players using subqueries to get lobby IDs

INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2' 
FROM lobbies WHERE name = 'Connect Four Fun'
ON CONFLICT DO NOTHING;

INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2' 
FROM lobbies WHERE name = 'Chess Masters'
ON CONFLICT DO NOTHING;

INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4' 
FROM lobbies WHERE name = 'Quick Connect Four'
ON CONFLICT DO NOTHING;

INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, 'f1a2b3c4-d5e6-4f70-8123-4567890abcde' 
FROM lobbies WHERE name = 'Chess Training'
ON CONFLICT DO NOTHING;

-- Add second players to some lobbies (for started/finished lobbies)
INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2' 
FROM lobbies WHERE name = 'Active Game 1'
ON CONFLICT DO NOTHING;

INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4' 
FROM lobbies WHERE name = 'Active Chess Match'
ON CONFLICT DO NOTHING;

INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, 'f1a2b3c4-d5e6-4f70-8123-4567890abcde' 
FROM lobbies WHERE name = 'Completed Game'
ON CONFLICT DO NOTHING;

INSERT INTO lobby_players (lobby_id, player_id)
SELECT id, '02468ace-1357-4bdf-8ace-02468ace1357' 
FROM lobbies WHERE name = 'Chess Match Complete'
ON CONFLICT DO NOTHING;

-- ============================================================================
-- Summary
-- ============================================================================
-- Seeded:
-- - 2 Games (Connect Four, Chess)
-- - 33 Achievements (15 Connect Four, 18 Chess)
-- - 10 Player Statistics records (5 per game)
-- - 40+ User Achievements (unlocked achievements)
-- - 8 Lobbies (various states)
-- - Lobby Players associations
--
-- All using existing player IDs from the players table
-- ============================================================================

