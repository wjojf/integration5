import { ApiService } from './api.service'
import { API_ENDPOINTS } from '../config/api.config'
import type { GamesResponse } from '../types/api.types'

const { GAME } = API_ENDPOINTS

export interface GameSession {
  session_id: string;
  game_id: string;
  game_type: string;
  status: string;
  current_player_id: string;
  player_ids: string[];
  game_state: any;
  total_moves: number;
  winner_id?: string | null;
}

export interface ApplyMoveRequest {
  player_id: string;
  move: any;
}

export interface ApplyMoveResponse {
  session_id: string;
  game_state: any;
  status: string;
  current_player_id: string;
  winner_id?: string | null;
  total_moves: number;
}

export interface CreateSessionRequest {
  session_id?: string;
  game_id: string;
  game_type: string;
  lobby_id?: string | null;
  player_ids: string[];
  starting_player_id: string;
  configuration?: any;
}

class GameService extends ApiService {
    async getAllGames() {
        return this.get<GamesResponse>(GAME.ALL)
    }

    async getSession(sessionId: string) {
        return this.get<GameSession>(GAME.SESSION(sessionId))
    }

    async createSession(data: CreateSessionRequest) {
        return this.post<GameSession>(GAME.CREATE_SESSION, data)
    }

    async applyMove(sessionId: string, data: ApplyMoveRequest) {
        return this.post<ApplyMoveResponse>(GAME.APPLY_MOVE(sessionId), data)
    }

    async abandonSession(sessionId: string, playerId: string) {
        return this.post<{ session_id: string; status: string; winner_id?: string | null; message: string }>(
            GAME.ABANDON_SESSION(sessionId),
            { player_id: playerId }
        )
    }
}

export const gameService = new GameService();
