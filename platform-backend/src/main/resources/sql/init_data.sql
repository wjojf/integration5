-- ============================================================================
-- Comprehensive Idempotent Data Initialization Script
-- ============================================================================
-- This script initializes all platform data including:
-- - Database schema (tables, columns, indexes)
-- - Games (Connect Four, Chess)
-- - Achievements (for both games)
-- - Players (test users)
-- - Player Statistics (for achievement evaluation)
-- - User Achievements (unlocked achievements)
-- - Lobbies (various states)
-- - Lobby Players associations
-- - Friendships
-- - Messages
--
-- This script is fully idempotent and can be run multiple times safely.
-- Uses IF NOT EXISTS, ON CONFLICT, and simple SQL constructs only.
-- ============================================================================

-- ============================================================================
-- 1. SCHEMA SETUP
-- ============================================================================

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
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS trigger_condition_string TEXT;
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS third_party_achievement BOOLEAN DEFAULT FALSE;
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS code VARCHAR(255);
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS criteria VARCHAR(50);
ALTER TABLE achievements ADD COLUMN IF NOT EXISTS triggering_event_type VARCHAR(50);

-- ============================================================================
-- 2. GAMES TABLE (if not exists)
-- ============================================================================

CREATE TABLE IF NOT EXISTS games (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    genre VARCHAR(100),
    image VARCHAR(500)
);

-- ============================================================================
-- 3. GAMES DATA
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
-- 3. PLAYERS
-- ============================================================================

INSERT INTO players (player_id, username, bio, email, address, rank, exp)
VALUES 
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 'user1',
     'Co-op first, chaos second. I main support but I''m not above a little trolling (lovingly).',
     'user1@banditgames.com', '221B Baker St, London, UK', 'BRONZE', 120),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', 'admin',
     'Co-op first, chaos second. I main support but I''m not above a little trolling (lovingly).',
     'admin@banditgames.com', '221B Baker St, London, UK', 'BRONZE', 120),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', 'bandit_ace',
     'Co-op first, chaos second. I main support but I''m not above a little trolling (lovingly).',
     'bandit_ace@example.com', '221B Baker St, London, UK', 'BRONZE', 120),
    ('2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'no_scope_nina',
     'Night owl gamer. Competitive when it counts, chill when it doesn''t.',
     'no_scope_nina@example.com', '350 5th Ave, New York, NY 10118, USA', 'SILVER', 1450),
    ('7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'lootlin22',
     'Exploring worlds, collecting loot, and forgetting to craft potions until it''s too late.',
     'lootlin22@example.com', '1 Chome-1-2 Oshiage, Sumida City, Tokyo 131-0045, Japan', 'GOLD', 5200),
    ('a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'player_one',
     'Strategy brain, reflex hands. If there''s a ladder, I''m climbing it.',
     'player_one@example.com', 'Pariser Platz, 10117 Berlin, Germany', 'PLATINUM', 11200),
    ('c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', 'tiltproof_tom',
     'Ranked enjoyer. Tilt-resistant. Mostly.',
     'tiltproof_tom@example.com', '10 Market St, Sydney NSW 2000, Australia', 'SILVER', 2100),
    ('d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', 'indie_ivy',
     'Achievement hunter with a soft spot for roguelikes and weird indie gems.',
     'indie_ivy@example.com', 'Av. Atlântica, Copacabana, Rio de Janeiro - RJ, Brazil', 'GOLD', 6800),
    ('e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 'clip_chris',
     'Here for good teammates, clean plays, and ridiculous moments worth clipping.',
     'clip_chris@example.com', '1600 Amphitheatre Pkwy, Mountain View, CA 94043, USA', 'PLATINUM', 14950),
    ('f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'builder_bea',
     'Builder, breaker, problem-solver. I like games that reward creativity.',
     'builder_bea@example.com', '100 King St W, Toronto, ON M5X 1A9, Canada', 'DIAMOND', 30500),
    ('13579bdf-2468-4ace-9bdf-13579bdf2468', 'speedy_sam',
     'Movement tech enjoyer. If it has dashes, slides, or grapples, I''m in.',
     'speedy_sam@example.com', 'Bandra Kurla Complex, Mumbai, Maharashtra 400051, India', 'GOLD', 8400),
    ('02468ace-1357-4bdf-8ace-02468ace1357', 'zen_zara',
     'Calm comms, smart rotates, and a clean endgame.',
     'zen_zara@example.com', 'Sheikh Zayed Rd, Dubai, United Arab Emirates', 'DIAMOND', 28750)
ON CONFLICT (player_id) DO NOTHING;

