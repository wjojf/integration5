import { InputComponents, DisplayComponents, LayoutComponents } from "../../components/app"
import { Badge } from '../../components/shared'
import { useModelMetrics, usePolicyPrediction, useWinProbability } from "../../hooks/analytics/useAnalytics"
import type { GameState } from "../../types/app.types"

const { Button } = InputComponents
const { Card, CardContent, CardHeader, CardTitle } = DisplayComponents
const { Tab, TabList, TabContent, TabTrigger } = LayoutComponents

export const Analytics = () => {
    const { data: metrics, isLoading: isLoadingMetrics } = useModelMetrics();
    const policyMutation = usePolicyPrediction();
    const winProbMutation = useWinProbability();

    // Mock simulation handlers
    const handleSimulatePrediction = () => {
        const mockState: GameState = {
            gameId: "sim-1",
            board: [],
            currentPlayer: "1"
        };
        policyMutation.mutate(mockState);
        winProbMutation.mutate(mockState);
    };

    return (
        <div className="space-y-6">
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="mb-2">Analytics</h1>
                    <p className="text-muted-foreground">View real-time model predictions and analytics</p>
                </div>
                <Button onClick={handleSimulatePrediction} disabled={policyMutation.isPending}>
                    {policyMutation.isPending ? "Analysing..." : "Simulate Live Game Update"}
                </Button>
            </div>

            <Tab defaultValue="metrics" className="space-y-4">
                <TabList>
                    <TabTrigger value="metrics">Model Metrics</TabTrigger>
                    <TabTrigger value="win-probability">Win Probability</TabTrigger>
                    <TabTrigger value="policy-imitation">Policy Imitation</TabTrigger>
                </TabList>

                <TabContent value="metrics" className="space-y-4">
                    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                        <Card>
                            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                                <CardTitle className="text-sm font-medium">Active Models</CardTitle>
                            </CardHeader>
                            <CardContent>
                                <div className="text-2xl font-bold">{metrics?.length || 0}</div>
                            </CardContent>
                        </Card>
                    </div>

                    <Card>
                        <CardHeader>
                            <CardTitle>Model Performance Registry</CardTitle>
                        </CardHeader>
                        <CardContent>
                            {isLoadingMetrics ? (
                                <div>Loading metrics...</div>
                            ) : (
                                <div className="space-y-4">
                                    {metrics?.map((metric: any) => (
                                        <div key={metric.modelName} className="flex items-center justify-between border-b pb-4 last:border-0 last:pb-0">
                                            <div>
                                                <p className="font-medium">{metric.modelName}</p>
                                                <p className="text-sm text-muted-foreground">Version: {metric.version}</p>
                                            </div>
                                            <div className="text-right">
                                                <div className="mb-1">
                                                    <Badge variant={metric.accuracy > 0.8 ? "default" : "secondary"}>
                                                        {(metric.accuracy * 100).toFixed(1)}% Accuracy
                                                    </Badge>
                                                </div>
                                                <p className="text-xs text-muted-foreground">
                                                    Trained on {metric.trainingGames.toLocaleString()} games
                                                </p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </CardContent>
                    </Card>
                </TabContent>

                <TabContent value="win-probability" className="space-y-4">
                    <div className="grid gap-4 md:grid-cols-2">
                        <Card>
                            <CardHeader>
                                <CardTitle>Live Win Probability</CardTitle>
                            </CardHeader>
                            <CardContent className="h-[300px] flex flex-col items-center justify-center">
                                {winProbMutation.data ? (
                                    <>
                                        <div className="text-5xl font-bold mb-4">
                                            {(winProbMutation.data.winProbability * 100).toFixed(1)}%
                                        </div>
                                        <p className="text-lg text-muted-foreground">
                                            Predicted Winner: <span className="text-foreground font-semibold">{winProbMutation.data.predictedWinner}</span>
                                        </p>
                                    </>
                                ) : (
                                    <p className="text-muted-foreground">Click "Simulate Live Game Update" to see data</p>
                                )}
                            </CardContent>
                        </Card>
                    </div>
                </TabContent>

                <TabContent value="policy-imitation" className="space-y-4">
                    <Card>
                        <CardHeader>
                            <CardTitle>Suggested Move</CardTitle>
                        </CardHeader>
                        <CardContent className="h-[300px] flex flex-col items-center justify-center">
                            {policyMutation.data ? (
                                <>
                                    <div className="text-4xl font-bold mb-2">
                                        Column {policyMutation.data.suggestedMove + 1}
                                    </div>
                                    <Badge className="mb-6">
                                        Confidence: {(policyMutation.data.confidence * 100).toFixed(1)}%
                                    </Badge>
                                    <div className="w-full max-w-xs space-y-2">
                                        <p className="text-xs text-muted-foreground text-center mb-2">Move Probability Distribution</p>
                                        <div className="flex items-end justify-between h-24 gap-1">
                                            {policyMutation.data.distribution.map((val: number, idx: number) => (
                                                <div key={idx} className="w-full bg-primary/20 hover:bg-primary/40 transition-colors rounded-t" style={{ height: `${val * 100}%` }} title={`Col ${idx + 1}: ${(val * 100).toFixed(0)}%`}></div>
                                            ))}
                                        </div>
                                    </div>
                                </>
                            ) : (
                                <p className="text-muted-foreground">Click "Simulate Live Game Update" to see data</p>
                            )}
                        </CardContent>
                    </Card>
                </TabContent>
            </Tab>
        </div>
    );
};
