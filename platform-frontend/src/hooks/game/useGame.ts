import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '../../config/api.config';
import { gameService } from '../../service/game.service';

const { GAME } = QUERY_KEYS

export const useGetAllGames = () => {
    return useQuery({
        queryKey: GAME.ALL,
        queryFn: () => gameService.getAllGames()
    })
}
