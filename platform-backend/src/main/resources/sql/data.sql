-- Players
-- Use ON CONFLICT to make inserts idempotent (won't fail if data already exists)
INSERT INTO players (player_id, username, bio, email, address, rank, exp)
VALUES ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 'user1',
        'Co-op first, chaos second. I main support but I’m not above a little trolling (lovingly).',
        'user1@banditgames.com', '221B Baker St, London, UK', 'BRONZE', 120),

       ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', 'admin',
        'Co-op first, chaos second. I main support but I’m not above a little trolling (lovingly).',
        'admin@banditgames.com', '221B Baker St, London, UK', 'BRONZE', 120),

       ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', 'bandit_ace',
        'Co-op first, chaos second. I main support but I’m not above a little trolling (lovingly).',
        'bandit_ace@example.com', '221B Baker St, London, UK', 'BRONZE', 120),

       ('2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'no_scope_nina',
        'Night owl gamer. Competitive when it counts, chill when it doesn’t.',
        'no_scope_nina@example.com', '350 5th Ave, New York, NY 10118, USA', 'SILVER', 1450),

       ('7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'lootlin22',
        'Exploring worlds, collecting loot, and forgetting to craft potions until it’s too late.',
        'lootlin22@example.com', '1 Chome-1-2 Oshiage, Sumida City, Tokyo 131-0045, Japan', 'GOLD', 5200),

       ('a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'player_one',
        'Strategy brain, reflex hands. If there’s a ladder, I’m climbing it.',
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
        'Movement tech enjoyer. If it has dashes, slides, or grapples, I’m in.',
        'speedy_sam@example.com', 'Bandra Kurla Complex, Mumbai, Maharashtra 400051, India', 'GOLD', 8400),

       ('02468ace-1357-4bdf-8ace-02468ace1357', 'zen_zara',
        'Calm comms, smart rotates, and a clean endgame.',
        'zen_zara@example.com', 'Sheikh Zayed Rd, Dubai, United Arab Emirates', 'DIAMOND', 28750),

       ('9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5', 'bandit_ace_2',
        'Co-op first, chaos second. I main support but I’m not above a little trolling (lovingly).',
        'bandit_ace_2@example.com', '4 Privet Drive, Little Whinging, Surrey, UK', 'BRONZE', 300),

       ('6a1b2c3d-4e5f-6789-8abc-9def01234567', 'no_rules',
        'Night owl gamer. Competitive when it counts, chill when it doesn’t.',
        'no_rules@example.com', '1 Infinite Loop, Cupertino, CA 95014, USA', 'SILVER', 1600),

       ('3e4f5a6b-7c8d-4e9f-a012-b345c678d901', 'lootlin',
        'Exploring worlds, collecting loot, and forgetting to craft potions until it’s too late.',
        'lootlin@example.com', 'Piazza del Colosseo, 1, 00184 Roma RM, Italy', 'GOLD', 5400),

       ('4b5c6d7e-8f90-4a1b-9c2d-3e4f5a6b7c8d', 'macro_mira',
        'Strategy brain, reflex hands. If there’s a ladder, I’m climbing it.',
        'macro_mira@example.com', 'Damrak 1-5, 1012 LG Amsterdam, Netherlands', 'PLATINUM', 11800),

       ('5c6d7e8f-9012-4b3c-8d4e-5f6a7b8c9d0e', 'tiltproof_tom_2',
        'Ranked enjoyer. Tilt-resistant. Mostly.',
        'tiltproof_tom_2@example.com', '1 Harbourfront Walk, Singapore 018956, Singapore', 'SILVER', 2400),

       ('7e8f9012-3a4b-4c5d-8e6f-0123456789ab', 'indie_ivy_2',
        'Achievement hunter with a soft spot for roguelikes and weird indie gems.',
        'indie_ivy_2@example.com', 'Gran Vía, 28013 Madrid, Spain', 'GOLD', 7100),

       ('8f901234-5b6c-4d7e-8f90-1a2b3c4d5e6f', 'clip_chris_2',
        'Here for good teammates, clean plays, and ridiculous moments worth clipping.',
        'clip_chris_2@example.com', 'Piazza del Duomo, 20122 Milano MI, Italy', 'PLATINUM', 15200),

       ('90123456-7c8d-4e9f-a012-3b4c5d6e7f80', 'builder_bea_2',
        'Builder, breaker, problem-solver. I like games that reward creativity.',
        'builder_bea_2@example.com', '5 Av. Anatole France, 75007 Paris, France', 'DIAMOND', 31200),

       ('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'speedy_sam_2',
        'Movement tech enjoyer. If it has dashes, slides, or grapples, I’m in.',
        'speedy_sam_2@example.com', '1 Macquarie St, Sydney NSW 2000, Australia', 'GOLD', 9000),

       ('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', 'zen_zara_2',
        'Calm comms, smart rotates, and a clean endgame.',
        'zen_zara_2@example.com', 'Praça do Comércio, 1100-148 Lisboa, Portugal', 'DIAMOND', 29000),

       ('c3d4e5f6-a7b8-4c9d-8e0f-1234567890ab', 'atlas_adept',
        'Map knowledge nerd. I ping everything and apologize for nothing.',
        'atlas_adept@example.com', 'R. Augusta, 1500 - Consolação, São Paulo - SP, Brazil', 'SILVER', 1800),

       ('d4e5f6a7-b8c9-4d0e-8f1a-2345678901bc', 'pixel_piper',
        'Indie gems, chill vibes, and a suspiciously organized inventory.',
        'pixel_piper@example.com', 'Orchard Rd, Singapore 238841, Singapore', 'GOLD', 6100),

       ('e5f6a7b8-c9d0-4e1f-8a2b-3456789012cd', 'queue_quinn',
        'I queue fill, play flex, and live for clutch revives.',
        'queue_quinn@example.com', 'Karl Johans gate, 0154 Oslo, Norway', 'PLATINUM', 12150),

       ('f6a7b8c9-d0e1-4f2a-8b3c-4567890123de', 'mossy_mako',
        'Stealth when I can, chaos when I can’t.',
        'mossy_mako@example.com', 'Queen St, Auckland 1010, New Zealand', 'BRONZE', 450),

       ('07a8b9c0-d1e2-4013-8c4d-5678901234ef', 'storm_sienna',
        'Aggressive rotations, calm comms. The vibe is “win politely.”',
        'storm_sienna@example.com', 'Nørrebrogade 3, 2200 København, Denmark', 'DIAMOND', 29800),

       ('18b9c0d1-e2f3-4124-8d5e-6789012345f0', 'tempo_tariq',
        'If it has a parry window, I will find it.',
        'tempo_tariq@example.com', 'Sheikh Mohammed bin Rashid Blvd, Dubai, UAE', 'GOLD', 7300),

       ('29c0d1e2-f3a4-4235-8e6f-7890123456a1', 'nova_nadia',
        'Support main with carry dreams. Shields up, damage out.',
        'nova_nadia@example.com', 'Paseo de la Reforma, Ciudad de México, Mexico', 'SILVER', 2500),

       ('3ad1e2f3-a4b5-4346-8f70-8901234567b2', 'bravo_basil',
        'I farm resources like it’s a second job (but fun).',
        'bravo_basil@example.com', 'Rua da Prata, 1100-420 Lisboa, Portugal', 'BRONZE', 980),

       ('4be2f3a4-b5c6-4457-8071-9012345678c3', 'glitch_greta',
        'I break games for science (and sometimes by accident).',
        'glitch_greta@example.com', 'Alexanderplatz, 10178 Berlin, Germany', 'PLATINUM', 13500),

       ('5cf3a4b5-c6d7-4568-9172-0123456789d4', 'ember_evan',
        'One more match. One more quest. One more… okay last one.',
        'ember_evan@example.com', 'Shibuya City, Tokyo 150-0002, Japan', 'GOLD', 8800),

       ('6da4b5c6-d7e8-4679-a173-1234567890e5', 'luna_lars',
        'Clean aim, cleaner comms. Minimal tilt, maximum memes.',
        'luna_lars@example.com', 'Mannerheimintie, 00100 Helsinki, Finland', 'SILVER', 3200),

       ('7eb5c6d7-e8f9-478a-b274-2345678901f6', 'cipher_cleo',
        'Puzzle brain in PvP lobbies. I overthink, then outplay.',
        'cipher_cleo@example.com', 'Rue de Rivoli, 75001 Paris, France', 'PLATINUM', 14050),

       ('8fc6d7e8-f901-489b-c375-3456789012a7', 'ranger_ryo',
        'Bow, blade, or blaster—if it’s precise, I’m into it.',
        'ranger_ryo@example.com', 'Gion, Kyoto, Japan', 'GOLD', 9600),

       ('90d7e8f9-0123-49ac-d476-4567890123b8', 'vortex_victor',
        'I like fast fights and faster queues.',
        'vortex_victor@example.com', 'Wielka 27, 00-001 Warszawa, Poland', 'BRONZE', 650),

       ('a1e8f901-2345-4abd-e577-5678901234c9', 'mellow_mina',
        'Cozy games by day, ranked grind by night.',
        'mellow_mina@example.com', 'Grafton St, Dublin, Ireland', 'SILVER', 4100),

       ('b2f90123-4567-4bce-f678-6789012345da', 'sable_sorin',
        'I play objectives like they owe me money.',
        'sable_sorin@example.com', 'Andrássy út, 1061 Budapest, Hungary', 'GOLD', 10200),

       ('c3012345-6789-4cdf-a789-7890123456eb', 'drift_daria',
        'Movement tech enthusiast. If there’s a ledge, I’m climbing it.',
        'drift_daria@example.com', 'Gran Vía, 28013 Madrid, Spain', 'PLATINUM', 15500),

       ('d4123456-7890-4def-b890-8901234567fc', 'forge_felix',
        'Crafter, theorycrafter, occasional raid dad.',
        'forge_felix@example.com', 'Storgata 1, 0184 Oslo, Norway', 'GOLD', 7700),

       ('e5234567-8901-4efa-c901-90123456780d', 'opal_olivia',
        'Calm under pressure. Loud when we win.',
        'opal_olivia@example.com', 'Champs-Élysées, 75008 Paris, France', 'DIAMOND', 32400),

       ('f6345678-9012-4fab-d012-01234567891e', 'kilo_kai',
        'I’m here for teamwork, clean plays, and ridiculous highlight reels.',
        'kilo_kai@example.com', 'Nathan Rd, Tsim Sha Tsui, Hong Kong', 'PLATINUM', 14600);


