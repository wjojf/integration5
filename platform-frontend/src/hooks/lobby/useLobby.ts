import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import type {
  CreateLobbyRequest, UpdateLobbyRequest, StartLobbyRequest, InviteToLobbyRequest, LobbySearchParams
} from '../../types/api.types';

import { QUERY_KEYS } from '../../config/api.config';
import { lobbyService } from '../../service/lobby.service';

const { LOBBY } = QUERY_KEYS

export const useAllLobbies = (params?: LobbySearchParams) => {
  return useQuery({
    queryKey: params ? [...LOBBY.ALL, params] : LOBBY.ALL,
    queryFn: () => lobbyService.getAll(params),
  });
};

export const useCurrentPlayerLobby = () => {
  return useQuery({
    queryKey: LOBBY.CURRENT_PLAYER,
    queryFn: async () => {
      try {
        return await lobbyService.getCurrentPlayerLobby();
      } catch (error: any) {
        // Return undefined for 404 (no lobby found), throw other errors
        if (error?.response?.status === 404) {
          return undefined;
        }
        throw error;
      }
    },
    retry: false,
  });
};

export const useLobbyById = (lobbyId: string) => {
  return useQuery({
    queryKey: LOBBY.BY_ID(lobbyId),
    queryFn: () => lobbyService.getByLobbyId(lobbyId),
    enabled: Boolean(lobbyId),
  });
};

export const useCreateLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateLobbyRequest) => lobbyService.create(data),
    onSuccess: () => [LOBBY.ALL, LOBBY.CURRENT_PLAYER].forEach(queryKey => queryClient.invalidateQueries({ queryKey }))
  });
};

export const useJoinLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (lobbyId: string) => lobbyService.join(lobbyId),
    onSuccess: () => [LOBBY.ALL, LOBBY.CURRENT_PLAYER].forEach(queryKey => queryClient.invalidateQueries({ queryKey }))
  });
};

export const useStartLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ lobbyId, data }: { lobbyId: string; data: StartLobbyRequest }) => lobbyService.start(lobbyId, data),
    onSuccess: (data, variables) => {
      // Invalidate all lobby queries to refresh data
      [LOBBY.ALL, LOBBY.CURRENT_PLAYER, LOBBY.BY_ID(variables.lobbyId)].forEach(queryKey => 
        queryClient.invalidateQueries({ queryKey })
      );
    },
  });
};

export const useReadyLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (lobbyId: string) => lobbyService.ready(lobbyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.LOBBY.CURRENT_PLAYER }),
  });
};

export const useInviteToLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ lobbyId, data }: { lobbyId: string; data: InviteToLobbyRequest }) =>
        lobbyService.invite(lobbyId, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: QUERY_KEYS.LOBBY.CURRENT_PLAYER }),
  });
};

export const useLeaveLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (lobbyId: string) => lobbyService.leave(lobbyId),
    onSuccess: () => {
      // Invalidate and refetch to ensure UI is updated
      queryClient.invalidateQueries({ queryKey: LOBBY.ALL });
      queryClient.invalidateQueries({ queryKey: LOBBY.CURRENT_PLAYER });
      // Also remove the current lobby from cache to force refetch
      queryClient.removeQueries({ queryKey: LOBBY.CURRENT_PLAYER });
    },
    onError: (error: any) => {
      // Even on error, clear the cache if it's a "not in lobby" error
      // This handles cases where the player is already not in the lobby
      const errorMessage = error?.response?.data?.message || error?.message || '';
      if (errorMessage.toLowerCase().includes("not in the lobby") || 
          errorMessage.toLowerCase().includes("player is not in")) {
        queryClient.invalidateQueries({ queryKey: LOBBY.ALL });
        queryClient.invalidateQueries({ queryKey: LOBBY.CURRENT_PLAYER });
        queryClient.removeQueries({ queryKey: LOBBY.CURRENT_PLAYER });
      }
    }
  });
};

export const useUpdateLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ lobbyId, data }: { lobbyId: string; data: UpdateLobbyRequest }) =>
        lobbyService.update(lobbyId, data),
    onSuccess: () => [LOBBY.ALL, LOBBY.CURRENT_PLAYER].forEach(queryKey => queryClient.invalidateQueries({ queryKey })),
  });
};

export const useCancelLobby = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (lobbyId: string) => lobbyService.cancel(lobbyId),
    onSuccess: () => [LOBBY.ALL, LOBBY.CURRENT_PLAYER].forEach(queryKey => queryClient.invalidateQueries({ queryKey }))
  });
};

export const useExternalGameInstance = (lobbyId: string, enabled: boolean = true) => {
  return useQuery({
    queryKey: [...LOBBY.BY_ID(lobbyId), 'external-game-instance'],
    queryFn: async () => {
      console.log(`[useExternalGameInstance] Fetching external game instance for lobby ${lobbyId}`)
      try {
        const result = await lobbyService.getExternalGameInstance(lobbyId)
        console.log(`[useExternalGameInstance] Result:`, result)
        // Ensure we always return a valid object, even if the API returns null/undefined
        if (!result) {
          console.warn(`[useExternalGameInstance] API returned null/undefined, returning default object`)
          return {
            lobbyId,
            gameId: null,
            externalGameInstanceId: null,
            hasExternalGameInstance: false
          }
        }
        return result
      } catch (error: any) {
        console.error(`[useExternalGameInstance] Error fetching:`, error)
        // Return a default object on error instead of throwing, so polling can continue
        return {
          lobbyId,
          gameId: null,
          externalGameInstanceId: null,
          hasExternalGameInstance: false
        }
      }
    },
    enabled: enabled && Boolean(lobbyId),
    retry: false, // Don't retry on error, just return the default object
    refetchInterval: (data) => {
      // Poll every 2 seconds if external game instance hasn't been created yet
      const shouldPoll = !data?.hasExternalGameInstance
      if (shouldPoll) {
        console.log(`[useExternalGameInstance] Polling for external game instance (lobbyId: ${lobbyId}, hasInstance: ${data?.hasExternalGameInstance}, data:`, data)
      } else {
        console.log(`[useExternalGameInstance] External game instance found, stopping polling:`, data)
      }
      return shouldPoll ? 2000 : false
    },
  });
};;
