import { ApiService } from './api.service';
import type { ModelMetric, PolicyPrediction, WinProbabilityPrediction, GameState } from '../types/app.types';

export class MLApiService extends ApiService {
    private static instance: MLApiService;

    private constructor() {
        super();
    }

    public static getInstance(): MLApiService {
        if (!MLApiService.instance) {
            MLApiService.instance = new MLApiService();
        }
        return MLApiService.instance;
    }

    public async getPolicyPrediction(gameState: GameState): Promise<PolicyPrediction> {
        // TODO: Replace with actual API call
        // return this.post<PolicyPrediction>('/ml/predict/policy', { gameState });

        // Mock data for now
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    suggestedMove: Math.floor(Math.random() * 7),
                    confidence: 0.85 + Math.random() * 0.1,
                    distribution: Array(7).fill(0).map(() => Math.random())
                });
            }, 500);
        });
    }

    public async getWinProbability(gameState: GameState): Promise<WinProbabilityPrediction> {
        // TODO: Replace with actual API call
        // return this.post<WinProbabilityPrediction>('/ml/predict/win-probability', { gameState });

        // Mock data for now
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve({
                    winProbability: Math.random(),
                    predictedWinner: Math.random() > 0.5 ? 'Player 1' : 'Player 2'
                });
            }, 500);
        });
    }

    public async getModelMetrics(): Promise<ModelMetric[]> {
        // TODO: Replace with actual API call
        // return this.get<ModelMetric[]>('/ml/metrics');

        // Mock data for now
        return new Promise((resolve) => {
            setTimeout(() => {
                resolve([
                    {
                        modelName: "Connect4-Policy-v1",
                        version: "1.0.2",
                        accuracy: 0.78,
                        lastUpdated: "2024-12-25T10:00:00Z",
                        trainingGames: 15600
                    },
                    {
                        modelName: "Connect4-WinProb-v1",
                        version: "1.0.0",
                        accuracy: 0.82,
                        lastUpdated: "2024-12-24T18:30:00Z",
                        trainingGames: 12400
                    }
                ]);
            }, 800);
        });
    }
}

export const mlService = MLApiService.getInstance();
