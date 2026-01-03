-- Database Schema for Game Service
-- PostgreSQL schema for ML-focused game logging
-- Optimized for Connect Four training data with expert labels

-- Create database (run this manually if needed)
-- CREATE DATABASE banditgames;

-- Session Logs Table
-- Stores game session metadata for ML experiments
CREATE TABLE IF NOT EXISTS session_logs (
    session_id VARCHAR(64) PRIMARY KEY,
    game_type VARCHAR(32) NOT NULL DEFAULT 'connect_four',

    -- Player Configuration
    p1_type VARCHAR(16) NOT NULL,  -- 'human' | 'ai'
    p2_type VARCHAR(16) NOT NULL,  -- 'human' | 'ai'
    p1_agent VARCHAR(64),  -- 'mcts_100', 'mcts_500', 'minimax', etc.
    p2_agent VARCHAR(64),
    p1_agent_level VARCHAR(16),  -- 'low', 'medium', 'high'
    p2_agent_level VARCHAR(16),

    -- Match Results
    winner VARCHAR(8),  -- 'p1', 'p2', 'draw'
    num_moves INTEGER NOT NULL DEFAULT 0,
    duration_seconds FLOAT,

    -- Timestamps
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,

    -- Dataset Management
    tag VARCHAR(128)  -- 'selfplay_v1', 'dataset_v2', 'human_vs_ai', etc.
);

-- Indexes for session_logs
CREATE INDEX IF NOT EXISTS idx_session_logs_game_type ON session_logs(game_type);
CREATE INDEX IF NOT EXISTS idx_session_logs_tag ON session_logs(tag);
CREATE INDEX IF NOT EXISTS idx_session_logs_start_time ON session_logs(start_time);
CREATE INDEX IF NOT EXISTS idx_session_logs_winner ON session_logs(winner);
CREATE INDEX IF NOT EXISTS idx_session_logs_p1_agent ON session_logs(p1_agent);
CREATE INDEX IF NOT EXISTS idx_session_logs_p2_agent ON session_logs(p2_agent);

-- Game Moves Table
-- Stores individual moves with ML training labels
CREATE TABLE IF NOT EXISTS game_moves (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES session_logs(session_id) ON DELETE CASCADE,
    move_number INTEGER NOT NULL,

    -- Player Info
    current_player VARCHAR(8) NOT NULL,  -- 'p1' | 'p2'
    current_player_id INTEGER NOT NULL,  -- 1 | 2 (player 1 or player 2)

    -- Board State
    board_state JSONB NOT NULL,  -- Raw board as JSON [[0,0,...], ...]
    board_flat INTEGER[] NOT NULL,  -- Flattened board [42 elements] for ML input

    -- Legal Moves
    legal_moves_mask INTEGER[] NOT NULL,  -- Binary mask [7 elements] (1=legal, 0=illegal)

    -- Move Played
    played_move INTEGER NOT NULL,  -- Column index 0-6

    -- Expert/MCTS Labels (CRITICAL FOR ML TRAINING)
    expert_policy FLOAT[] NOT NULL,  -- Probability distribution [7 elements] from strong MCTS
    expert_best_move INTEGER NOT NULL,  -- argmax(expert_policy), target for policy network
    expert_value FLOAT NOT NULL,  -- Win probability [0-1], target for value network

    -- Game Result
    final_result VARCHAR(16),  -- 'win', 'loss', 'draw', 'ongoing'
    final_result_numeric FLOAT,  -- 1.0 (win) / 0.0 (loss) / 0.5 (draw)

    -- Metadata
    mcts_iterations INTEGER,  -- Number of MCTS iterations used
    game_status VARCHAR(16),  -- 'ongoing', 'finished'
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for game_moves
CREATE INDEX IF NOT EXISTS idx_game_moves_session_id ON game_moves(session_id);
CREATE INDEX IF NOT EXISTS idx_game_moves_move_number ON game_moves(move_number);
CREATE INDEX IF NOT EXISTS idx_game_moves_timestamp ON game_moves(timestamp);
CREATE INDEX IF NOT EXISTS idx_game_moves_current_player_id ON game_moves(current_player_id);
CREATE INDEX IF NOT EXISTS idx_game_moves_game_status ON game_moves(game_status);

-- Comments for documentation
COMMENT ON TABLE session_logs IS 'ML-focused game session metadata for training experiments';
COMMENT ON TABLE game_moves IS 'Individual moves with expert labels for supervised learning';

COMMENT ON COLUMN session_logs.p1_agent IS 'Agent configuration for Player 1 (e.g., mcts_100, mcts_500)';
COMMENT ON COLUMN session_logs.p2_agent IS 'Agent configuration for Player 2';
COMMENT ON COLUMN session_logs.tag IS 'Experiment tag for dataset organization (e.g., selfplay_v1)';

COMMENT ON COLUMN game_moves.current_player_id IS 'Normalized player ID: 1 (current) or -1 (opponent) for perspective normalization';
COMMENT ON COLUMN game_moves.board_flat IS 'Flattened board [42 elements] with values {0, 1, -1} as ML input';
COMMENT ON COLUMN game_moves.legal_moves_mask IS 'Binary mask [7 elements] indicating legal columns';
COMMENT ON COLUMN game_moves.expert_policy IS 'MCTS probability distribution [7 elements] as soft label for policy network';
COMMENT ON COLUMN game_moves.expert_best_move IS 'Best move column (0-6) from argmax(expert_policy) as hard label';
COMMENT ON COLUMN game_moves.expert_value IS 'Win probability [0-1] from MCTS as target for value network';
