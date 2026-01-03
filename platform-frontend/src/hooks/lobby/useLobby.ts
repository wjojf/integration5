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
    onSuccess: () => [LOBBY.ALL, LOBBY.CURRENT_PLAYER].forEach(queryKey => queryClient.invalidateQueries({ queryKey })),
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
    queryFn: () => lobbyService.getExternalGameInstance(lobbyId),
    enabled: enabled && Boolean(lobbyId),
    refetchInterval: (data) => {
      // Poll every 2 seconds if external game instance hasn't been created yet
      return data?.hasExternalGameInstance ? false : 2000;
    },
  });
};;
