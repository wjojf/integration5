import { useState } from "react";
import { Search, Users, Bot, X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";

import type {GamesResponse} from '../../types/api.types'
import type {Game } from '../../types/app.types'
import { Badge } from '../../components/shared'
import { InputComponents, DisplayComponents } from '../../components/app'
import { useGetAllGames } from '../../hooks/game/useGame'
import { useCreateLobby, useCurrentPlayerLobby } from '../../hooks/lobby/useLobby'
import { gameService, CreateSessionRequest } from '../../service/game.service'
import { aiPlayerService, LevelsResponse } from '../../service/aiPlayer.service'
import { useKeycloak } from '@react-keycloak/web'

const { Button, Input  } = InputComponents
const { Card, Image } = DisplayComponents

// Chess frontend URL - external chess game frontend
const CHESS_FRONTEND_URL = import.meta.env.VITE_CHESS_FRONTEND_URL || 'http://localhost:3333'

// Chess game ID from game service
const CHESS_GAME_ID = "550e8400-e29b-41d4-a716-446655440002"

export const GameLibrary = () => {
  const { keycloak } = useKeycloak()
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedGame, setSelectedGame] = useState<Game | null>(null)
  const [showPlayDialog, setShowPlayDialog] = useState(false)
  const [showAIDialog, setShowAIDialog] = useState(false)
  const [aiLevels, setAiLevels] = useState<LevelsResponse | null>(null)
  const [selectedLevel, setSelectedLevel] = useState<string>('medium')
  const [isLoadingLevels, setIsLoadingLevels] = useState(false)
  const [isCreatingAISession, setIsCreatingAISession] = useState(false)
  const [loadingGameId, setLoadingGameId] = useState<string | null>(null)
  const navigate = useNavigate()
  const { data: gamesResponse = {} as GamesResponse, isLoading: isLoadingGames } = useGetAllGames()
  const { games = [] } = gamesResponse
  const createLobby = useCreateLobby()
  const { refetch: refetchCurrentLobby } = useCurrentPlayerLobby()

  const filteredGames = games.filter((game) =>
    game.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    game.genre.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const isChessGame = (game: Game): boolean => {
    return game.title.toLowerCase() === 'chess'
  }

  const handlePlayGame = (game: Game) => {
    // Special handling for chess - create lobby directly
    if (isChessGame(game)) {
      handleChessGame(game)
    } else {
      // For other games, show play dialog with options
      setSelectedGame(game)
      setShowPlayDialog(true)
    }
  }

  const handleChessGame = async (game: Game) => {
    try {
      setLoadingGameId(game.id.toString())
      
      // Check if user is already in a lobby
      const currentLobbyResult = await refetchCurrentLobby()
      if (currentLobbyResult.data) {
        toast.error("You are already in a lobby! Please leave your current lobby before creating a new one.")
        setLoadingGameId(null)
        return
      }

      // Create a lobby for chess (max 2 players)
      const lobby = await createLobby.mutateAsync({
        name: `${game.title} Lobby`,
        description: `A lobby for ${game.title}`,
        maxPlayers: 2,
        private: false,
      })
      
      toast.success("Chess lobby created! Waiting for another player...")
      
      // Navigate to lobbies page
      navigate("/app/lobbies")
    } catch (error: any) {
      console.error('Failed to create chess lobby:', error)
      const errorMessage = error?.response?.data?.message || "Failed to create chess lobby"
      toast.error(errorMessage)
    } finally {
      setLoadingGameId(null)
    }
  }

  const handleStartLobby = async () => {
    if (!selectedGame) return

    try {
      // Check if user is already in a lobby
      const currentLobbyResult = await refetchCurrentLobby()
      if (currentLobbyResult.data) {
        toast.error("You are already in a lobby! Please leave your current lobby before creating a new one.")
        setShowPlayDialog(false)
        return
      }

      // Create lobby with name and description (don't start it yet - let others join first)
      await createLobby.mutateAsync({
        name: `${selectedGame.title} Lobby`,
        description: `A lobby for ${selectedGame.title}`,
        maxPlayers: 2,
        private: false,
      })
      
      toast.success("Lobby created! Waiting for other players to join...")
      setShowPlayDialog(false)
      
      // Navigate to lobbies page where host can see the lobby and start when ready
      navigate("/app/lobbies")
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || "Failed to create lobby"
      toast.error(errorMessage)
    }
  }

  const handlePlayWithAI = async () => {
    setShowPlayDialog(false)
    setIsLoadingLevels(true)
    
    try {
      // Fetch available AI levels
      const levels = await aiPlayerService.getLevels()
      setAiLevels(levels)
      setShowAIDialog(true)
    } catch (error: any) {
      console.error('Failed to load AI levels:', error)
      toast.error('Failed to load AI difficulty levels')
    } finally {
      setIsLoadingLevels(false)
    }
  }

  const handleStartAIGame = async () => {
    if (!selectedGame || !selectedLevel) return

    setIsCreatingAISession(true)
    try {
      const currentUserId = keycloak.tokenParsed?.sub || ''
      const aiPlayerId = 'ai_p2'
      
      // Create session with AI as opponent
      const sessionRequest: CreateSessionRequest = {
        game_id: selectedGame.id.toString(),
        game_type: 'connect_four',
        player_ids: [currentUserId, aiPlayerId],
        starting_player_id: currentUserId, // Human starts
        configuration: {
          ai_level: selectedLevel,
        },
      }

      const session = await gameService.createSession(sessionRequest)
      
      toast.success("AI game started!")
      setShowAIDialog(false)
      
      // Navigate to game page using session_id
      navigate(`/app/game/ai/${session.session_id}`)
    } catch (error: any) {
      console.error('Failed to create AI game session:', error)
      const errorMessage = error?.response?.data?.detail || error?.response?.data?.message || 'Failed to start AI game'
      toast.error(errorMessage)
    } finally {
      setIsCreatingAISession(false)
    }
  }

  return (
    <>
      <div className="space-y-6">
        <div>
          <h1 className="mb-2">Game Library</h1>
          <p className="text-muted-foreground">Browse and join your favorite games</p>
        </div>

        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-muted-foreground" />
          <Input
            type="text"
            placeholder="Search games by title or genre..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10"
          />
        </div>

        {isLoadingGames ? (
          <div className="text-center py-12">
            <p className="text-muted-foreground">Loading games...</p>
          </div>
        ) : filteredGames.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-muted-foreground">No games found matching "{searchTerm}"</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredGames.map((game) => (
              <Card key={game.id} className="overflow-hidden bg-card border-border flex flex-col">
                <div className="relative w-full h-48 flex-shrink-0">
                  <Image
                    src={game.image}
                    alt={game.title}
                    className="w-full h-full object-cover"
                  />
                </div>

                <div className="p-4 space-y-3 flex-1 flex flex-col">
                  <div>
                    <h3 className="mb-1">{game.title}</h3>
                    <Badge className={`bg-blue-500/20 text-blue-400 uppercase border-0 text-xs rounded px-2 py-1 mb-3`}>
                      {game.genre}
                    </Badge>
                  </div>

                  <Button
                    className="w-full mt-auto cursor-pointer"
                    onClick={(e) => {
                      e.preventDefault();
                      e.stopPropagation();
                      handlePlayGame(game);
                    }}
                    type="button"
                    disabled={loadingGameId === game.id.toString()}
                  >
                    {loadingGameId === game.id.toString() ? 'Loading...' : 'Play Now'}
                  </Button>
                </div>
              </Card>
            ))}
          </div>
        )}
      </div>

      {/* Play Game Dialog */}
      {showPlayDialog && (
        <div className="fixed inset-0 z-[9998] flex items-center justify-center">
          <div 
            className="fixed inset-0 bg-black/40 backdrop-blur-[1px]"
            onClick={() => setShowPlayDialog(false)}
          />
          <div className="relative z-[9999] w-full max-w-md rounded-lg bg-card border border-border p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">
                Play {selectedGame?.title}
              </h2>
              <button
                onClick={() => setShowPlayDialog(false)}
                className="h-8 w-8 rounded-full hover:bg-secondary flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-sm text-muted-foreground mb-6">
              Choose how you want to play this game
            </p>

            <div className="space-y-3">
              <Button
                className="w-full justify-start"
                onClick={handleStartLobby}
                disabled={createLobby.isPending}
              >
                <Users className="w-4 h-4 mr-2" />
                {createLobby.isPending ? "Creating..." : "Create a Lobby (Play with Friends)"}
              </Button>

              <Button
                className="w-full justify-start"
                variant="outline"
                onClick={handlePlayWithAI}
                disabled={isLoadingLevels}
              >
                <Bot className="w-4 h-4 mr-2" />
                {isLoadingLevels ? "Loading..." : "Play with AI"}
              </Button>
            </div>

            <div className="mt-6 flex justify-end">
              <Button
                variant="outline"
                onClick={() => setShowPlayDialog(false)}
              >
                Cancel
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* AI Difficulty Selection Dialog */}
      {showAIDialog && (
        <div className="fixed inset-0 z-[9998] flex items-center justify-center">
          <div 
            className="fixed inset-0 bg-black/40 backdrop-blur-[1px]"
            onClick={() => setShowAIDialog(false)}
          />
          <div className="relative z-[9999] w-full max-w-md rounded-lg bg-card border border-border p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">
                Choose AI Difficulty
              </h2>
              <button
                onClick={() => setShowAIDialog(false)}
                className="h-8 w-8 rounded-full hover:bg-secondary flex items-center justify-center"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <p className="text-sm text-muted-foreground mb-6">
              Select the difficulty level for your AI opponent
            </p>

            {aiLevels && (
              <div className="space-y-3 mb-6">
                {aiLevels.levels.map((level) => (
                  <button
                    key={level.level}
                    onClick={() => setSelectedLevel(level.level)}
                    className={`w-full p-4 rounded-lg border-2 text-left transition-colors ${
                      selectedLevel === level.level
                        ? 'border-primary bg-primary/10'
                        : 'border-border hover:border-primary/50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-semibold capitalize">{level.level}</p>
                        <p className="text-sm text-muted-foreground">
                          {level.iterations} iterations
                        </p>
                      </div>
                      {selectedLevel === level.level && (
                        <div className="w-4 h-4 rounded-full bg-primary" />
                      )}
                    </div>
                  </button>
                ))}
              </div>
            )}

            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={() => setShowAIDialog(false)}
                className="flex-1"
              >
                Cancel
              </Button>
              <Button
                onClick={handleStartAIGame}
                disabled={isCreatingAISession || !selectedLevel}
                className="flex-1"
              >
                {isCreatingAISession ? "Starting..." : "Start Game"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