-- ============================================================================
-- 4. ACHIEVEMENTS (Connect Four)
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
    ('b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', '550e8400-e29b-41d4-a716-446655440001', 
     'Unbreakable', 'Win 10 games of Connect Four in a row.', 
     'DIFFICULTY', 'EPIC', 'STREAK', 'GAME_WON', 'Win 10 games in a row.', false),
    ('4e96f75b-23bb-4e2f-b919-9fd7c2d0b9f0', '550e8400-e29b-41d4-a716-446655440001', 
     'Comeback Kid', 'Lose 10 games of Connect Four (hey, it happens).', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAMES_LOST', 'Lose 10 games.', false)
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
-- 5. ACHIEVEMENTS TABLE (if not exists)
-- ============================================================================

CREATE TABLE IF NOT EXISTS achievements (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    achievement_category VARCHAR(50),
    achievement_rarity VARCHAR(50),
    criteria VARCHAR(50),
    triggering_event_type VARCHAR(50),
    trigger_condition_string TEXT,
    third_party_achievement BOOLEAN DEFAULT FALSE,
    code VARCHAR(255),
    CONSTRAINT fk_achievements_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- ============================================================================
-- 6. ACHIEVEMENTS (Chess)
-- ============================================================================

INSERT INTO achievements (id, game_id, name, description, achievement_category, achievement_rarity, criteria, triggering_event_type, trigger_condition_string, third_party_achievement)
VALUES 
    ('efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', '550e8400-e29b-41d4-a716-446655440002', 
     'Marathon Session', 'Accumulate 1 hour of play time.', 
     'TIME', 'UNCOMMON', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 1 hour of play time.', false),
    ('b8938611-43e6-4b05-84e8-7ba0b2f9ee53', '550e8400-e29b-41d4-a716-446655440002', 
     'Weekend Warrior', 'Accumulate 10 hours of play time.', 
     'TIME', 'RARE', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 10 hours of play time.', false),
    ('2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', '550e8400-e29b-41d4-a716-446655440002', 
     'Social Butterfly', 'Play against 20 unique players.', 
     'SOCIAL', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Play against 20 unique players.', false),
    ('5fe78f61-8a2e-4c90-b4bb-14d9a1d1c36c', '550e8400-e29b-41d4-a716-446655440002',
     'Speed Runner', 'Win a game under 2 minutes.', 
     'TIME', 'EPIC', 'TIME_REACHED', 'GAME_WON', 'Win a game under 2 minutes.', false),
    ('d1bce2e4-f96c-4b9d-a0a9-0d10e1a2c28a', '550e8400-e29b-41d4-a716-446655440002', 
     'Legend of the Arena', 'Achieve an elite milestone reserved for top players.', 
     'DIFFICULTY', 'LEGENDARY', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 100 games.', false)
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
-- 7. PLAYER GAME PREFERENCES
-- ============================================================================

INSERT INTO player_game_preferences (player_id, game_id)
VALUES
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440001'),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440002'),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '550e8400-e29b-41d4-a716-446655440001'),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '550e8400-e29b-41d4-a716-446655440002'),
    ('2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '550e8400-e29b-41d4-a716-446655440001'),
    ('7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', '550e8400-e29b-41d4-a716-446655440001'),
    ('a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '550e8400-e29b-41d4-a716-446655440002'),
    ('c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', '550e8400-e29b-41d4-a716-446655440001'),
    ('d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', '550e8400-e29b-41d4-a716-446655440002'),
    ('e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', '550e8400-e29b-41d4-a716-446655440001'),
    ('f1a2b3c4-d5e6-4f70-8123-4567890abcde', '550e8400-e29b-41d4-a716-446655440001'),
    ('13579bdf-2468-4ace-9bdf-13579bdf2468', '550e8400-e29b-41d4-a716-446655440002'),
    ('02468ace-1357-4bdf-8ace-02468ace1357', '550e8400-e29b-41d4-a716-446655440001')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 8. USER ACHIEVEMENTS TABLE (if not exists)
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    achievement_id UUID NOT NULL,
    unlocked_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, achievement_id),
    CONSTRAINT fk_user_achievements_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE
);

-- ============================================================================
-- 9. USER ACHIEVEMENTS DATA (Sample unlocked achievements)
-- ============================================================================

INSERT INTO user_achievements (id, user_id, achievement_id, unlocked_at)
VALUES
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', CURRENT_TIMESTAMP - INTERVAL '22 days')
ON CONFLICT (user_id, achievement_id) DO NOTHING;

-- ============================================================================
-- 10. PLAYER STATISTICS DATA (Sample statistics for achievement evaluation)
-- ============================================================================

INSERT INTO player_statistics (id, player_id, game_id, total_wins, total_losses, total_draws, current_win_streak, longest_win_streak, total_play_time_seconds, total_moves, total_games)
VALUES
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440001', 15, 5, 0, 3, 5, 3600, 150, 20),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440002', 8, 12, 0, 0, 2, 7200, 200, 20),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '550e8400-e29b-41d4-a716-446655440001', 25, 10, 0, 5, 8, 5400, 300, 35),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '550e8400-e29b-41d4-a716-446655440002', 50, 20, 0, 10, 12, 18000, 500, 70),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', '550e8400-e29b-41d4-a716-446655440001', 100, 15, 0, 15, 20, 36000, 1000, 115)
ON CONFLICT (player_id, game_id) DO UPDATE SET
    total_wins = EXCLUDED.total_wins,
    total_losses = EXCLUDED.total_losses,
    total_draws = EXCLUDED.total_draws,
    current_win_streak = EXCLUDED.current_win_streak,
    longest_win_streak = EXCLUDED.longest_win_streak,
    total_play_time_seconds = EXCLUDED.total_play_time_seconds,
    total_moves = EXCLUDED.total_moves,
    total_games = EXCLUDED.total_games;