-- Player Game Preferences
INSERT INTO player_game_preferences (player_id, game_id)
VALUES
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440001'),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '550e8400-e29b-41d4-a716-446655440002'),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '550e8400-e29b-41d4-a716-446655440001'),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '550e8400-e29b-41d4-a716-446655440002'),
    ('0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', '550e8400-e29b-41d4-a716-446655440002'),
    ('2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '550e8400-e29b-41d4-a716-446655440001'),
    ('2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '550e8400-e29b-41d4-a716-446655440002'),
    ('7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', '550e8400-e29b-41d4-a716-446655440001'),
    ('a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '550e8400-e29b-41d4-a716-446655440002'),
    ('c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', '550e8400-e29b-41d4-a716-446655440001'),
    ('c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', '550e8400-e29b-41d4-a716-446655440002'),
    ('d7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', '550e8400-e29b-41d4-a716-446655440002'),
    ('e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', '550e8400-e29b-41d4-a716-446655440001'),
    ('f1a2b3c4-d5e6-4f70-8123-4567890abcde', '550e8400-e29b-41d4-a716-446655440001'),
    ('f1a2b3c4-d5e6-4f70-8123-4567890abcde', '550e8400-e29b-41d4-a716-446655440002'),
    ('13579bdf-2468-4ace-9bdf-13579bdf2468', '550e8400-e29b-41d4-a716-446655440002'),
    ('02468ace-1357-4bdf-8ace-02468ace1357', '550e8400-e29b-41d4-a716-446655440001'),
    ('02468ace-1357-4bdf-8ace-02468ace1357', '550e8400-e29b-41d4-a716-446655440002'),
    ('9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5', '550e8400-e29b-41d4-a716-446655440001'),
    ('6a1b2c3d-4e5f-6789-8abc-9def01234567', '550e8400-e29b-41d4-a716-446655440002'),
    ('3e4f5a6b-7c8d-4e9f-a012-b345c678d901', '550e8400-e29b-41d4-a716-446655440001'),
    ('4b5c6d7e-8f90-4a1b-9c2d-3e4f5a6b7c8d', '550e8400-e29b-41d4-a716-446655440002'),
    ('5c6d7e8f-9012-4b3c-8d4e-5f6a7b8c9d0e', '550e8400-e29b-41d4-a716-446655440001'),
    ('7e8f9012-3a4b-4c5d-8e6f-0123456789ab', '550e8400-e29b-41d4-a716-446655440002'),
    ('8f901234-5b6c-4d7e-8f90-1a2b3c4d5e6f', '550e8400-e29b-41d4-a716-446655440001'),
    ('90123456-7c8d-4e9f-a012-3b4c5d6e7f80', '550e8400-e29b-41d4-a716-446655440002'),
    ('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', '550e8400-e29b-41d4-a716-446655440001'),
    ('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', '550e8400-e29b-41d4-a716-446655440002'),
    ('c3d4e5f6-a7b8-4c9d-8e0f-1234567890ab', '550e8400-e29b-41d4-a716-446655440001'),
    ('d4e5f6a7-b8c9-4d0e-8f1a-2345678901bc', '550e8400-e29b-41d4-a716-446655440002');


-- Achievements
INSERT INTO achievements (id, game_id, name, description, achievement_category, achievement_rarity, criteria, triggering_event_type, trigger_condition_string, third_party_achievement)
VALUES 
       ('2c381abf-6522-4ce1-b2c2-24d40a6b8f75', '550e8400-e29b-41d4-a716-446655440001', 'First Victory','Win your first game.', 'PROGRESSION', 'COMMON', 'ONE_TIME_EVENT', 'GAME_WON', 'Win your first game.', false),
       ('8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', '550e8400-e29b-41d4-a716-446655440001', 'Getting Warm', 'Win 5 games.','PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 5 games.', false),
       ('f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', '550e8400-e29b-41d4-a716-446655440001', 'Double Digits','Win 10 games.', 'PROGRESSION', 'UNCOMMON', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 10 games.', false),
       ('b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', '550e8400-e29b-41d4-a716-446655440001', 'Unbreakable','Win 10 games in a row.', 'DIFFICULTY', 'EPIC', 'STREAK', 'GAME_WON', 'Win 10 games in a row.', false),
       ('4e96f75b-23bb-4e2f-b919-9fd7c2d0b9f0', '550e8400-e29b-41d4-a716-446655440001', 'Comeback Kid','Lose 10 games (hey, it happens).', 'PROGRESSION', 'COMMON', 'COUNTER_REACHES_THRESHOLD', 'GAMES_LOST', 'Lose 10 games.', false),
       ('efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', '550e8400-e29b-41d4-a716-446655440002', 'Marathon Session','Accumulate 1 hour of play time.', 'TIME', 'UNCOMMON', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 1 hour of play time.', false),
       ('b8938611-43e6-4b05-84e8-7ba0b2f9ee53', '550e8400-e29b-41d4-a716-446655440002', 'Weekend Warrior','Accumulate 10 hours of play time.', 'TIME', 'RARE', 'TIME_REACHED', 'TIME_PASSED', 'Accumulate 10 hours of play time.', false),
       ('2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', '550e8400-e29b-41d4-a716-446655440002', 'Social Butterfly','Play against 20 unique players.', 'SOCIAL', 'RARE', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Play against 20 unique players.', false),
       ('5fe78f61-8a2e-4c90-b4bb-14d9a1d1c36c','550e8400-e29b-41d4-a716-446655440002','Speed Runner', 'Win a game under 2 minutes.', 'TIME', 'EPIC', 'TIME_REACHED', 'GAME_WON', 'Win a game under 2 minutes.', false),
       ('d1bce2e4-f96c-4b9d-a0a9-0d10e1a2c28a', '550e8400-e29b-41d4-a716-446655440002', 'Legend of the Arena','Achieve an elite milestone reserved for top players.', 'DIFFICULTY', 'LEGENDARY', 'COUNTER_REACHES_THRESHOLD', 'GAME_WON', 'Win 100 games.', false);

-- Player Achievements
INSERT INTO user_achievements (id, user_id, achievement_id, unlocked_at)
VALUES
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '18 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', 'b8938611-43e6-4b05-84e8-7ba0b2f9ee53', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', 'f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', CURRENT_TIMESTAMP - INTERVAL '26 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', '2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', CURRENT_TIMESTAMP - INTERVAL '12 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '5fe78f61-8a2e-4c90-b4bb-14d9a1d1c36c', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (gen_random_uuid(), '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', '4e96f75b-23bb-4e2f-b919-9fd7c2d0b9f0', CURRENT_TIMESTAMP - INTERVAL '24 days'),
    (gen_random_uuid(), '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', CURRENT_TIMESTAMP - INTERVAL '22 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'd1bce2e4-f96c-4b9d-a0a9-0d10e1a2c28a', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (gen_random_uuid(), 'c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '21 days'),
    (gen_random_uuid(), 'c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', '2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', CURRENT_TIMESTAMP - INTERVAL '9 days'),
    (gen_random_uuid(), 'd7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (gen_random_uuid(), 'd7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', 'b8938611-43e6-4b05-84e8-7ba0b2f9ee53', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (gen_random_uuid(), 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 'f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    (gen_random_uuid(), 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', '5fe78f61-8a2e-4c90-b4bb-14d9a1d1c36c', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'b10e6c47-41ae-4fd5-9c46-062c9f1ef7b0', CURRENT_TIMESTAMP - INTERVAL '17 days'),
    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'd1bce2e4-f96c-4b9d-a0a9-0d10e1a2c28a', CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (gen_random_uuid(), '13579bdf-2468-4ace-9bdf-13579bdf2468', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '16 days'),
    (gen_random_uuid(), '13579bdf-2468-4ace-9bdf-13579bdf2468', 'efc2b3c6-1b93-4c3f-81cb-40a01a99c6c1', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', 'f8a5f7a7-2f49-47a6-a55e-53c1436b0d57', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (gen_random_uuid(), '9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5', '2c381abf-6522-4ce1-b2c2-24d40a6b8f75', CURRENT_TIMESTAMP - INTERVAL '14 days'),
    (gen_random_uuid(), '9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5', 'b8938611-43e6-4b05-84e8-7ba0b2f9ee53', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (gen_random_uuid(), '6a1b2c3d-4e5f-6789-8abc-9def01234567', '4e96f75b-23bb-4e2f-b919-9fd7c2d0b9f0', CURRENT_TIMESTAMP - INTERVAL '13 days'),
    (gen_random_uuid(), '6a1b2c3d-4e5f-6789-8abc-9def01234567', '5fe78f61-8a2e-4c90-b4bb-14d9a1d1c36c', CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (gen_random_uuid(), '3e4f5a6b-7c8d-4e9f-a012-b345c678d901', '8ab7d9c3-6b1b-4f48-9f27-ea2aafdb5b57', CURRENT_TIMESTAMP - INTERVAL '12 days'),
    (gen_random_uuid(), '3e4f5a6b-7c8d-4e9f-a012-b345c678d901', '2f03a6a2-11e2-46c6-88a3-96d1c4c9d250', CURRENT_TIMESTAMP - INTERVAL '1 hours');

-- Friendships
INSERT INTO friendships (id, requester_id, addressee_id, status, created_at, updated_at)
VALUES
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '40 days', CURRENT_TIMESTAMP - INTERVAL '35 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '29 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '24 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'PENDING',  CURRENT_TIMESTAMP - INTERVAL '6 days',  CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '22 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', 'c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', 'PENDING',  CURRENT_TIMESTAMP - INTERVAL '5 days',  CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', 'd7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', 'REJECTED', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '17 days'),
    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP - INTERVAL '15 days'),
    (gen_random_uuid(), '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'PENDING',  CURRENT_TIMESTAMP - INTERVAL '3 days',  CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '13 days'),
    (gen_random_uuid(), 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '12 days', CURRENT_TIMESTAMP - INTERVAL '11 days'),
    (gen_random_uuid(), '13579bdf-2468-4ace-9bdf-13579bdf2468', '02468ace-1357-4bdf-8ace-02468ace1357', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '19 days'),
    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', 'c9b7ab66-9b53-4c2d-9d6a-11a9f617fdf3', 'PENDING',  CURRENT_TIMESTAMP - INTERVAL '2 days',  CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (gen_random_uuid(), '9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5', '6a1b2c3d-4e5f-6789-8abc-9def01234567', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP - INTERVAL '16 days'),
    (gen_random_uuid(), '6a1b2c3d-4e5f-6789-8abc-9def01234567', '3e4f5a6b-7c8d-4e9f-a012-b345c678d901', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP - INTERVAL '14 days'),
    (gen_random_uuid(), '3e4f5a6b-7c8d-4e9f-a012-b345c678d901', '4b5c6d7e-8f90-4a1b-9c2d-3e4f5a6b7c8d', 'PENDING',  CURRENT_TIMESTAMP - INTERVAL '4 days',  CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (gen_random_uuid(), '5c6d7e8f-9012-4b3c-8d4e-5f6a7b8c9d0e', '7e8f9012-3a4b-4c5d-8e6f-0123456789ab', 'REJECTED', CURRENT_TIMESTAMP - INTERVAL '11 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (gen_random_uuid(), '7e8f9012-3a4b-4c5d-8e6f-0123456789ab', '8f901234-5b6c-4d7e-8f90-1a2b3c4d5e6f', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '9 days'),
    (gen_random_uuid(), '8f901234-5b6c-4d7e-8f90-1a2b3c4d5e6f', '90123456-7c8d-4e9f-a012-3b4c5d6e7f80', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (gen_random_uuid(), '90123456-7c8d-4e9f-a012-3b4c5d6e7f80', 'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e', 'BLOCKED',  CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (gen_random_uuid(), 'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d', 'c3d4e5f6-a7b8-4c9d-8e0f-1234567890ab', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '13 days'),
    (gen_random_uuid(), 'c3d4e5f6-a7b8-4c9d-8e0f-1234567890ab', 'd4e5f6a7-b8c9-4d0e-8f1a-2345678901bc', 'PENDING',  CURRENT_TIMESTAMP - INTERVAL '1 days',  CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (gen_random_uuid(), 'd4e5f6a7-b8c9-4d0e-8f1a-2345678901bc', 'e5f6a7b8-c9d0-4e1f-8a2b-3456789012cd', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (gen_random_uuid(), 'e5f6a7b8-c9d0-4e1f-8a2b-3456789012cd', 'f6a7b8c9-d0e1-4f2a-8b3c-4567890123de', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (gen_random_uuid(), 'f6a7b8c9-d0e1-4f2a-8b3c-4567890123de', '07a8b9c0-d1e2-4013-8c4d-5678901234ef', 'PENDING',  CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (gen_random_uuid(), '07a8b9c0-d1e2-4013-8c4d-5678901234ef', '18b9c0d1-e2f3-4124-8d5e-6789012345f0', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '9 days'),
    (gen_random_uuid(), '18b9c0d1-e2f3-4124-8d5e-6789012345f0', '29c0d1e2-f3a4-4235-8e6f-7890123456a1', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (gen_random_uuid(), '29c0d1e2-f3a4-4235-8e6f-7890123456a1', '3ad1e2f3-a4b5-4346-8f70-8901234567b2', 'REJECTED', CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (gen_random_uuid(), '3ad1e2f3-a4b5-4346-8f70-8901234567b2', '4be2f3a4-b5c6-4457-8071-9012345678c3', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (gen_random_uuid(), '4be2f3a4-b5c6-4457-8071-9012345678c3', '5cf3a4b5-c6d7-4568-9172-0123456789d4', 'PENDING', CURRENT_TIMESTAMP - INTERVAL '1 days', CURRENT_TIMESTAMP - INTERVAL '1 days'),
    (gen_random_uuid(), '5cf3a4b5-c6d7-4568-9172-0123456789d4', '6da4b5c6-d7e8-4679-a173-1234567890e5', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (gen_random_uuid(), '6da4b5c6-d7e8-4679-a173-1234567890e5', '7eb5c6d7-e8f9-478a-b274-2345678901f6', 'ACCEPTED', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '4 days');

-- Chat Messages
INSERT INTO messages (id, sender_id, receiver_id, content, status, sent_at, read_at)
VALUES
    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1',
     'Hey! Quick question — are we testing the matchmaking flow today?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '5 days 03:12:00', CURRENT_TIMESTAMP - INTERVAL '5 days 03:10:00'),

    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2',
     'Yep — start with game 550e...400001. I’ll watch the logs.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '5 days 03:09:00', CURRENT_TIMESTAMP - INTERVAL '5 days 03:08:20'),

    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df1',
     'Nice. Also, the leaderboard endpoint is returning 200 but empty.', 'DELIVERED',
     CURRENT_TIMESTAMP - INTERVAL '5 days 03:07:10', NULL),

    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3',
     'GGs yesterday. That last comeback was wild.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '4 days 21:40:00', CURRENT_TIMESTAMP - INTERVAL '4 days 21:35:00'),

    (gen_random_uuid(), '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df3', '0b6d9d5a-5d12-4a6f-8c40-9b79e7bb2df2',
     'LOL thanks. I was one hit from throwing. Queue again tonight?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '4 days 21:34:30', CURRENT_TIMESTAMP - INTERVAL '4 days 21:33:00'),

    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4',
     'You always rotate early or was that a one-off?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '3 days 19:05:00', CURRENT_TIMESTAMP - INTERVAL '3 days 19:01:00'),

    (gen_random_uuid(), 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4', '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2',
     'Always. Early info wins games. Want to duo sometime?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '3 days 19:00:40', CURRENT_TIMESTAMP - INTERVAL '3 days 18:58:10'),

    (gen_random_uuid(), '2c8a7d11-4d5d-4e8f-9c79-2e7e63c3f0a2', 'a3c2f771-0b7a-4a4f-bf1c-1c61c4d7e0d4',
     'Sure. Just don’t judge my aim on the first warmup match 😅', 'DELIVERED',
     CURRENT_TIMESTAMP - INTERVAL '3 days 18:57:00', NULL),

    (gen_random_uuid(), '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1', 'd7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c',
     'Found a weird build that melts bosses. Wanna test it?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '6 days 10:30:00', CURRENT_TIMESTAMP - INTERVAL '6 days 10:20:00'),

    (gen_random_uuid(), 'd7f6b94a-9d5b-49b1-86b3-2d2e1a3c4b5c', '7f2c3f9a-1e3c-4c59-9d61-3b2a5c7d88a1',
     'Always. If it’s cursed, I’m even more in.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '6 days 10:18:00', CURRENT_TIMESTAMP - INTERVAL '6 days 10:15:00'),

    (gen_random_uuid(), 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10', 'f1a2b3c4-d5e6-4f70-8123-4567890abcde',
     'That trap setup was nasty. I clipped it 😄', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '2 days 14:02:00', CURRENT_TIMESTAMP - INTERVAL '2 days 13:59:00'),

    (gen_random_uuid(), 'f1a2b3c4-d5e6-4f70-8123-4567890abcde', 'e2b1c0d9-3f8d-4f35-bc4c-3f6b7a8d9c10',
     'Send it! Also I have a better variant for tight corridors.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '2 days 13:58:00', CURRENT_TIMESTAMP - INTERVAL '2 days 13:55:00'),

    (gen_random_uuid(), '13579bdf-2468-4ace-9bdf-13579bdf2468', '02468ace-1357-4bdf-8ace-02468ace1357',
     'You play so calm it’s contagious. Teach me your secrets?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '1 days 22:20:00', CURRENT_TIMESTAMP - INTERVAL '1 days 22:10:00'),

    (gen_random_uuid(), '02468ace-1357-4bdf-8ace-02468ace1357', '13579bdf-2468-4ace-9bdf-13579bdf2468',
     'Breathe, call timings, don’t ego peek. Also… tea.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '1 days 22:08:00', CURRENT_TIMESTAMP - INTERVAL '1 days 22:06:00'),

    (gen_random_uuid(), '9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5', '6a1b2c3d-4e5f-6789-8abc-9def01234567',
     'Your name is “no_rules” but you always follow the plan. Respect.', 'DELIVERED',
     CURRENT_TIMESTAMP - INTERVAL '7 days 18:00:00', NULL),

    (gen_random_uuid(), '6a1b2c3d-4e5f-6789-8abc-9def01234567', '9d2f7c1a-3a4b-4d6e-8f10-11a1b2c3d4e5',
     'The trick is breaking the *right* rules.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '7 days 17:55:00', CURRENT_TIMESTAMP - INTERVAL '7 days 17:52:00'),

    (gen_random_uuid(), '4b5c6d7e-8f90-4a1b-9c2d-3e4f5a6b7c8d', '5c6d7e8f-9012-4b3c-8d4e-5f6a7b8c9d0e',
     'You’re actually tiltproof? Drop the settings. 😂', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '9 days 11:00:00', CURRENT_TIMESTAMP - INTERVAL '9 days 10:58:00'),

    (gen_random_uuid(), '5c6d7e8f-9012-4b3c-8d4e-5f6a7b8c9d0e', '4b5c6d7e-8f90-4a1b-9c2d-3e4f5a6b7c8d',
     'I’m tilt-resistant, not tilt-immune. But yeah: lower sens, higher patience.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '9 days 10:57:00', CURRENT_TIMESTAMP - INTERVAL '9 days 10:55:30'),

    (gen_random_uuid(), '7e8f9012-3a4b-4c5d-8e6f-0123456789ab', '8f901234-5b6c-4d7e-8f90-1a2b3c4d5e6f',
     'You collect clips, I collect achievements. Perfect duo?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '4 days 12:00:00', CURRENT_TIMESTAMP - INTERVAL '4 days 11:59:00'),

    (gen_random_uuid(), '8f901234-5b6c-4d7e-8f90-1a2b3c4d5e6f', '7e8f9012-3a4b-4c5d-8e6f-0123456789ab',
     'Only if you promise not to make me grind “Lose 10 games” 😭', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '4 days 11:58:40', CURRENT_TIMESTAMP - INTERVAL '4 days 11:57:00'),

    (gen_random_uuid(), '90123456-7c8d-4e9f-a012-3b4c5d6e7f80', 'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e',
     'I made a new layout for the arena. Want to review?', 'DELIVERED',
     CURRENT_TIMESTAMP - INTERVAL '2 days 09:15:00', NULL),

    (gen_random_uuid(), 'c3d4e5f6-a7b8-4c9d-8e0f-1234567890ab', 'd4e5f6a7-b8c9-4d0e-8f1a-2345678901bc',
     'Pinged 12 spots in one round. I might have a problem.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '3 days 08:00:00', CURRENT_TIMESTAMP - INTERVAL '3 days 07:58:00'),

    (gen_random_uuid(), 'd4e5f6a7-b8c9-4d0e-8f1a-2345678901bc', 'c3d4e5f6-a7b8-4c9d-8e0f-1234567890ab',
     'Not a problem — it’s a lifestyle. Also I organized my inventory by color again.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '3 days 07:57:00', CURRENT_TIMESTAMP - INTERVAL '3 days 07:55:00'),

    (gen_random_uuid(), 'e5f6a7b8-c9d0-4e1f-8a2b-3456789012cd', 'f6a7b8c9-d0e1-4f2a-8b3c-4567890123de',
     'If you stealth in first, I’ll follow with shields. Deal?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '6 days 20:00:00', CURRENT_TIMESTAMP - INTERVAL '6 days 19:58:00'),

    (gen_random_uuid(), 'f6a7b8c9-d0e1-4f2a-8b3c-4567890123de', 'e5f6a7b8-c9d0-4e1f-8a2b-3456789012cd',
     'Deal. If things go loud, we pretend it was the plan.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '6 days 19:57:00', CURRENT_TIMESTAMP - INTERVAL '6 days 19:56:00'),

    (gen_random_uuid(), '07a8b9c0-d1e2-4013-8c4d-5678901234ef', '18b9c0d1-e2f3-4124-8d5e-6789012345f0',
     'You actually hit that parry? I heard it from across the map.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '1 days 18:40:00', CURRENT_TIMESTAMP - INTERVAL '1 days 18:35:00'),

    (gen_random_uuid(), '18b9c0d1-e2f3-4124-8d5e-6789012345f0', '07a8b9c0-d1e2-4013-8c4d-5678901234ef',
     'Window was tiny but my ego was bigger.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '1 days 18:34:00', CURRENT_TIMESTAMP - INTERVAL '1 days 18:33:00'),

    (gen_random_uuid(), '29c0d1e2-f3a4-4235-8e6f-7890123456a1', '3ad1e2f3-a4b5-4346-8f70-8901234567b2',
     'You farm like a machine. What route are you using?', 'DELIVERED',
     CURRENT_TIMESTAMP - INTERVAL '8 days 09:00:00', NULL),

    (gen_random_uuid(), '3ad1e2f3-a4b5-4346-8f70-8901234567b2', '29c0d1e2-f3a4-4235-8e6f-7890123456a1',
     'Top-left sweep, then rotate mid. It’s boring but it works.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '8 days 08:55:00', CURRENT_TIMESTAMP - INTERVAL '8 days 08:50:00'),

    (gen_random_uuid(), '4be2f3a4-b5c6-4457-8071-9012345678c3', '5cf3a4b5-c6d7-4568-9172-0123456789d4',
     'Found a bug where sprint cancels reload. Accidentally amazing.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '2 days 23:00:00', CURRENT_TIMESTAMP - INTERVAL '2 days 22:55:00'),

    (gen_random_uuid(), '5cf3a4b5-c6d7-4568-9172-0123456789d4', '4be2f3a4-b5c6-4457-8071-9012345678c3',
     'Please report it before it becomes “meta” and ruins my life 😂', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '2 days 22:54:00', CURRENT_TIMESTAMP - INTERVAL '2 days 22:52:00'),

    (gen_random_uuid(), '6da4b5c6-d7e8-4679-a173-1234567890e5', '7eb5c6d7-e8f9-478a-b274-2345678901f6',
     'You overthink and still win. Teach me that power.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '12 days 16:10:00', CURRENT_TIMESTAMP - INTERVAL '12 days 16:05:00'),

    (gen_random_uuid(), '7eb5c6d7-e8f9-478a-b274-2345678901f6', '6da4b5c6-d7e8-4679-a173-1234567890e5',
     'Step 1: panic. Step 2: pattern-match. Step 3: pretend it was planned.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '12 days 16:04:00', CURRENT_TIMESTAMP - INTERVAL '12 days 16:02:00'),

    (gen_random_uuid(), '8fc6d7e8-f901-489b-c375-3456789012a7', '90d7e8f9-0123-49ac-d476-4567890123b8',
     'Fast fights, faster queues — you’d love a bow-only run.', 'SENT',
     CURRENT_TIMESTAMP - INTERVAL '0 days 02:30:00', NULL),

    (gen_random_uuid(), 'a1e8f901-2345-4abd-e577-5678901234c9', 'b2f90123-4567-4bce-f678-6789012345da',
     'Objective players are rare. Want to squad later?', 'DELIVERED',
     CURRENT_TIMESTAMP - INTERVAL '0 days 05:45:00', NULL),

    (gen_random_uuid(), 'c3012345-6789-4cdf-a789-7890123456eb', 'd4123456-7890-4def-b890-8901234567fc',
     'If there’s a ledge I’m climbing it. You crafting the grapples?', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '3 days 17:00:00', CURRENT_TIMESTAMP - INTERVAL '3 days 16:58:00'),

    (gen_random_uuid(), 'd4123456-7890-4def-b890-8901234567fc', 'c3012345-6789-4cdf-a789-7890123456eb',
     'Already done. Also made extras for when you inevitably fall.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '3 days 16:57:00', CURRENT_TIMESTAMP - INTERVAL '3 days 16:55:00'),

    (gen_random_uuid(), 'e5234567-8901-4efa-c901-90123456780d', 'f6345678-9012-4fab-d012-01234567891e',
     'Clean plays only today. No clowning.', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '4 days 09:30:00', CURRENT_TIMESTAMP - INTERVAL '4 days 09:28:00'),

    (gen_random_uuid(), 'f6345678-9012-4fab-d012-01234567891e', 'e5234567-8901-4efa-c901-90123456780d',
     'Agreed. (I will still clip the ridiculous moments though.)', 'READ',
     CURRENT_TIMESTAMP - INTERVAL '4 days 09:27:00', CURRENT_TIMESTAMP - INTERVAL '4 days 09:26:00');
