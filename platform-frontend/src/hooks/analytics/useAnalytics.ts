import { useQuery, useMutation } from '@tanstack/react-query';
import { mlService } from '../../service/ml.service';
import type { GameState } from '../../types/app.types';

export const usePolicyPrediction = () => {
    return useMutation({
        mutationFn: (gameState: GameState) => mlService.getPolicyPrediction(gameState),
    });
};

export const useWinProbability = () => {
    return useMutation({
        mutationFn: (gameState: GameState) => mlService.getWinProbability(gameState),
    });
};

export const useModelMetrics = () => {
    return useQuery({
        queryKey: ['ml-metrics'],
        queryFn: () => mlService.getModelMetrics(),
    });
};
