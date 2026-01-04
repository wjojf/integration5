import { useState, useEffect, useCallback, useRef } from 'react';
import { toast } from 'sonner';
import { useKeycloak } from '@react-keycloak/web';
import { LogOut } from 'lucide-react';
import { Row } from '../connectFour/Row';
import { Button } from '../app/input/Button';
import { useGameWebSocket, GameMoveAppliedEvent, GameSessionEndedEvent } from '../../hooks/game/useGameWebSocket';
import { aiPlayerService } from '../../service/aiPlayer.service';

interface ConnectFourGameProps {
  sessionId: string;
  playerIds: string[];
  lobbyId?: string;
  aiLevel?: string; // AI difficulty level if playing against AI
  onGameEnd?: () => void;
  onQuit?: () => void;
}

export const ConnectFourGame = ({ sessionId, playerIds, lobbyId, aiLevel, onGameEnd, onQuit }: ConnectFourGameProps) => {
  const { keycloak } = useKeycloak();
  const currentUserId = keycloak.tokenParsed?.sub || '';
  
  const [board, setBoard] = useState<(number | null)[][]>(Array(6).fill(null).map(() => Array(7).fill(null)));
  const [currentPlayerId, setCurrentPlayerId] = useState<string>('');
  const [gameStatus, setGameStatus] = useState<string>('active'); // Session status: 'active', 'finished', 'abandoned'
  const [winnerId, setWinnerId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isApplyingMove, setIsApplyingMove] = useState(false);
  const [isAIMakingMove, setIsAIMakingMove] = useState(false);
  const aiProcessingRef = useRef(false);
  
  // Check if current player is AI
  const isAIPlayer = (playerId: string) => playerId === 'ai_p2' || playerId.startsWith('ai_');

  // Get player number (1 or 2) based on position in playerIds array
  const getPlayerNumber = useCallback((playerId: string) => {
    const index = playerIds.indexOf(playerId);
    return index >= 0 ? index + 1 : null;
  }, [playerIds]);

  const updateGameState = useCallback((gameState: any, currentPlayer: string, status: string, winner?: string | null) => {
    // Convert game state board (0 = empty, 1 = player1, 2 = player2) to our format (null = empty, 1 = player1, 2 = player2)
    const boardData = gameState.board || Array(6).fill(null).map(() => Array(7).fill(0));
    const convertedBoard = boardData.map((row: number[]) => 
      row.map((cell: number) => cell === 0 ? null : cell)
    );
    
    setBoard(convertedBoard);
    setCurrentPlayerId(currentPlayer);
    setGameStatus(status);
    setWinnerId(winner || null);
  }, []);

  // Reset state and load initial game state when sessionId changes
  useEffect(() => {
    // Reset all state immediately when sessionId changes
    setBoard(Array(6).fill(null).map(() => Array(7).fill(null)));
    setCurrentPlayerId('');
    setGameStatus('active');
    setWinnerId(null);
    setIsLoading(true);
    setIsApplyingMove(false);
    setIsAIMakingMove(false);
    aiProcessingRef.current = false;

    const loadGameState = async () => {
      try {
        const { gameService } = await import('../../service/game.service');
        const session = await gameService.getSession(sessionId);
        updateGameState(session.game_state, session.current_player_id, session.status, session.winner_id);
        setIsLoading(false);
      } catch (error) {
        console.error('Failed to load game state:', error);
        toast.error('Failed to load game state');
        setIsLoading(false);
      }
    };

    if (sessionId) {
      loadGameState();
    } else {
      setIsLoading(false);
    }
  }, [sessionId, updateGameState]);

  // Handle move applied event from WebSocket
  const handleMoveApplied = useCallback((event: GameMoveAppliedEvent) => {
    updateGameState(event.game_state, event.current_player_id, event.status, event.winner_id);
  }, [updateGameState]);

  // Make AI move
  const makeAIMove = useCallback(async () => {
    if (aiProcessingRef.current || !aiLevel || gameStatus !== 'active' || !sessionId) {
      return;
    }

    // Check if it's AI's turn
    if (!isAIPlayer(currentPlayerId)) {
      return;
    }

    aiProcessingRef.current = true;
    setIsAIMakingMove(true);

    try {
      // Get current session state - always fetch fresh state from server
      const { gameService } = await import('../../service/game.service');
      const session = await gameService.getSession(sessionId);
      
      // Verify session is still active
      if (session.status !== 'active') {
        console.log('Session is no longer active, skipping AI move');
        // Update local state to match backend to prevent infinite loop
        updateGameState(session.game_state, session.current_player_id, session.status, session.winner_id);
        return;
      }

      // Verify it's still the AI's turn (session state is source of truth)
      if (!isAIPlayer(session.current_player_id)) {
        console.log('Not AI turn anymore, skipping AI move. Backend says current player is:', session.current_player_id);
        // IMPORTANT: Update local state to match backend to prevent infinite loop!
        // This happens when WebSocket events are missed or state is out of sync
        updateGameState(session.game_state, session.current_player_id, session.status, session.winner_id);
        return;
      }
      
      // Convert board format for AI (0 = empty, 1 = player1, 2 = player2)
      const aiBoard = session.game_state.board || [];

      // Prepare game state for AI using session data as source of truth
      const gameState = {
        board: aiBoard,
        current_player: session.current_player_id,
        player_ids: session.player_ids || playerIds,
        status: session.status === 'active' ? 'ongoing' : session.status,
        move_number: session.total_moves || 0,
      };

      // Get AI move
      const aiResponse = await aiPlayerService.getAIMove({
        game_state: gameState,
        level: aiLevel as 'low' | 'medium' | 'high' | 'very_high',
        game_type: 'connect_four',
      });

      // Apply AI move
      const { gameService: gameServiceForMove } = await import('../../service/game.service');
      await gameServiceForMove.applyMove(sessionId, {
        player_id: session.current_player_id,
        move: aiResponse.move, // { column: number }
      });

      // The WebSocket will update the state when the move is applied
    } catch (error: any) {
      console.error('Failed to get AI move:', error);
      // Backend sends error in 'error' field, not 'detail'
      const errorMessage = error?.response?.data?.error || error?.response?.data?.detail || error?.message || 'Unknown error';
      // Only show error if it's not a session validation error
      if (!errorMessage?.includes('not active') && 
          !errorMessage?.includes('not found')) {
        toast.error('AI move failed: ' + errorMessage);
      }
    } finally {
      setIsAIMakingMove(false);
      aiProcessingRef.current = false;
    }
  }, [sessionId, currentPlayerId, gameStatus, aiLevel, playerIds, updateGameState]);

  // Auto-trigger AI move when it's AI's turn
  useEffect(() => {
    if (gameStatus === 'active' && isAIPlayer(currentPlayerId) && aiLevel && !isAIMakingMove && !aiProcessingRef.current) {
      // Small delay to ensure state is updated
      const timer = setTimeout(() => {
        makeAIMove();
      }, 500);
      return () => clearTimeout(timer);
    }
  }, [currentPlayerId, gameStatus, aiLevel, isAIMakingMove, makeAIMove]);

  // Handle session ended event
  const handleSessionEnded = useCallback((event: GameSessionEndedEvent) => {
    setGameStatus('finished'); // Session status is 'finished' when game ends
    setWinnerId(event.winner_id || null);
    if (onGameEnd) {
      onGameEnd();
    }
  }, [onGameEnd]);

  // WebSocket connection
  const { connected } = useGameWebSocket(sessionId, {
    onMoveApplied: handleMoveApplied,
    onSessionEnded: handleSessionEnded,
    onError: (error) => {
      console.error('WebSocket error:', error);
    },
    onConnect: () => {
      console.log('Connected to game WebSocket');
    },
    onDisconnect: () => {
      console.log('Disconnected from game WebSocket');
    },
  });

  const play = useCallback(async (column: number) => {
    // Early validation - prevent any move if conditions aren't met
    if (gameStatus !== 'active') {
      toast.error('Game is over!');
      return;
    }

    if (currentPlayerId !== currentUserId) {
      toast.error("It's not your turn! Please wait for your opponent.");
      return;
    }

    if (isApplyingMove) {
      toast.info('Move is being processed, please wait...');
      return;
    }

    // Additional check: ensure we have a valid session
    if (!sessionId) {
      toast.error('Game session not found');
      return;
    }

    // Check if user is actually a player in this game
    if (!playerIds.includes(currentUserId)) {
      console.error('Player validation failed:', {
        currentUserId,
        playerIds,
        sessionId,
        isIncluded: playerIds.includes(currentUserId)
      });
      toast.error('You are not a player in this game');
      return;
    }

    setIsApplyingMove(true);

    try {
      const { gameService } = await import('../../service/game.service');
      await gameService.applyMove(sessionId, {
        player_id: currentUserId,
        move: { column }
      });
      // The WebSocket will update the state when the move is applied
    } catch (error: any) {
      console.error('Failed to apply move:', error);
      // Backend sends error in 'error' field, not 'detail'
      const errorMessage = error?.response?.data?.error || error?.response?.data?.detail || error?.message || 'Failed to apply move';
      
      // Check if error is about turn validation
      if (errorMessage.toLowerCase().includes('turn') || errorMessage.toLowerCase().includes('not your turn')) {
        toast.error("It's not your turn! Please wait for your opponent.");
      } else {
        toast.error(errorMessage);
      }
    } finally {
      setIsApplyingMove(false);
    }
  }, [sessionId, currentUserId, currentPlayerId, gameStatus, isApplyingMove, playerIds]);

  const isMyTurn = currentPlayerId === currentUserId;
  const myPlayerNumber = getPlayerNumber(currentUserId);
  const currentPlayerNumber = getPlayerNumber(currentPlayerId);
  const [isQuitting, setIsQuitting] = useState(false);

  const handleQuit = useCallback(async () => {
    if (isQuitting) {
      return;
    }

    if (!confirm('Are you sure you want to quit the game? This will end the session.')) {
      return;
    }

    setIsQuitting(true);

    try {
      // First abandon the game session
      const { gameService } = await import('../../service/game.service');
      await gameService.abandonSession(sessionId, currentUserId);
      
      // Then leave the lobby if it exists
      if (lobbyId) {
        try {
          const { lobbyService } = await import('../../service/lobby.service');
          await lobbyService.leave(lobbyId);
          toast.success('Left the game and lobby');
        } catch (error: any) {
          // If error is "not in lobby", treat it as success (idempotent operation)
          const errorMessage = error?.response?.data?.message || error?.response?.data?.detail || error?.message || '';
          if (errorMessage.toLowerCase().includes("not in the lobby") || 
              errorMessage.toLowerCase().includes("player is not in")) {
            toast.success('Left the game and lobby');
          } else {
            // For other errors, still show success for leaving game, but log the lobby error
            console.warn('Failed to leave lobby (but game was abandoned):', error);
            toast.success('Left the game');
          }
        }
      } else {
        toast.success('Left the game');
      }
      
      if (onQuit) {
        onQuit();
      }
    } catch (error: any) {
      console.error('Failed to quit game:', error);
      toast.error(error?.response?.data?.detail || error?.message || 'Failed to quit game');
    } finally {
      setIsQuitting(false);
    }
  }, [sessionId, lobbyId, currentUserId, isQuitting, onQuit]);

  if (isLoading) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">Loading game...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2">Connect 4</h1>
          <p className="text-muted-foreground">Classic Connect Four game - Get 4 in a row to win!</p>
        </div>
        <Button
          onClick={handleQuit}
          disabled={isQuitting}
          variant="destructive"
          size="sm"
        >
          <LogOut className="w-4 h-4 mr-2" />
          {isQuitting ? 'Quitting...' : 'Quit Game'}
        </Button>
      </div>

      <div className="connect4-container">
        <div className="connect4-game">
          <div className="connect4-status">
            {!connected && (
              <p className="text-sm text-yellow-500 mb-2">Connecting to game...</p>
            )}
            {connected && gameStatus === 'active' && (
              <p className="text-lg font-semibold">
                {isMyTurn ? (
                  <span className="text-green-500">Your turn! (Player {myPlayerNumber})</span>
                ) : isAIPlayer(currentPlayerId) ? (
                  <span className="text-muted-foreground">
                    {isAIMakingMove ? 'AI is thinking...' : 'AI is making a move...'}
                  </span>
                ) : (
                  <span className="text-muted-foreground">
                    Waiting for Player {currentPlayerNumber}...
                  </span>
                )}
              </p>
            )}
            {gameStatus !== 'active' && (
              <div className="space-y-2">
                {winnerId ? (
                  <p className={`text-lg font-bold ${winnerId === currentUserId ? 'text-green-500' : 'text-red-500'}`}>
                    {winnerId === currentUserId ? 'You won!' : 'You lost!'}
                  </p>
                ) : (
                  <p className="text-lg font-bold text-primary">Draw game!</p>
                )}
              </div>
            )}
          </div>

          <div className="connect4-board-wrapper">
            <table className="connect4-board">
              <tbody>
                {board.map((row, i) => (
                  <Row 
                    key={i} 
                    row={row} 
                    play={isMyTurn && gameStatus === 'active' && !isApplyingMove ? play : () => {}}
                    disabled={!isMyTurn || gameStatus !== 'active' || isApplyingMove}
                  />
                ))}
              </tbody>
            </table>
          </div>

          {gameStatus !== 'active' && (
            <div className="mt-4 text-center">
              <p className="text-sm text-muted-foreground mb-2">Game Over</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