-- ============================================================================
-- 11. LOBBIES TABLE (if not exists)
-- ============================================================================

CREATE TABLE IF NOT EXISTS lobbies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    game_id UUID,
    host_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    max_players INTEGER NOT NULL,
    visibility VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lobbies_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS lobby_players (
    lobby_id UUID NOT NULL,
    player_id UUID NOT NULL,
    PRIMARY KEY (lobby_id, player_id),
    CONSTRAINT fk_lobby_players_lobby FOREIGN KEY (lobby_id) REFERENCES lobbies(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lobby_external_game_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lobby_id UUID NOT NULL,
    game_id UUID NOT NULL,
    external_game_type VARCHAR(50) NOT NULL,
    external_game_instance_id UUID NOT NULL,
    UNIQUE(lobby_id),
    CONSTRAINT fk_external_game_lobby FOREIGN KEY (lobby_id) REFERENCES lobbies(id) ON DELETE CASCADE,
    CONSTRAINT fk_external_game_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

-- ============================================================================
-- 12. LOBBIES DATA (Sample lobbies in various states)
-- ============================================================================

INSERT INTO lobbies (id, name, description, game_id, host_id, status, max_players, visibility, created_at, started_at)
VALUES
    (gen_random_uuid(), 'Connect Four Fun', 'Casual Connect Four games', '550e8400-e29b-41d4-a716-446655440001', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 'WAITING', 2, 'PUBLIC', CURRENT_TIMESTAMP, NULL),
    (gen_random_uuid(), 'Chess Masters', 'Serious chess matches only', '550e8400-e29b-41d4-a716-446655440002', 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'WAITING', 2, 'PUBLIC', CURRENT_TIMESTAMP, NULL),
    (gen_random_uuid(), 'Quick Connect Four', 'Fast-paced games', '550e8400-e29b-41d4-a716-446655440001', '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'WAITING', 2, 'PUBLIC', CURRENT_TIMESTAMP, NULL),
    (gen_random_uuid(), 'Chess Training', 'Practice and learn', '550e8400-e29b-41d4-a716-446655440002', 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'WAITING', 2, 'PUBLIC', CURRENT_TIMESTAMP, NULL)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    game_id = EXCLUDED.game_id,
    host_id = EXCLUDED.host_id,
    status = EXCLUDED.status,
    max_players = EXCLUDED.max_players,
    visibility = EXCLUDED.visibility,
    started_at = EXCLUDED.started_at;

-- ============================================================================
-- 13. LOBBY PLAYERS DATA (Associations)
-- ============================================================================

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

-- ============================================================================
-- 14. FRIENDSHIPS TABLE (if not exists)
-- ============================================================================

CREATE TABLE IF NOT EXISTS friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL,
    addressee_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(requester_id, addressee_id)
);

-- ============================================================================
-- 15. FRIENDSHIPS DATA (Sample friendships)
-- ============================================================================

INSERT INTO friendships (id, requester_id, addressee_id, status, created_at, updated_at)
VALUES
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '35 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '29 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '13 days')
ON CONFLICT (requester_id, addressee_id) DO NOTHING;

-- ============================================================================
-- 16. MESSAGES TABLE (if not exists)
-- ============================================================================

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

-- ============================================================================
-- 17. MESSAGES DATA (Sample chat messages)
-- ============================================================================

INSERT INTO messages (id, sender_id, receiver_id, content, status, sent_at, read_at)
VALUES
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1',
     'Hey! Quick question — are we testing the matchmaking flow today?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '5 days 03:12:00', CURRENT_TIMESTAMP - INTERVAL '5 days 03:10:00'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2',
     'Yep — start with game 550e...400001. I''ll watch the logs.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '5 days 03:09:00', CURRENT_TIMESTAMP - INTERVAL '5 days 03:08:20'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4',
     'You always rotate early or was that a one-off?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '3 days 19:05:00', CURRENT_TIMESTAMP - INTERVAL '3 days 19:01:00')
ON CONFLICT (id) DO NOTHING;

