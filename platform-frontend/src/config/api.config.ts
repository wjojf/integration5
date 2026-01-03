export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  WS_URL: import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws',
  KEYCLOAK: {
    URL: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
    REALM: 'banditgames',
    CLIENT_ID: 'platform-client',
    TOKEN_ENDPOINT: '/realms/banditgames/protocol/openid-connect/token',
  },
  TIMEOUT: 30000,
  PAGINATION: {
    DEFAULT_PAGE: 0,
    DEFAULT_SIZE: 20,
    MAX_SIZE: 50,
  },
  WEBSOCKET: {
    RECONNECT_DELAY: 5000,
    HEARTBEAT_INCOMING: 4000,
    HEARTBEAT_OUTGOING: 4000,
  },
} as const;

export const API_ENDPOINTS = {
  ACHIEVEMENTS: {
    ALL: '/api/platform/achievements',
    BY_USER__ID: (userId: string) => `/api/platform/achievements/users/${userId}`,
  },
  PLAYER: {
    PROFILE: '/api/platform/players',
    FRIENDS: '/api/platform/players/friends',
    EXP: '/api/platform/players/exp',
    BY_ID: (playerId: string) => `/api/platform/players/${playerId}`,
    SEARCH: '/api/platform/players/search',
  },
  GAME: {
    ALL: '/api/v1/games/games',
    SESSION: (sessionId: string) => `/api/v1/games/sessions/${sessionId}`,
    CREATE_SESSION: '/api/v1/games/sessions',
    APPLY_MOVE: (sessionId: string) => `/api/v1/games/sessions/${sessionId}/moves`,
    ABANDON_SESSION: (sessionId: string) => `/api/v1/games/sessions/${sessionId}/abandon`,
  },
  AI_PLAYER: {
    MOVE: '/api/v1/ai_player/move',
    LEVELS: '/api/v1/ai_player/levels',
    ADJUST_DIFFICULTY: '/api/v1/ai_player/adjust-difficulty',
  },
  FRIENDS: {
    ALL: '/api/platform/friends',
    BY_ID: (friendshipId: string) => `/api/platform/friends/requests/${friendshipId}`,
    SEND_REQUEST: '/api/platform/friends/requests',
  },
  LOBBY: {
    ALL: '/api/platform/lobbies',
    CURRENT_PLAYER: '/api/platform/lobbies/current',
    BY_ID: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}`,
    JOIN: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}/join`,
    LEAVE: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}/leave`,
    START: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}/start`,
    READY: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}/ready`,
    INVITE: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}/invite`,
    UPDATE: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}`,
    CANCEL: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}`,
    SEARCH: '/api/platform/lobbies/search',
    EXTERNAL_GAME_INSTANCE: (lobbyId: string) => `/api/platform/lobbies/${lobbyId}/external-game-instance`,
  },
  CHAT: {
    CONVERSATIONS: '/api/platform/chat/conversations',
    CONVERSATION: (userId: string) => `/api/platform/chat/conversations/${userId}`,
    MESSAGES: '/api/platform/chat/messages',
    UNREAD_COUNT: '/api/platform/chat/messages/unread/count',
    MARK_READ: (messageId: string) => `/api/platform/chat/messages/${messageId}/read`,
  },
  CHATBOT: {
    BY_ID: (conversationId: string) => `/api/v1/chatbot/conversation/${conversationId}`,
    CREATE_CHAT: `/api/v1/chatbot/chat`
  },
  CHESS: {
    REGISTER: '/api/external/chess/register',
    GET_GAME: (gameId: string) => `/api/external/chess/games/${gameId}`,
    CREATE_GAME: (gameId: string) => `/api/external/chess/games/${gameId}`,
    UPDATE_GAME: (gameId: string) => `/api/external/chess/games/${gameId}`,
    MAKE_MOVE: (gameId: string) => `/api/external/chess/moves/${gameId}`,
    END_MATE: (gameId: string) => `/api/external/chess/games/${gameId}/mate`,
    END_DRAW: (gameId: string) => `/api/external/chess/games/${gameId}/draw`,
  },
  WEBSOCKET: {
    MESSAGES: '/user/queue/messages',
  },
} as const;

export const QUERY_KEYS = {
  PLAYER: {
    PROFILE: ['player', 'profile'] as const,
    SEARCH: ['player', 'search'] as const,
    BY_ID: (id: string) => ['player', 'by-id', id] as const,
  },
  GAME: {
    ALL: ['games', 'all'] as const
  },
  ACHIEVEMENTS: {
    ALL: ['achievements', 'all'] as const,
    BY_USER_ID: (userId: string) => ['achievements', 'user', userId] as const
  },
  FRIENDSHIPS: {
    ALL: ['friendships', 'all'] as const,
    PENDING: ['friendships', 'pending'] as const,
    ACCEPTED: ['friendships', 'accepted'] as const,
  },
  FRIENDS: {
    ALL: ['friends', 'all'] as const,
  },
  LOBBY: {
    ALL: ['lobby', 'all'] as const,
    CURRENT_PLAYER: ['lobby', 'current'] as const,
    BY_ID: (id: string) => ['lobby', id] as const,
  },
  CHATBOT: {
    HISTORY: (conversationId: string) => ["chatbot", "history", conversationId] as const,
  },
  CHAT: {
    CONVERSATIONS: ['chat', 'conversations'] as const,
    CONVERSATION: (userId: string) => ['chat', 'conversation', userId] as const,
    UNREAD_COUNT: ['chat', 'unread'] as const,
  },
} as const;

