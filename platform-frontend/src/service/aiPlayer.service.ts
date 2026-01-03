import { ApiService } from './api.service';
import { API_ENDPOINTS } from '../config/api.config';

const { AI_PLAYER } = API_ENDPOINTS;

export interface AIMoveRequest {
  game_state: {
    board: number[][];
    current_player: string;
    player_ids: string[];
    status: string;
    move_number?: number;
  };
  level: 'low' | 'medium' | 'high' | 'very_high';
  game_type?: string;
}

export interface AIMoveResponse {
  move: { column: number };
  level: string;
  confidence: number;
  thinking_time_ms: number;
  iterations: number;
}

export interface LevelInfo {
  level: string;
  iterations: number;
}

export interface LevelsResponse {
  levels: LevelInfo[];
}

class AIPlayerService extends ApiService {
  async getAIMove(request: AIMoveRequest): Promise<AIMoveResponse> {
    return this.post<AIMoveResponse>(AI_PLAYER.MOVE, {
      game_state: request.game_state,
      level: request.level,
      game_type: request.game_type || 'connect_four',
    });
  }

  async getLevels(): Promise<LevelsResponse> {
    return this.get<LevelsResponse>(AI_PLAYER.LEVELS);
  }
}

export const aiPlayerService = new AIPlayerService();

