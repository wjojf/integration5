-- ============================================================================
-- Achievements Loading Script for Connect Four and Chess
-- ============================================================================
-- This script loads achievements for both Connect Four and Chess games
-- 
-- Game IDs:
-- - Connect Four: 550e8400-e29b-41d4-a716-446655440001
-- - Chess: 550e8400-e29b-41d4-a716-446655440002
--
-- Achievement Criteria Types:
-- - COUNTER_REACHES_THRESHOLD: For cumulative achievements (e.g., "Win 10 games")
-- - STREAK: For consecutive achievements (e.g., "Win 10 games in a row")
-- - ONE_TIME_EVENT: For first-time achievements (e.g., "First Victory")
-- - TIME_REACHED: For time-based achievements (e.g., "Win under 2 minutes")
--
-- Triggering Event Types:
-- - GAME_WON: Triggered when player wins
-- - GAMES_LOST: Triggered when player loses
-- - TIME_PASSED: Triggered based on time accumulation
-- ============================================================================

-- Connect Four Achievements
-- ============================================================================

-- Progression Achievements (Counter-based)
INSERT INTO achievements (id, game_id, name, description, achievement_category, achievement_rarity, criteria, triggering_event_type, trigger_condition_string, third_party_achievement)
VALUES 
    -- First Victory (One-time)
    ('2c381abf-6522-4ce1-b2c2-24d40a6b8f75', '550e8400-e29b-41d4-a716-446655440001', 
     'First Victory', 'Win your first game of Connect Four.', 
     'PROGRESSION', 'COMMON', 'ONE_TIME_EVENT', 'GAME_WON', 
     'Win your first game.', false),
    
    -- Getting Warm (Counter: 5 wins)
    ('8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', '550e8400-e29b-41d4-a716-446655440001', 
     'Getting Warm', 'Win 5 games of Connect Four.', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 5 games.', false),
    
    -- Double Digits (Counter: 10 wins)
    ('f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', '550e8400-e29b-41d4-a716-446655440001', 
     'Double Digits', 'Win 10 games of Connect Four.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 10 games.', false),
    
    -- Quarter Century (Counter: 25 wins)
    ('a1b2c3d4-5e6f-4789-8abc-9def01234567', '550e8400-e29b-41d4-a716-446655440001', 
     'Quarter Century', 'Win 25 games of Connect Four.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 25 games.', false),
    
    -- Half Century (Counter: 50 wins)
    ('b2c3d4e5-6f7a-4890-9bcd-0ef123456789', '550e8400-e29b-41d4-a716-446655440001', 
     'Half Century', 'Win 50 games of Connect Four.', 
     'PROGRESSION', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 50 games.', false),
    
    -- Centurion (Counter: 100 wins)
    ('c3d4e5f6-7a8b-4901-9cde-0f123456789a', '550e8400-e29b-41d4-a716-446655440001', 
     'Centurion', 'Win 100 games of Connect Four.', 
     'PROGRESSION', 'EPIC', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 100 games.', false),
    
    -- Comeback Kid (Counter: 10 losses)
    ('4e96f75b-23bb-4e2f-b919-9fd7c2d0b9f0', '550e8400-e29b-41d4-a716-446655440001', 
     'Comeback Kid', 'Lose 10 games of Connect Four (hey, it happens).', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAMES_LOST', 
     'Lose 10 games.', false),

-- Difficulty Achievements (Streak-based)
    -- Unbreakable (Streak: 10 wins in a row)
    ('b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', '550e8400-e29b-41d4-a716-446655440001', 
     'Unbreakable', 'Win 10 games of Connect Four in a row.', 
     'DIFFICULTY', 'EPIC', 'STREAK', 'GAME_WON', 
     'Win 10 games in a row.', false),
    
    -- Dominator (Streak: 5 wins in a row)
    ('d5e6f7a8-9b0c-4a12-9def-0123456789ab', '550e8400-e29b-41d4-a716-446655440001', 
     'Dominator', 'Win 5 games of Connect Four in a row.', 
     'DIFFICULTY', 'RARE', 'STREAK', 'GAME_WON', 
     'Win 5 games in a row.', false),
    
    -- Hot Streak (Streak: 3 wins in a row)
    ('e6f7a8b9-0c1d-4a23-9ef0-123456789abc', '550e8400-e29b-41d4-a716-446655440001', 
     'Hot Streak', 'Win 3 games of Connect Four in a row.', 
     'DIFFICULTY', 'UNCOMMON', 'STREAK', 'GAME_WON', 
     'Win 3 games in a row.', false),

-- Time-based Achievements
    -- Speed Runner (Time: win under 2 minutes)
    ('f7a8b9c0-1d2e-4a34-9f01-23456789abcd', '550e8400-e29b-41d4-a716-446655440001', 
     'Speed Runner', 'Win a game of Connect Four under 2 minutes.', 
     'TIME', 'EPIC', 'TIME_REACHED', 'GAME_WON', 
     'Win a game under 2 minutes.', false),
    
    -- Quick Win (Time: win under 5 minutes)
    ('a8b9c0d1-2e3f-4a45-9012-3456789abcde', '550e8400-e29b-41d4-a716-446655440001', 
     'Quick Win', 'Win a game of Connect Four under 5 minutes.', 
     'TIME', 'RARE', 'TIME_REACHED', 'GAME_WON', 
     'Win a game under 5 minutes.', false),
    
    -- Marathon Session (Time: 1 hour total)
    ('b9c0d1e2-3f4a-4a56-0123-456789abcdef', '550e8400-e29b-41d4-a716-446655440001', 
     'Marathon Session', 'Accumulate 1 hour of Connect Four play time.', 
     'TIME', 'UNCOMMON', 'TIME_REACHED', 'TIME_PASSED', 
     'Accumulate 1 hour of play time.', false),
    
    -- Weekend Warrior (Time: 10 hours total)
    ('c0d1e2f3-4a5b-4a67-1234-56789abcdef0', '550e8400-e29b-41d4-a716-446655440001', 
     'Weekend Warrior', 'Accumulate 10 hours of Connect Four play time.', 
     'TIME', 'RARE', 'TIME_REACHED', 'TIME_PASSED', 
     'Accumulate 10 hours of play time.', false),

-- Social Achievements
    -- Social Butterfly (Social: 20 unique opponents)
    ('d1e2f3a4-5b6c-4a78-2345-6789abcdef01', '550e8400-e29b-41d4-a716-446655440001', 
     'Social Butterfly', 'Play against 20 unique players in Connect Four.', 
     'SOCIAL', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Play against 20 unique players.', false),
    
    -- Networker (Social: 10 unique opponents)
    ('e2f3a4b5-6c7d-4a89-3456-789abcdef012', '550e8400-e29b-41d4-a716-446655440001', 
     'Networker', 'Play against 10 unique players in Connect Four.', 
     'SOCIAL', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Play against 10 unique players.', false);

-- Chess Achievements
-- ============================================================================

INSERT INTO achievements (id, game_id, name, description, achievement_category, achievement_rarity, criteria, triggering_event_type, trigger_condition_string, third_party_achievement)
VALUES 
    -- Progression Achievements (Counter-based)
    ('efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', '550e8400-e29b-41d4-a716-446655440002', 
     'First Checkmate', 'Win your first game of Chess.', 
     'PROGRESSION', 'COMMON', 'ONE_TIME_EVENT', 'GAME_WON', 
     'Win your first game.', false),
    
    ('1a2b3c4d-5e6f-4789-8abc-9def01234567', '550e8400-e29b-41d4-a716-446655440002', 
     'Rising Star', 'Win 5 games of Chess.', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 5 games.', false),
    
    ('2b3c4d5e-6f7a-4890-9bcd-0ef123456789', '550e8400-e29b-41d4-a716-446655440002', 
     'Master Candidate', 'Win 10 games of Chess.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 10 games.', false),
    
    ('3c4d5e6f-7a8b-4901-9cde-0f123456789a', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Master', 'Win 25 games of Chess.', 
     'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 25 games.', false),
    
    ('4d5e6f7a-8b9c-4a12-9def-0123456789ab', '550e8400-e29b-41d4-a716-446655440002', 
     'Grandmaster', 'Win 50 games of Chess.', 
     'PROGRESSION', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 50 games.', false),
    
    ('5e6f7a8b-9c0d-4a23-9ef0-123456789abc', '550e8400-e29b-41d4-a716-446655440002', 
     'World Champion', 'Win 100 games of Chess.', 
     'PROGRESSION', 'EPIC', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 100 games.', false),
    
    ('6f7a8b9c-0d1e-4a34-9f01-23456789abcd', '550e8400-e29b-41d4-a716-446655440002', 
     'Legend of the Arena', 'Win 200 games of Chess.', 
     'DIFFICULTY', 'LEGENDARY', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Win 200 games.', false),
    
    ('7a8b9c0d-1e2f-4a45-9012-3456789abcde', '550e8400-e29b-41d4-a716-446655440002', 
     'Resilient', 'Lose 10 games of Chess (learning experience).', 
     'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAMES_LOST', 
     'Lose 10 games.', false),

    -- Difficulty Achievements (Streak-based)
    ('8b9c0d1e-2f3a-4a56-0123-456789abcdef', '550e8400-e29b-41d4-a716-446655440002', 
     'Unstoppable', 'Win 10 games of Chess in a row.', 
     'DIFFICULTY', 'EPIC', 'STREAK', 'GAME_WON', 
     'Win 10 games in a row.', false),
    
    ('9c0d1e2f-3a4b-4a67-1234-56789abcdef0', '550e8400-e29b-41d4-a716-446655440002', 
     'Dominant', 'Win 5 games of Chess in a row.', 
     'DIFFICULTY', 'RARE', 'STREAK', 'GAME_WON', 
     'Win 5 games in a row.', false),
    
    ('0d1e2f3a-4b5c-4a78-2345-6789abcdef01', '550e8400-e29b-41d4-a716-446655440002', 
     'On Fire', 'Win 3 games of Chess in a row.', 
     'DIFFICULTY', 'UNCOMMON', 'STREAK', 'GAME_WON', 
     'Win 3 games in a row.', false),

    -- Time-based Achievements
    ('1e2f3a4b-5c6d-4a89-3456-789abcdef012', '550e8400-e29b-41d4-a716-446655440002', 
     'Lightning Fast', 'Win a game of Chess under 5 minutes.', 
     'TIME', 'EPIC', 'TIME_REACHED', 'GAME_WON', 
     'Win a game under 5 minutes.', false),
    
    ('2f3a4b5c-6d7e-4a90-4567-89abcdef0123', '550e8400-e29b-41d4-a716-446655440002', 
     'Quick Thinker', 'Win a game of Chess under 10 minutes.', 
     'TIME', 'RARE', 'TIME_REACHED', 'GAME_WON', 
     'Win a game under 10 minutes.', false),
    
    ('b8938611-43e6-4b05-84e8-7ba0b2f9ee53', '550e8400-e29b-41d4-a716-446655440002', 
     'Marathon Session', 'Accumulate 1 hour of Chess play time.', 
     'TIME', 'UNCOMMON', 'TIME_REACHED', 'TIME_PASSED', 
     'Accumulate 1 hour of play time.', false),
    
    ('3a4b5c6d-7e8f-4a01-5678-9abcdef01234', '550e8400-e29b-41d4-a716-446655440002', 
     'Dedicated Player', 'Accumulate 10 hours of Chess play time.', 
     'TIME', 'RARE', 'TIME_REACHED', 'TIME_PASSED', 
     'Accumulate 10 hours of play time.', false),
    
    ('4b5c6d7e-8f9a-4a12-6789-abcdef012345', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Enthusiast', 'Accumulate 50 hours of Chess play time.', 
     'TIME', 'EPIC', 'TIME_REACHED', 'TIME_PASSED', 
     'Accumulate 50 hours of play time.', false),

    -- Social Achievements
    ('2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', '550e8400-e29b-41d4-a716-446655440002', 
     'Social Butterfly', 'Play against 20 unique players in Chess.', 
     'SOCIAL', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Play against 20 unique players.', false),
    
    ('5c6d7e8f-9a0b-4a23-789a-bcdef0123456', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Networker', 'Play against 10 unique players in Chess.', 
     'SOCIAL', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Play against 10 unique players.', false),
    
    ('6d7e8f9a-0b1c-4a34-89ab-cdef01234567', '550e8400-e29b-41d4-a716-446655440002', 
     'Chess Community', 'Play against 50 unique players in Chess.', 
     'SOCIAL', 'EPIC', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 
     'Play against 50 unique players.', false);

-- ============================================================================
-- Summary
-- ============================================================================
-- Connect Four Achievements: 15 achievements
--   - Progression: 7 (First Victory, 5, 10, 25, 50, 100 wins, 10 losses)
--   - Difficulty: 3 (3, 5, 10 win streaks)
--   - Time: 4 (2 min, 5 min wins, 1 hour, 10 hours total)
--   - Social: 2 (10, 20 unique opponents)
--
-- Chess Achievements: 18 achievements
--   - Progression: 8 (First Checkmate, 5, 10, 25, 50, 100, 200 wins, 10 losses)
--   - Difficulty: 3 (3, 5, 10 win streaks)
--   - Time: 5 (5 min, 10 min wins, 1 hour, 10 hours, 50 hours total)
--   - Social: 3 (10, 20, 50 unique opponents)
--
-- Total: 33 achievements across both games
-- ============================================================================

