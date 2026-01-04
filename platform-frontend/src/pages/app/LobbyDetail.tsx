import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { Users, Crown, Play, Loader2, LogOut } from "lucide-react";
import { useKeycloak } from "@react-keycloak/web";

import { InputComponents, DisplayComponents } from '../../components/app'
import { Badge } from '../../components/shared'
import { useLobbyById, useStartLobby, useExternalGameInstance, useLeaveLobby } from "../../hooks/lobby/useLobby";
import { gameService } from "../../service/game.service";

const { Button } = InputComponents
const { Card, CardContent, CardHeader, CardTitle } = DisplayComponents

// Chess game ID from game service
const CHESS_GAME_ID = "550e8400-e29b-41d4-a716-446655440002"
const CHESS_FRONTEND_URL = import.meta.env.VITE_CHESS_FRONTEND_URL || 'http://localhost:3333'

export const LobbyDetail = () => {
  const { lobbyId } = useParams<{ lobbyId: string }>()
  const navigate = useNavigate()
  const { keycloak } = useKeycloak()
  
  const { data: lobby, isLoading, refetch } = useLobbyById(lobbyId || '')
  const startLobby = useStartLobby()
  const leaveLobby = useLeaveLobby()
  const [isForceLeaving, setIsForceLeaving] = useState(false)
  
  // Poll for external game instance if lobby is started and game is chess
  const isChessGame = lobby?.gameId === CHESS_GAME_ID
  const isLobbyStarted = lobby?.status === "STARTED" || lobby?.status === "IN_PROGRESS"
  const shouldPoll = isLobbyStarted && isChessGame && Boolean(lobbyId)
  
  const { data: externalGameData, isFetching: isFetchingExternalGame, error: externalGameError } = useExternalGameInstance(
    lobbyId || '',
    shouldPoll
  )
  
  // Log external game data changes for debugging
  useEffect(() => {
    if (shouldPoll) {
      console.log("[LobbyDetail] External game instance state:", {
        hasData: !!externalGameData,
        data: externalGameData,
        isFetching: isFetchingExternalGame,
        error: externalGameError,
        shouldPoll
      })
    }
  }, [externalGameData, isFetchingExternalGame, externalGameError, shouldPoll])
  
  // Debug logging
  useEffect(() => {
    if (shouldPoll) {
      console.log("Polling for external game instance:", {
        lobbyId,
        gameId: lobby?.gameId,
        status: lobby?.status,
        isChessGame,
        isLobbyStarted
      })
    }
  }, [shouldPoll, lobbyId, lobby?.gameId, lobby?.status, isChessGame, isLobbyStarted])
  
  // Redirect to chess frontend when game is ready
  useEffect(() => {
    if (externalGameData?.hasExternalGameInstance && externalGameData.externalGameInstanceId) {
      const externalGameInstanceId = externalGameData.externalGameInstanceId
      const chessGameUrl = `${CHESS_FRONTEND_URL}/game/${externalGameInstanceId}`
      console.log("Chess game ready, redirecting to:", chessGameUrl)
      toast.success("Redirecting to chess game...")
      // Redirect to chess frontend
      window.location.href = chessGameUrl
    }
  }, [externalGameData])
  
  const isHost = lobby?.hostId === keycloak.tokenParsed?.sub
  const isFull = lobby ? lobby.playerIds.length >= lobby.maxPlayers : false
  const canStart = isHost && isFull && lobby?.status === "WAITING" && lobby?.gameId === CHESS_GAME_ID
  
  const handleStartLobby = async () => {
    if (!lobby || !lobby.gameId) {
      toast.error("No game selected")
      return
    }
    
    try {
      const updatedLobby = await startLobby.mutateAsync({
        lobbyId: lobby.id,
        data: { gameId: lobby.gameId }
      })
      toast.success("Lobby started! Creating chess game...")
      // Refetch to get updated lobby with STARTED status
      await refetch()
      // Also check if it's a chess game and start polling immediately
      if (updatedLobby?.gameId === CHESS_GAME_ID && updatedLobby?.status === "STARTED") {
        console.log("Chess lobby started, waiting for external game instance...")
      }
    } catch (error) {
      console.error('Failed to start lobby:', error)
      toast.error("Failed to start lobby")
    }
  }

  const handleForceLeave = async () => {
    if (!lobby) {
      return
    }

    if (!confirm('Are you sure you want to force leave this game? This will abandon the game session and leave the lobby.')) {
      return
    }

    setIsForceLeaving(true)
    const currentUserId = keycloak.tokenParsed?.sub || ''

    try {
      // First, try to abandon the game session if it exists
      if (lobby.sessionId) {
        try {
          await gameService.abandonSession(lobby.sessionId, currentUserId)
          toast.success('Game session abandoned')
        } catch (error: any) {
          // Continue even if abandon fails - the session might already be ended or in a weird state
          console.warn('Failed to abandon session (continuing anyway):', error)
          // Don't show error - we'll still try to leave the lobby
        }
      }

      // Then leave the lobby
      await leaveLobby.mutateAsync(lobby.id)
      toast.success('Left the lobby successfully')
      
      // Navigate back to lobbies
      navigate("/app/lobbies")
    } catch (error: any) {
      console.error('Failed to force leave:', error)
      const errorMessage = error?.response?.data?.message || error?.response?.data?.detail || 'Failed to leave lobby'
      // If error is "not in lobby", treat it as success (idempotent operation)
      if (errorMessage.toLowerCase().includes("not in the lobby") || 
          errorMessage.toLowerCase().includes("player is not in")) {
        toast.success('Left the lobby successfully')
        navigate("/app/lobbies")
      } else {
        toast.error(errorMessage)
      }
    } finally {
      setIsForceLeaving(false)
    }
  }
  
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
      </div>
    )
  }
  
  if (!lobby) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Lobby not found</p>
      </div>
    )
  }
  
  // Check if we should show loading state while waiting for chess game
  const isWaitingForChessGame = isChessGame && 
    (lobby.status === "STARTED" || lobby.status === "IN_PROGRESS") && 
    (!externalGameData?.hasExternalGameInstance || !externalGameData.externalGameInstanceId)

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div>
        <h1 className="mb-2">{lobby.name || "Lobby Details"}</h1>
        {lobby.description && (
          <p className="text-muted-foreground">{lobby.description}</p>
        )}
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Lobby Info Card - Sidebar on large screens */}
        <div className="lg:col-span-1">
          <Card>
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg flex items-center gap-2">
                    {isHost && <Crown className="w-4 h-4 text-yellow-500" />}
                    {isHost ? "Your Lobby" : "Lobby"}
                  </CardTitle>
                  {lobby.gameId && (
                    <p className="text-sm text-muted-foreground mt-1">
                      Game: {lobby.gameId === CHESS_GAME_ID ? "Chess" : lobby.gameId}
                    </p>
                  )}
                </div>
                <Badge className={
                  lobby.status === "WAITING" ? "bg-yellow-500/20 text-yellow-500" :
                  lobby.status === "STARTED" || lobby.status === "IN_PROGRESS" ? "bg-green-500/20 text-green-500" :
                  "bg-gray-500/20 text-gray-500"
                }>
                  {lobby.status}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Users className="w-4 h-4" />
                <span>
                  {lobby.playerIds.length} / {lobby.maxPlayers} players
                </span>
              </div>
              
              {lobby.status === "WAITING" && (
                <div className="space-y-2">
                  {isChessGame && !isFull && (
                    <p className="text-sm text-muted-foreground">
                      Waiting for {lobby.maxPlayers - lobby.playerIds.length} more player(s) to join...
                    </p>
                  )}
                  
                  {canStart && (
                    <Button
                      onClick={handleStartLobby}
                      disabled={startLobby.isPending}
                      className="w-full"
                    >
                      <Play className="w-4 h-4 mr-2" />
                      {startLobby.isPending ? "Starting..." : "Start Chess Game"}
                    </Button>
                  )}
                </div>
              )}
              
              {isWaitingForChessGame && (
                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Creating chess game... Redirecting shortly...</span>
                  </div>
                </div>
              )}

              {/* Force Leave Game button - always visible */}
              <div className="pt-2 border-t border-border">
                <Button
                  onClick={handleForceLeave}
                  disabled={isForceLeaving}
                  variant="destructive"
                  className="w-full"
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  {isForceLeaving ? "Leaving..." : "Force Leave Game"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Loading state while waiting for chess game redirect */}
        {isWaitingForChessGame && (
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle>Preparing Chess Game</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-col items-center justify-center py-12 space-y-4">
                  <Loader2 className="w-12 h-12 animate-spin text-muted-foreground" />
                  <p className="text-muted-foreground text-center">
                    Creating your chess game... You will be redirected shortly.
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </div>
  )
}

