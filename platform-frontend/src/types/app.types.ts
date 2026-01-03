export type UserRank = "BRONZE" | "SILVER" | "GOLD" | "PLATINUM" | "DIAMOND"

export enum FriendshipStatus {
    PENDING = "PENDING",
    ACCEPTED = "ACCEPTED",
    REJECTED = "REJECTED",
    BLOCKED = "BLOCKED"
}

export enum LobbyStatus {
    WAITING = "WAITING",
    READY = "READY",
    IN_PROGRESS = "IN_PROGRESS",
    STARTED = "STARTED",
    COMPLETED = "COMPLETED",
    CANCELLED = "CANCELLED"
}

export interface Player {
    playerId: string;
    username: string;
    bio?: string | null;
    gamePreferences: string[];
    email: string | null;
    address?: string | null;
    rank: UserRank;
    exp: number;
}

export interface Game {
    id: number,
    title: string,
    genre: string,
    image: string
}

export interface Achievement {
    id: string,
    gameId?: string
    name: string,
    description: string,
    category: "PROGRESSION" | "TIME" | "DIFFICULTY" | "SOCIAL"
    rarity: "COMMON" | "UNCOMMON" | "RARE" | "EPIC" | "LEGENDARY"
}

export interface UserUnlockedAchievement {
    id: string,
    userId: string,
    achievementId: string,
    unlockedAt: string
}

export interface Friendship {
    id: string;
    requesterId: string;
    addresseeId: string;
    status: FriendshipStatus;
    createdAt: string;
    updatedAt: string;
}

export interface Lobby {
    id: string;
    gameId: string | null;
    sessionId: string | null;
    hostId: string;
    name: string;
    description: string | null;
    playerIds: string[];
    status: LobbyStatus;
    maxPlayers: number;
    visibility: 'PUBLIC' | 'PRIVATE';
    invitedPlayerIds: string[];
    createdAt: string;
    startedAt: string | null;
}

export type MessageStatus = "SENT" | "DELIVERED" | "READ"

export interface Message {
    id: string;
    senderId: string;
    receiverId: string;
    content: string;
    status: MessageStatus
    sentAt: string;
    readAt?: string | null;
}

export type ChatRole = "user" | "assistant"

export interface ChatMessage {
    role: ChatRole
    content: string
}

export interface ChatMessages {
    conversation_id: string
    history: ChatMessage[]
}

export interface GameState {
    gameId: string;
    board: string[][];
    currentPlayer: string;
}

export interface PolicyPrediction {
    suggestedMove: number;
    confidence: number;
    distribution: number[];
}

export interface WinProbabilityPrediction {
    winProbability: number;
    predictedWinner: string | null;
}

export interface ModelMetric {
    modelName: string;
    version: string;
    accuracy: number;
    lastUpdated: string;
    trainingGames: number;
}
