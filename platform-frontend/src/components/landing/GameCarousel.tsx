import {useEffect, useState} from "react";
import {ChevronLeft, ChevronRight} from "lucide-react";
import { useKeycloak } from "@react-keycloak/web"

import type {GamesResponse} from '../../types/api.types'
import type {Game} from '../../types/app.types'
import { Badge } from '../shared'
import { useGetAllGames } from '../../hooks/game/useGame'
import {Image} from "../app/display/Image";

export const GamesCarousel = () => {
    const [currentIndex, setCurrentIndex] = useState(0);
    const { data: gamesResponse = {} as GamesResponse } = useGetAllGames()
    const { games = [] } = gamesResponse

    useEffect(() => {
        if (games.length <= 1) return;

        const interval = setInterval(() => setCurrentIndex((prev) => (prev + 1) % games.length), 5000);

        return () => clearInterval(interval);
    }, [games.length]);

    const goToPrevious = () => games.length !== 0 && setCurrentIndex((prev) => (prev - 1 + games.length) % games.length)
    const goToNext = () => games.length !== 0 && setCurrentIndex((prev) => (prev + 1) % games.length);

    return (
        <div className="relative">
            <div className="overflow-hidden rounded-xl border border-border bg-card/60">
                <div className="flex transition-transform duration-500 ease-out"
                     style={{ transform: `translateX(-${currentIndex * 100}%)` }}>
                    {games.map((game, index) => (
                        <div key={index} className="w-full flex-shrink-0">
                            <GameCard game={game} />
                        </div>
                    ))}
                </div>
            </div>

            <button
                onClick={goToPrevious}
                className="absolute left-4 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full border border-border bg-card/90 backdrop-blur flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-card transition-all shadow-lg"
            >
                <ChevronLeft className="h-5 w-5" />
            </button>
            <button
                onClick={goToNext}
                className="absolute right-4 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full border border-border bg-card/90 backdrop-blur flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-card transition-all shadow-lg"
            >
                <ChevronRight className="h-5 w-5" />
            </button>

            <div className="flex items-center justify-center gap-2 mt-6">
                {games.map((item, index) => (
                    <button
                        key={index}
                        onClick={() => setCurrentIndex(index)}
                        className={`h-2 rounded-full transition-all ${index === currentIndex ? "w-8 bg-primary" : "w-2 bg-border hover:bg-border/60"
                        }`}
                    />
                ))}
            </div>
        </div>
    );
}

function GameCard({ game }: { game: Game }) {
    const { keycloak } = useKeycloak()

    return (
        <div className="flex flex-col md:flex-row items-center gap-8 p-8 md:p-12">
            <div className="w-full md:w-1/2 aspect-video rounded-lg bg-gradient-to-br border border-border/40 overflow-hidden">
                <Image
                    src={game.image}
                    alt={game.title ?? "Game image"}
                    className="h-full w-full object-contain"
                />
            </div>
            <div className="w-full md:w-1/2 text-center md:text-left">
                <h3 className="text-2xl mb-3">{game.title}</h3>
                <Badge className={`bg-blue-500/20 text-blue-400 uppercase border-0 text-xs rounded px-2 py-1 mb-3`}>
                    {game.genre}
                </Badge>
                <p className="text-sm text-muted-foreground mb-6 max-w-md mx-auto md:mx-0">
                    Join thousands of players in this exciting game. Quick to learn, challenging to master, and always
                    fun to play.
                </p>
                <button
                    onClick={() => keycloak.login({ redirectUri: `${window.location.origin}/app` })}
                    className="inline-flex items-center rounded-full bg-primary px-6 py-2.5 text-sm text-primary-foreground shadow-md shadow-black/40 hover:bg-primary/90 transition-colors">
                    Play now
                </button>
            </div>
        </div>
    );
}
