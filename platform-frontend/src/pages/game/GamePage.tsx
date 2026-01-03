import { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { toast } from "sonner";
import { Users, Crown, LogOut, ArrowLeft, Bot } from "lucide-react";
import { useKeycloak } from "@react-keycloak/web";

import { InputComponents, DisplayComponents } from '../../components/app'
import { Badge } from '../../components/shared'
import { ConnectFourGame } from '../../components/game/ConnectFourGame'
import { useLobbyById, useCurrentPlayerLobby } from "../../hooks/lobby/useLobby";
import { useGetAllGames } from "../../hooks/game/useGame";
import { usePlayerById } from "../../hooks/player/usePlayer";
import { LobbyStatus } from "../../types/app.types";
import type { GameSession } from "../../service/game.service";

const { Button } = InputComponents
const { Card, CardContent, CardHeader, CardTitle } = DisplayComponents

// Chess game ID from game service
const CHESS_GAME_ID = "550e8400-e29b-41d4-a716-446655440002"

export const GamePage = () => {
  const { lobbyId, sessionId } = useParams<{ lobbyId?: string; sessionId?: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();
  const currentUserId = keycloak.tokenParsed?.sub || '';

  // Check if this is an AI game (route: /app/game/ai/:sessionId)
  const isAIGame = location.pathname.includes('/game/ai/');
  
  const { data: lobby, isLoading: isLoadingLobby, refetch: refetchLobby } = useLobbyById(lobbyId || '');
  
  // Check if this is a chess game - if so, redirect to lobby detail page which has embedded chess UI
  const isChessGame = lobby?.gameId === CHESS_GAME_ID;
  
  // Redirect chess games to lobby detail page (which has embedded chess UI)
  useEffect(() => {
    if (!isAIGame && lobbyId && isChessGame) {
      navigate(`/app/lobbies/${lobbyId}`, { replace: true });
    }
  }, [isChessGame, lobbyId, isAIGame, navigate]);
  const { data: currentLobby } = useCurrentPlayerLobby();
  const { data: gamesResponse } = useGetAllGames();
  const games = gamesResponse?.games || [];

  // For AI games, get session directly
  const [aiSession, setAiSession] = useState<GameSession | null>(null);
  const [isLoadingAISession, setIsLoadingAISession] = useState(false);
  const [aiLevel, setAiLevel] = useState<string>('medium');
  
  // For lobby games, also get the session to get accurate player IDs
  const [lobbySession, setLobbySession] = useState<GameSession | null>(null);
  const [isLoadingLobbySession, setIsLoadingLobbySession] = useState(false);

  // Get opponent player ID (the other player in the lobby)
  const opponentId = lobby?.playerIds.find(id => id !== currentUserId);
  const { data: opponent } = usePlayerById(opponentId);
  
  // Check if opponent is AI
  const isAIOpponent = opponentId === 'ai_p2' || opponentId?.startsWith('ai_');

  // Check if current user is the host
  const isHost = lobby?.hostId === currentUserId;

  // Load AI session if this is an AI game
  useEffect(() => {
    // Clear old session when sessionId changes or is cleared
    if (!sessionId) {
      setAiSession(null);
      setIsLoadingAISession(false);
      return;
    }

    if (isAIGame && sessionId) {
      setIsLoadingAISession(true);
      // Clear old session immediately when sessionId changes
      setAiSession(null);
      
      (async () => {
        try {
          const { gameService } = await import("../../service/game.service");
          const session = await gameService.getSession(sessionId);
          // Only set session if we're still looking at the same sessionId
          // (prevents race conditions if sessionId changes while loading)
          if (sessionId === session.session_id) {
            setAiSession(session);
            // Extract AI level from configuration if available
            if (session.game_state?.configuration?.ai_level) {
              setAiLevel(session.game_state.configuration.ai_level);
            }
          }
        } catch (error) {
          console.error('Failed to load AI session:', error);
          toast.error('Failed to load game session');
        } finally {
          setIsLoadingAISession(false);
        }
      })();
    }
  }, [isAIGame, sessionId]);

  // Load lobby session to get accurate player IDs
  useEffect(() => {
    // Clear old session when sessionId changes or is cleared
    if (!lobby?.sessionId) {
      setLobbySession(null);
      setIsLoadingLobbySession(false);
      return;
    }

    const lobbySessionId = lobby?.sessionId;
    if (!isAIGame && lobbySessionId) {
      setIsLoadingLobbySession(true);
      // Clear old session immediately when sessionId changes
      setLobbySession(null);
      
      (async () => {
        try {
          const { gameService } = await import("../../service/game.service");
          const session = await gameService.getSession(lobbySessionId);
          // Only set session if we're still looking at the same sessionId
          // (prevents race conditions if sessionId changes while loading)
          if (lobbySessionId === session.session_id) {
            setLobbySession(session);
          }
        } catch (error) {
          console.error('Failed to load lobby session:', error);
        } finally {
          setIsLoadingLobbySession(false);
        }
      })();
    }
  }, [isAIGame, lobby?.sessionId]);

  // Poll for sessionId if lobby is started but sessionId is not yet available
  useEffect(() => {
    let pollInterval: NodeJS.Timeout | null = null;

    if (!isAIGame && lobby &&
      (lobby.status === 'STARTED' || lobby.status === 'IN_PROGRESS') &&
      !lobby.sessionId) {
      // Poll for sessionId every 500ms for up to 10 seconds
      let attempts = 0;
      const maxAttempts = 20;

      pollInterval = setInterval(async () => {
        attempts++;
        try {
          const result = await refetchLobby();
          if (result.data?.sessionId) {
            clearInterval(pollInterval!);
            pollInterval = null;
          } else if (attempts >= maxAttempts) {
            clearInterval(pollInterval!);
            pollInterval = null;
          }
        } catch (error) {
          console.error('Failed to poll for sessionId:', error);
        }
      }, 500);
    }

    return () => {
      if (pollInterval) {
        clearInterval(pollInterval);
      }
    };
  }, [lobby?.status, lobby?.sessionId, refetchLobby, isAIGame]);


  // Redirect if user is not in this lobby (only for lobby-based games)
  useEffect(() => {
    if (!isAIGame && lobby && currentLobby && lobby.id !== currentLobby.id) {
      toast.error("You are not in this lobby");
      navigate("/app/lobbies");
    }
  }, [lobby, currentLobby, navigate, isAIGame]);

  // Redirect if lobby is completed or cancelled
  useEffect(() => {
    if (!isAIGame && lobby && (lobby.status === 'COMPLETED' || lobby.status === 'CANCELLED')) {
      toast.info("This game has ended");
      navigate("/app/lobbies");
    }
  }, [lobby?.status, navigate, isAIGame]);

  // Redirect if session is ended (finished or abandoned)
  useEffect(() => {
    if (lobbySession && (lobbySession.status === 'finished' || lobbySession.status === 'abandoned')) {
      toast.info("Game has ended");
      navigate("/app/lobbies");
    }
    if (aiSession && (aiSession.status === 'finished' || aiSession.status === 'abandoned')) {
      toast.info("Game has ended");
      navigate("/app/games");
    }
  }, [lobbySession?.status, aiSession?.status, navigate]);

  // Redirect if no sessionId and lobby is not started (but allow some time for session to be created)
  useEffect(() => {
    if (lobby && !lobby.sessionId && lobby.status !== 'STARTED' && lobby.status !== 'IN_PROGRESS') {
      // Don't redirect immediately - give time for session to be created
      // The polling effect above will handle sessionId when it becomes available
    }
  }, [lobby, navigate]);

  const getStatusColor = (status: LobbyStatus) => {
    switch (status) {
      case "WAITING":
        return "bg-yellow-500/20 text-yellow-500";
      case "READY":
        return "bg-blue-500/20 text-blue-500";
      case "IN_PROGRESS":
      case "STARTED":
        return "bg-green-500/20 text-green-500";
      case "COMPLETED":
        return "bg-gray-500/20 text-gray-500";
      case "CANCELLED":
        return "bg-red-500/20 text-red-500";
      default:
        return "bg-gray-500/20 text-gray-500";
    }
  };

  // Determine session ID and player IDs - use session's player_ids as source of truth
  const activeSessionId = isAIGame ? sessionId : lobby?.sessionId;
  // Get player_ids from session, with fallback to game_state or lobby
  const getPlayerIds = (session: GameSession | null) => {
    if (session?.player_ids) return session.player_ids;
    // Fallback: try to get from game_state if available
    if (session?.game_state?.player_ids) return session.game_state.player_ids;
    return [];
  };
  const activePlayerIds = isAIGame 
    ? getPlayerIds(aiSession)
    : (getPlayerIds(lobbySession) || lobby?.playerIds || []);

  // Poll for session status to detect when game ends
  useEffect(() => {
    let pollInterval: NodeJS.Timeout | null = null;

    if (activeSessionId && (lobbySession || aiSession)) {
      // Poll every 2 seconds to check if game has ended
      pollInterval = setInterval(async () => {
        try {
          const { gameService } = await import("../../service/game.service");
          const session = await gameService.getSession(activeSessionId);
          const isEnded = session.status === 'finished' || session.status === 'abandoned';
          
          if (isEnded) {
            // Game has ended, redirect back to lobby/games
            clearInterval(pollInterval!);
            pollInterval = null;
            toast.success('Game has ended');
            navigate(isAIGame ? "/app/games" : "/app/lobbies");
          }
        } catch (error) {
          console.error('Failed to poll session status:', error);
        }
      }, 2000);
    }

    return () => {
      if (pollInterval) {
        clearInterval(pollInterval);
      }
    };
  }, [activeSessionId, lobbySession, aiSession, isAIGame, navigate]);

  if (isAIGame && isLoadingAISession) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Loading game...</p>
      </div>
    );
  }

  if (!isAIGame && (isLoadingLobby || isLoadingLobbySession)) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Loading game...</p>
      </div>
    );
  }

  if (isAIGame && !aiSession) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Game session not found</p>
        <Button
          onClick={() => navigate("/app/games")}
          className="mt-4"
          variant="outline"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Games
        </Button>
      </div>
    );
  }

  if (!isAIGame && !lobby) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Lobby not found</p>
        <Button
          onClick={() => navigate("/app/lobbies")}
          className="mt-4"
          variant="outline"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Lobbies
        </Button>
      </div>
    );
  }

  // If this is a chess game, don't render - redirect should handle it, but show loading just in case
  if (!isAIGame && isChessGame) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Redirecting to chess game...</p>
      </div>
    );
  }

  if (!isAIGame && !lobby?.sessionId) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Waiting for game session to start...</p>
        <Button
          onClick={() => navigate("/app/lobbies")}
          className="mt-4"
          variant="outline"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Lobbies
        </Button>
      </div>
    );
  }

  // Check if session is ended before rendering game
  const sessionStatus = lobbySession?.status || aiSession?.status;
  if (sessionStatus === 'finished' || sessionStatus === 'abandoned') {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">This game has ended</p>
        <Button
          onClick={() => navigate(isAIGame ? "/app/games" : "/app/lobbies")}
          className="mt-4"
          variant="outline"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          {isAIGame ? "Back to Games" : "Back to Lobbies"}
        </Button>
      </div>
    );
  }

  if (!activeSessionId) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Game session not available</p>
        <Button
          onClick={() => navigate(isAIGame ? "/app/games" : "/app/lobbies")}
          className="mt-4"
          variant="outline"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2">Game Session</h1>
          <p className="text-muted-foreground">Playing Connect 4</p>
        </div>
        <Button
          onClick={() => navigate(isAIGame ? "/app/games" : "/app/lobbies")}
          variant="outline"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          {isAIGame ? "Back to Games" : "Back to Lobbies"}
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Sidebar with lobby/player info */}
        <div className="lg:col-span-1 space-y-4">
          {/* Lobby Info Card (only for lobby-based games) */}
          {!isAIGame && lobby && (
            <Card className="border-2 border-primary bg-primary/5">
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  {lobby.name || 'Lobby Information'}
                  {isHost && (
                    <Crown className="w-4 h-4 text-yellow-500" />
                  )}
                </CardTitle>
                {lobby.description && (
                  <p className="text-sm text-muted-foreground mt-1">
                    {lobby.description}
                  </p>
                )}
                {lobby.gameId && (
                  <p className="text-sm text-muted-foreground mt-1">
                    Game: {games.find(g => g.id.toString() === lobby.gameId)?.title || lobby.gameId}
                  </p>
                )}
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Users className="w-4 h-4" />
                  <span>
                    {lobby.playerIds.length} / {lobby.maxPlayers} players
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <Badge className={getStatusColor(lobby.status)}>
                    {lobby.status}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          )}

          {/* AI Game Info Card */}
          {isAIGame && aiSession && (
            <Card className="border-2 border-primary bg-primary/5">
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <Bot className="w-4 h-4" />
                  AI Game
                </CardTitle>
                <p className="text-sm text-muted-foreground mt-1 capitalize">
                  Difficulty: {aiLevel}
                </p>
              </CardHeader>
            </Card>
          )}

          {/* Opponent Info Card (only for human opponents) */}
          {!isAIGame && !isAIOpponent && opponent && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Opponent</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <div>
                  <p className="font-semibold">{opponent.username}</p>
                  <p className="text-sm text-muted-foreground">Rank: {opponent.rank}</p>
                </div>
                {opponent.bio && (
                  <p className="text-sm text-muted-foreground">{opponent.bio}</p>
                )}
              </CardContent>
            </Card>
          )}

          {/* AI Opponent Info Card */}
          {(isAIGame || isAIOpponent) && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <Bot className="w-4 h-4" />
                  AI Opponent
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <div>
                  <p className="font-semibold">AI Player</p>
                  <p className="text-sm text-muted-foreground capitalize">
                    Difficulty: {aiLevel}
                  </p>
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Game area */}
        <div className="lg:col-span-3">
          <ConnectFourGame
            sessionId={activeSessionId}
            playerIds={activePlayerIds}
            lobbyId={isAIGame ? undefined : lobby?.id}
            aiLevel={isAIGame || isAIOpponent ? aiLevel : undefined}
            onGameEnd={() => {
              // Game ended, redirect back to lobby/games
              setTimeout(() => {
                navigate(isAIGame ? "/app/games" : "/app/lobbies");
              }, 2000); // Give user 2 seconds to see the game over message
            }}
            onQuit={() => {
              navigate(isAIGame ? "/app/games" : "/app/lobbies");
            }}
          />
        </div>
      </div>
    </div>
  );
};

