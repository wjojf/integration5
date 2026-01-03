import { useQuery } from '@tanstack/react-query';

import type { PlayerSearchParams } from '../../types/api.types';

import { QUERY_KEYS } from '../../config/api.config';
import { playerService } from '../../service/player.service';

const { PLAYER } = QUERY_KEYS

export const useSearchPlayers = (params: PlayerSearchParams) => {
  return useQuery({
    queryKey: [...PLAYER.SEARCH, params],
    queryFn: () => playerService.searchPlayers(params)
  });
};

export const usePlayerById = (playerId: string | undefined) => {
  return useQuery({
    queryKey: playerId ? PLAYER.BY_ID(playerId) : ['player', 'by-id', 'disabled'],
    queryFn: () => playerService.getPlayerById(playerId!),
    enabled: Boolean(playerId),
  });
};
