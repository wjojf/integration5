import { ApiService } from './api.service';
import { API_ENDPOINTS } from '../config/api.config';
import type {
  CreateLobbyRequest, UpdateLobbyRequest, StartLobbyRequest, InviteToLobbyRequest, LobbySearchParams, PageableResponse
} from '../types/api.types';
import {Lobby} from '../types/app.types';

const { LOBBY } = API_ENDPOINTS

class LobbyService extends ApiService {

  async getAll(params?: LobbySearchParams): Promise<PageableResponse<Lobby>> {
    return this.get<PageableResponse<Lobby>>(LOBBY.SEARCH, params);
  }

  async getCurrentPlayerLobby(): Promise<Lobby> {
    return this.get<Lobby>(LOBBY.CURRENT_PLAYER);
  }

  async getByLobbyId(lobbyId: string): Promise<Lobby> {
    return this.get<Lobby>(LOBBY.BY_ID(lobbyId));
  }

  async create(data: CreateLobbyRequest): Promise<Lobby> {
    return this.post<Lobby>(LOBBY.ALL, data);
  }

  async join(lobbyId: string): Promise<Lobby> {
    return this.post<Lobby>(LOBBY.JOIN(lobbyId));
  }

  async start(lobbyId: string, data: StartLobbyRequest): Promise<Lobby> {
    return this.post<Lobby>(LOBBY.START(lobbyId), data);
  }

  async ready(lobbyId: string): Promise<Lobby> {
    return this.patch<Lobby>(LOBBY.READY(lobbyId));
  }

  async invite(lobbyId: string, data: InviteToLobbyRequest): Promise<Lobby> {
    return this.patch<Lobby>(LOBBY.INVITE(lobbyId), data);
  }

  async update(lobbyId: string, data: UpdateLobbyRequest): Promise<Lobby> {
    return this.patch<Lobby>(LOBBY.UPDATE(lobbyId), data);
  }

  async leave(lobbyId: string): Promise<Lobby> {
    return this.post<Lobby>(LOBBY.LEAVE(lobbyId));
  }

  async cancel(lobbyId: string): Promise<void> {
    return this.delete<void>(LOBBY.CANCEL(lobbyId));
  }

  async getExternalGameInstance(lobbyId: string): Promise<{ lobbyId: string; gameId: string | null; externalGameInstanceId: string | null; hasExternalGameInstance: boolean }> {
    return this.get<{ lobbyId: string; gameId: string | null; externalGameInstanceId: string | null; hasExternalGameInstance: boolean }>(LOBBY.EXTERNAL_GAME_INSTANCE(lobbyId));
  }
}

export const lobbyService = new LobbyService();
