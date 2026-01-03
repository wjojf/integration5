import { ApiService } from './api.service';
import { API_ENDPOINTS } from '../config/api.config';
import type { PageableResponse, UpdatePlayerRequest, PlayerSearchParams } from '../types/api.types';
import type { Player } from '../types/app.types';

const { PLAYER } = API_ENDPOINTS

class PlayerService extends ApiService {

  async getProfile(): Promise<Player> {
    return this.get<Player>(PLAYER.PROFILE);
  }

  async updateProfile(data: UpdatePlayerRequest): Promise<Player> {
    return this.patch<Player>(PLAYER.PROFILE, data);
  }

  async searchPlayers(params: PlayerSearchParams): Promise<PageableResponse<Player>> {
    return this.get<PageableResponse<Player>>(PLAYER.SEARCH, params);
  }

  async getPlayerFriends(): Promise<Player[]> {
    return this.get<Player[]>(PLAYER.FRIENDS);
  }

  async getPlayerById(playerId: string): Promise<Player> {
    return this.get<Player>(PLAYER.BY_ID(playerId));
  }
}

export const playerService = new PlayerService();
