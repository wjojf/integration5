import type { Game, UserRank } from './app.types'

export interface GamesResponse {
  games: Game[]
}

export type PlayerSearchParams = {
  username: string
  rank: UserRank
  page: number
  size: number
};

export interface UpdatePlayerRequest {
  username?: string;
  bio?: string;
  address?: string;
  gamePreferences?: string[];
}

export interface SendFriendRequestPayload {
  addresseeId: string;
}

export interface ModifyFriendRequest{
  id: string,
  data: ModifyFriendRequestPayload
}

export interface ModifyFriendRequestPayload {
  action: 'ACCEPT' | 'REJECT' | 'BLOCK' | 'CANCEL' | 'REMOVE' | 'UNBLOCK';
}

export interface CreateLobbyRequest {
  name: string;
  description?: string | null;
  maxPlayers: number;
  private: boolean;
}

export interface UpdateLobbyRequest {
  name: string;
  description?: string | null;
}

export interface StartLobbyRequest {
  gameId: string;
}

export interface InviteToLobbyRequest {
  invitedPlayerId: string;
}

export interface LobbySearchParams {
  gameId?: string;
  username?: string;
  page?: number;
  size?: number;
}

export interface SendMessageRequest {
  receiverId: string;
  content: string;
}

export interface ConversationParams {
  page?: number;
  size?: number;
}

export interface ChatbotMessageResponse {
  response: string
  conversation_id: string
  sources: string[]
  cached: boolean
}

export interface ChatRequest {
  message: string
  conversation_id?: string | null
  user_id: string
}

export type WebSocketMessageNotification = {
  messageId: string;
  senderId: string;
  receiverId: string;
  content: string;
  sentAt: string;
  status?: string;
}

export interface PageableResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort?: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  size: number;
  number: number;
  sort?: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  empty: boolean;
}
