import { useState, useRef, useEffect } from "react";
import { toast } from "sonner";
import { Users, Search, Plus, Crown, Play, LogOut, Gamepad2, X, Edit } from "lucide-react";
import { useKeycloak } from "@react-keycloak/web";

import { InputComponents, DisplayComponents } from '../../components/app'
import { Badge } from '../../components/shared'
import { SelectTrigger, SelectValue, SelectContent, SelectItem } from '../../components/app/input/Select'
import { LobbySearchParams } from "../../types/api.types";
import { useNavigate } from "react-router-dom";
import { LobbyStatus } from "../../types/app.types";
import { useAllLobbies, useCreateLobby, useJoinLobby, useCurrentPlayerLobby, useStartLobby, useLeaveLobby, useUpdateLobby } from "../../hooks/lobby/useLobby";
import { useGetAllGames } from "../../hooks/game/useGame";

const { Button, Input, Select, TextArea, Label } = InputComponents
const { Card, CardContent, CardHeader, CardTitle } = DisplayComponents

// Chess game ID from game service
const CHESS_GAME_ID = "550e8400-e29b-41d4-a716-446655440002"

export const Lobbies = () => {
  const { keycloak } = useKeycloak();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useState<LobbySearchParams>({});
  const [gameIdFilter, setGameIdFilter] = useState("");
  const [selectedGameId, setSelectedGameId] = useState<string>("");
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [lobbyName, setLobbyName] = useState("");
  const [lobbyDescription, setLobbyDescription] = useState("");
  const [maxPlayers, setMaxPlayers] = useState(4);
  const [isPrivate, setIsPrivate] = useState(false);
  const [showUpdateDialog, setShowUpdateDialog] = useState(false);
  const [updateLobbyName, setUpdateLobbyName] = useState("");
  const [updateLobbyDescription, setUpdateLobbyDescription] = useState("");
  const [isForceLeaving, setIsForceLeaving] = useState(false);
  const currentLobbyRef = useRef<HTMLDivElement>(null);

  const { data: lobbiesData, isLoading, refetch } = useAllLobbies(searchParams);
  const { data: currentLobby, refetch: refetchCurrentLobby } = useCurrentPlayerLobby();
  const { data: gamesResponse } = useGetAllGames();
  const games = gamesResponse?.games || [];
  const createLobby = useCreateLobby();
  const joinLobby = useJoinLobby();
  const startLobby = useStartLobby();
  const leaveLobby = useLeaveLobby();
  const updateLobby = useUpdateLobby();

  const lobbies = lobbiesData?.content || [];

  // Check if current user is the host
  const isHost = currentLobby?.hostId === keycloak.tokenParsed?.sub;

  // Poll for sessionId if lobby is started but sessionId is not yet available
  useEffect(() => {
    let pollInterval: NodeJS.Timeout | null = null;

    if (currentLobby &&
      (currentLobby.status === 'STARTED' || currentLobby.status === 'IN_PROGRESS') &&
      !currentLobby.sessionId) {
      // Poll for sessionId every 500ms for up to 10 seconds
      let attempts = 0;
      const maxAttempts = 20;

      pollInterval = setInterval(async () => {
        attempts++;
        try {
          const result = await refetchCurrentLobby();
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
  }, [currentLobby?.status, currentLobby?.sessionId, refetchCurrentLobby]);

  // Navigate to game page when sessionId is available
  // Only redirect if user is actually in the lobby (check playerIds) and game is not ended
  // Skip for chess games - they are handled by LobbyDetail with external game instance redirect
  useEffect(() => {
    const currentUserId = keycloak.tokenParsed?.sub;
    const sessionId = currentLobby?.sessionId;
    const isChessGame = currentLobby?.gameId === CHESS_GAME_ID;
    
    // For chess games, don't redirect here - LobbyDetail handles it with external game instance
    if (isChessGame) {
      return;
    }
    
    if (sessionId && 
        currentUserId && 
        currentLobby.playerIds?.includes(currentUserId) &&
        currentLobby.status !== 'COMPLETED' &&
        currentLobby.status !== 'CANCELLED') {
      // Check session status before redirecting
      const checkAndRedirect = async () => {
        try {
          // Dynamic import to avoid circular dependency issues
          const { gameService } = await import("../../service/game.service");
          const session = await gameService.getSession(sessionId);
          // Only redirect if session is still active
          if (session.status === 'active' || session.status === 'created') {
            navigate(`/app/game/${currentLobby.id}`);
          } else {
            // Session is ended, don't redirect - user should stay on lobbies page
            console.log('Game session has ended, not redirecting to game page');
          }
        } catch (error) {
          console.error('Failed to check session status:', error);
          // If we can't check, still redirect (better UX than blocking)
          navigate(`/app/game/${currentLobby.id}`);
        }
      };
      checkAndRedirect();
    }
  }, [currentLobby?.sessionId, currentLobby?.id, currentLobby?.gameId, currentLobby?.playerIds, currentLobby?.status, keycloak.tokenParsed?.sub, navigate]);

  const handleSearch = () => {
    setSearchParams({
      ...(gameIdFilter && { gameId: gameIdFilter }),
    });
    refetch();
  };

  const handleCreateLobby = async () => {
    if (!lobbyName.trim()) {
      toast.error("Lobby name is required");
      return;
    }

    try {
      // First check if user is already in a lobby
      // Refresh the current lobby data to make sure we have the latest
      const result = await refetchCurrentLobby();

      if (result.data) {
        toast.error("You are already in a lobby! A player can only be in one lobby at a time. Please leave your current lobby before creating a new one.");
        // Scroll to the current lobby card
        setTimeout(() => {
          currentLobbyRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }, 100);
        return;
      }

      // User is not in a lobby, create a new one
      await createLobby.mutateAsync({
        name: lobbyName.trim(),
        description: lobbyDescription.trim() || null,
        maxPlayers,
        private: isPrivate,
      });
      toast.success("Lobby created successfully!");
      setShowCreateDialog(false);
      setLobbyName("");
      setLobbyDescription("");
      setMaxPlayers(4);
      setIsPrivate(false);
      refetch();
      refetchCurrentLobby();
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || "Failed to create lobby";
      toast.error(errorMessage);
    }
  };

  const handleJoinLobby = async (lobbyId: string) => {
    try {
      // First check if user is already in a lobby
      const currentLobbyResult = await refetchCurrentLobby();

      if (currentLobbyResult.data) {
        if (currentLobbyResult.data.id === lobbyId) {
          toast.info("You are already in this lobby!");
          return;
        } else {
          toast.error("You are already in a lobby. Please leave your current lobby before joining another one.");
          // Scroll to the current lobby card
          setTimeout(() => {
            currentLobbyRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
          }, 100);
          return;
        }
      }

      await joinLobby.mutateAsync(lobbyId);
      toast.success("Joined lobby successfully!");
      refetch();
      refetchCurrentLobby();
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || "Failed to join lobby";
      toast.error(errorMessage);
    }
  };

  const handleLeaveLobby = async () => {
    if (!currentLobby) {
      return;
    }

    try {
      await leaveLobby.mutateAsync(currentLobby.id);
      toast.success("Left lobby successfully!");
      // Refetch immediately after successful leave
      await Promise.all([refetch(), refetchCurrentLobby()]);
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || "Failed to leave lobby";
      // If error is "not in lobby", treat it as success (idempotent operation)
      if (errorMessage.toLowerCase().includes("not in the lobby") || 
          errorMessage.toLowerCase().includes("player is not in")) {
        toast.success("Left lobby successfully!");
        // Clear cache to ensure UI is updated
        await Promise.all([refetch(), refetchCurrentLobby()]);
      } else {
        toast.error(errorMessage);
        // Still refetch in case of error to ensure UI is in sync
        await Promise.all([refetch(), refetchCurrentLobby()]);
      }
    }
  };
;
  const handleUpdateLobby = async () => {
    if (!currentLobby || !updateLobbyName.trim()) {
      toast.error("Lobby name is required");
      return;
    }

    try {
      await updateLobby.mutateAsync({
        lobbyId: currentLobby.id,
        data: {
          name: updateLobbyName.trim(),
          description: updateLobbyDescription.trim() || null,
        },
      });
      toast.success("Lobby updated successfully!");
      setShowUpdateDialog(false);
      refetch();
      refetchCurrentLobby();
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message || "Failed to update lobby";
      toast.error(errorMessage);
    }
  };

  const openUpdateDialog = () => {
    if (currentLobby) {
      setUpdateLobbyName(currentLobby.name || "");
      setUpdateLobbyDescription(currentLobby.description || "");
      setShowUpdateDialog(true);
    }
  };

  const handleStartLobby = async () => {
    if (!currentLobby || !selectedGameId) {
      toast.error("Please select a game first");
      return;
    }

    try {
      await startLobby.mutateAsync({
        lobbyId: currentLobby.id,
        data: { gameId: selectedGameId }
      });
      toast.success("Lobby started! Waiting for game session...");

      // Poll for sessionId - the game service creates it asynchronously via RabbitMQ
      let attempts = 0;
      const maxAttempts = 30; // Poll for up to 15 seconds (30 * 500ms)
      let pollInterval: NodeJS.Timeout | null = null;

      pollInterval = setInterval(async () => {
        attempts++;
        try {
          const result = await refetchCurrentLobby();

          if (result.data?.sessionId) {
            if (pollInterval) {
              clearInterval(pollInterval);
              pollInterval = null;
            }
            toast.success("Game session started!");
            refetch();
          } else if (attempts >= maxAttempts) {
            if (pollInterval) {
              clearInterval(pollInterval);
              pollInterval = null;
            }
            toast.warning("Game session is taking longer than expected. The session may still be initializing.");
          }
        } catch (error) {
          console.error('Error polling for sessionId:', error);
          if (attempts >= maxAttempts) {
            if (pollInterval) {
              clearInterval(pollInterval);
              pollInterval = null;
            }
          }
        }
      }, 500); // Poll every 500ms

      // Also refetch immediately
      await refetchCurrentLobby();
      refetch();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Failed to start lobby");
    }
  };

  const handleForceLeave = async () => {
    if (!currentLobby) {
      return;
    }

    if (!confirm('Are you sure you want to force leave this game? This will abandon the game session and leave the lobby.')) {
      return;
    }

    setIsForceLeaving(true);
    const currentUserId = keycloak.tokenParsed?.sub || '';

    try {
      // First, try to abandon the game session if it exists
      const sessionId = currentLobby.sessionId;
      if (sessionId) {
        try {
          const { gameService } = await import("../../service/game.service");
          await gameService.abandonSession(sessionId, currentUserId);
          toast.success('Game session abandoned');
        } catch (error: any) {
          // Continue even if abandon fails - the session might already be ended or in a weird state
          console.warn('Failed to abandon session (continuing anyway):', error);
          // Don't show error - we'll still try to leave the lobby
        }
      }

      // Then leave the lobby
      await leaveLobby.mutateAsync(currentLobby.id);
      toast.success('Left the lobby successfully');
      
      // Refetch to update UI
      await refetchCurrentLobby();
      refetch();
    } catch (error: any) {
      console.error('Failed to force leave:', error);
      const errorMessage = error?.response?.data?.message || error?.response?.data?.detail || 'Failed to leave lobby';
      // If error is "not in lobby", treat it as success (idempotent operation)
      if (errorMessage.toLowerCase().includes("not in the lobby") || 
          errorMessage.toLowerCase().includes("player is not in")) {
        toast.success('Left the lobby successfully');
        // Refetch to update UI
        await refetchCurrentLobby();
        refetch();
      } else {
        toast.error(errorMessage);
      }
    } finally {
      setIsForceLeaving(false);
    }
  };

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

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2">Game Lobbies</h1>
          <p className="text-muted-foreground">Find and join game lobbies</p>
        </div>
        <Button onClick={() => setShowCreateDialog(true)}>
          <Plus className="w-4 h-4 mr-2" />
          Create Lobby
        </Button>
      </div>

      {/* Create Lobby Dialog */}
      {showCreateDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md mx-4">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Create New Lobby</CardTitle>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setShowCreateDialog(false);
                    setLobbyName("");
                    setLobbyDescription("");
                    setMaxPlayers(4);
                    setIsPrivate(false);
                  }}
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="lobby-name">Lobby Name *</Label>
                <Input
                  id="lobby-name"
                  value={lobbyName}
                  onChange={(e) => setLobbyName(e.target.value)}
                  placeholder="Enter lobby name"
                  maxLength={100}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="lobby-description">Description</Label>
                <TextArea
                  id="lobby-description"
                  value={lobbyDescription}
                  onChange={(e) => setLobbyDescription(e.target.value)}
                  placeholder="Enter lobby description (optional)"
                  maxLength={500}
                  rows={3}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="max-players">Max Players</Label>
                <Select
                  value={maxPlayers.toString()}
                  onValueChange={(value) => setMaxPlayers(parseInt(value))}
                >
                  <SelectTrigger id="max-players">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[2, 3, 4, 5, 6, 7, 8, 9, 10].map((num) => (
                      <SelectItem key={num} value={num.toString()}>
                        {num} players
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="is-private"
                  checked={isPrivate}
                  onChange={(e) => setIsPrivate(e.target.checked)}
                  className="w-4 h-4 rounded border-gray-300"
                />
                <Label htmlFor="is-private" className="cursor-pointer">
                  Private lobby (requires invitation)
                </Label>
              </div>

              <div className="flex gap-2 pt-2">
                <Button
                  onClick={handleCreateLobby}
                  disabled={createLobby.isPending || !lobbyName.trim()}
                  className="flex-1"
                >
                  {createLobby.isPending ? "Creating..." : "Create Lobby"}
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowCreateDialog(false);
                    setLobbyName("");
                    setLobbyDescription("");
                    setMaxPlayers(4);
                    setIsPrivate(false);
                  }}
                  className="flex-1"
                >
                  Cancel
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Show current lobby prominently if user is in one */}
      {currentLobby && (
        <div ref={currentLobbyRef}>
          <Card className="border-2 border-primary bg-primary/5">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <CardTitle className="text-lg flex items-center gap-2">
                    {currentLobby.name || 'Your Current Lobby'}
                    {isHost && (
                      <Crown className="w-4 h-4 text-yellow-500" />
                    )}
                  </CardTitle>
                  {currentLobby.description && (
                    <p className="text-sm text-muted-foreground mt-1">
                      {currentLobby.description}
                    </p>
                  )}
                  {currentLobby.gameId && (
                    <p className="text-sm text-muted-foreground mt-1">
                      Game: {games.find(g => g.id.toString() === currentLobby.gameId)?.title || currentLobby.gameId}
                    </p>
                  )}
                </div>
                <Badge className={getStatusColor(currentLobby.status)}>
                  {currentLobby.status}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Users className="w-4 h-4" />
                <span>
                  {currentLobby.playerIds.length} / {currentLobby.maxPlayers} players
                </span>
              </div>

              <div className="flex items-center gap-2">
                <Badge>
                  {currentLobby.visibility}
                </Badge>
              </div>

              {/* Edit button for host when lobby is waiting */}
              {isHost && currentLobby.status === "WAITING" && (
                <div className="pt-2 border-t border-border">
                  <Button
                    onClick={openUpdateDialog}
                    variant="outline"
                    className="w-full"
                  >
                    <Edit className="w-4 h-4 mr-2" />
                    Edit Lobby
                  </Button>
                </div>
              )}

              {/* Game selection and start button for host */}
              {isHost && currentLobby.status === "WAITING" && !currentLobby.gameId && (
                <div className="space-y-3 pt-2 border-t border-border">
                  <div className="space-y-2">
                    <label className="text-sm font-medium">Select Game</label>
                    <Select
                      value={selectedGameId}
                      onValueChange={setSelectedGameId}
                      disabled={startLobby.isPending}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Choose a game..." />
                      </SelectTrigger>
                      <SelectContent>
                        {games.map((game) => (
                          <SelectItem key={game.id} value={game.id.toString()}>
                            {game.title}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {selectedGameId && (
                    <Button
                      onClick={handleStartLobby}
                      disabled={startLobby.isPending}
                      className="w-full"
                    >
                      <Play className="w-4 h-4 mr-2" />
                      Start Lobby
                    </Button>
                  )}
                </div>
              )}

              {/* Force Leave Game button - always visible when in a lobby */}
              <div className="pt-2 border-t border-border space-y-2">
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

              {/* Open Game button - show when sessionId exists */}
              {currentLobby?.sessionId && (
                <div className="pt-2 border-t border-border">
                  <Button
                    onClick={() => navigate(`/app/game/${currentLobby.id}`)}
                    className="w-full"
                  >
                    <Gamepad2 className="w-4 h-4 mr-2" />
                    Open Game
                  </Button>
                </div>
              )}

              {/* Leave lobby button - available for all statuses except COMPLETED */}
              {currentLobby.status !== "COMPLETED" && (
                <div className="pt-2 border-t border-border">
                  <Button
                    onClick={handleLeaveLobby}
                    disabled={leaveLobby.isPending}
                    variant="outline"
                    className="w-full"
                  >
                    <LogOut className="w-4 h-4 mr-2" />
                    Leave Lobby
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Update Lobby Dialog */}
      {showUpdateDialog && currentLobby && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md mx-4">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Update Lobby</CardTitle>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowUpdateDialog(false)}
                >
                  <X className="w-4 h-4" />
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="update-lobby-name">Lobby Name *</Label>
                <Input
                  id="update-lobby-name"
                  value={updateLobbyName}
                  onChange={(e) => setUpdateLobbyName(e.target.value)}
                  placeholder="Enter lobby name"
                  maxLength={100}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="update-lobby-description">Description</Label>
                <TextArea
                  id="update-lobby-description"
                  value={updateLobbyDescription}
                  onChange={(e) => setUpdateLobbyDescription(e.target.value)}
                  placeholder="Enter lobby description (optional)"
                  maxLength={500}
                  rows={3}
                />
              </div>

              <div className="flex gap-2 pt-2">
                <Button
                  onClick={handleUpdateLobby}
                  disabled={updateLobby.isPending || !updateLobbyName.trim()}
                  className="flex-1"
                >
                  {updateLobby.isPending ? "Updating..." : "Update Lobby"}
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setShowUpdateDialog(false)}
                  className="flex-1"
                >
                  Cancel
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Search Lobbies</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4">
            <div className="flex-1">
              <Input
                placeholder="Game ID (optional)"
                value={gameIdFilter}
                onChange={(e) => setGameIdFilter(e.target.value)}
              />
            </div>
            <Button onClick={handleSearch}>
              <Search className="w-4 h-4 mr-2" />
              Search
            </Button>
          </div>
        </CardContent>
      </Card>

      {isLoading ? (
        <div className="text-center py-12 text-muted-foreground">Loading lobbies...</div>
      ) : lobbies.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No lobbies found. Create one to get started!
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {lobbies.map((lobby) => {
            const isHost = lobby.hostId === keycloak.tokenParsed?.sub;
            const isFull = lobby.playerIds.length >= lobby.maxPlayers;
            const canJoin = !isHost && !isFull && lobby.status === "WAITING";

            return (
              <Card key={lobby.id} className="hover:border-accent transition-colors">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <CardTitle className="text-lg flex items-center gap-2">
                        {lobby.name || 'Unnamed Lobby'}
                        {isHost && <Crown className="w-4 h-4 text-yellow-500" />}
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
                    </div>
                    <Badge className={getStatusColor(lobby.status)}>
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

                  <div className="flex items-center gap-2">
                    <Badge>
                      {lobby.visibility}
                    </Badge>
                  </div>

                  {canJoin && (
                    <Button
                      onClick={() => handleJoinLobby(lobby.id)}
                      disabled={joinLobby.isPending}
                      className="w-full"
                    >
                      Join Lobby
                    </Button>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
};
