import { MessageCircle, Trophy, Bot, Gamepad2 } from "lucide-react"
import { useKeycloak } from "@react-keycloak/web"

import { Icon } from "../../components/shared"
import { GamesCarousel } from "../../components/landing/GameCarousel"
import { FeatureCard } from "../../components/landing/FeatureCard"
import iconImg from "../../assets/icon.png";

export const Landing = () => {
    const { keycloak } = useKeycloak()

    if (keycloak.authenticated) {
        window.location.href = `${window.location.origin}/app/games`
    }

    return (
        <div className="min-h-screen bg-background text-foreground flex flex-col">
            <header className="header border-b border-border/60">
                <div className="header__left">
                    <a href="#" className="logo group">
                        <div className="logo__mark group-hover:scale-105 transition-transform">
                            <Icon src={iconImg} imageAlt={'BanditGames Icons'} className="rounded" />
                        </div>

                        <div className="flex flex-col gap-0.5">
                            <span className="logo__text tracking-[0.18em] uppercase">
                                BanditGames
                            </span>
                            <span className="text-[0.7rem] text-muted-foreground">
                                Your Gaming Platform
                            </span>
                        </div>
                    </a>
                </div>

                <div className="header__right">
                    <button
                        onClick={() => keycloak.login({ redirectUri: `${window.location.origin}/app` })}
                        className="hidden md:inline-flex items-center rounded-full border border-border bg-secondary/60 px-3.5 py-1.5 text-xs text-muted-foreground hover:bg-secondary hover:text-foreground transition-colors">
                        Log in
                    </button>
                </div>
            </header>

            <main className="flex-1">
                <section
                    className="flex flex-col items-center relative mx-auto max-w-5xl px-4 py-16 md:py-24 text-center">
                    <div className="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
                        <div
                            className="absolute left-1/2 top-0 h-96 w-96 -translate-x-1/2 rounded-full bg-primary/20 blur-3xl" />
                    </div>

                    <div
                        className="inline-flex items-center gap-2 rounded-full border border-border bg-secondary/60 px-3 py-1 text-s text-muted-foreground backdrop-blur mb-6">
                        <span className="inline-flex h-2.5 w-2.5 rounded-full bg-emerald-400 animate-pulse" />
                        <span>Join 10K+ players online</span>
                    </div>

                    <h1 className="text-4xl sm:text-5xl md:text-6xl tracking-tight max-w-3xl mx-auto">
                        Your favorite board games, <span className="block text-primary">always ready to play</span>
                    </h1>

                    <div className="mt-6 text-base sm:text-lg text-muted-foreground max-w-2xl mx-auto">
                        Play instantly in your browser. No downloads, no waiting.
                        Challenge friends or match with players worldwide.
                    </div>

                    <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
                        <button
                            onClick={() => keycloak.login({ redirectUri: `${window.location.origin}/app` })}
                            className="inline-flex items-center rounded-full bg-primary px-6 py-3 text-sm text-primary-foreground shadow-lg shadow-primary/25 hover:bg-primary/90 transition-all hover:shadow-xl hover:shadow-primary/30">
                            Start playing now
                        </button>
                    </div>

                    <div className="mt-12 grid grid-cols-3 gap-35 max-w-2xl mx-auto text-sm">
                        <div>
                            <div className="text-2xl text-primary">50+</div>
                            <div className="text-muted-foreground mt-1">Games</div>
                        </div>
                        <div>
                            <div className="text-2xl text-primary">10k+</div>
                            <div className="text-muted-foreground mt-1">Players</div>
                        </div>
                        <div>
                            <div className="text-2xl text-primary">24/7</div>
                            <div className="text-muted-foreground mt-1">Active</div>
                        </div>
                    </div>
                </section>

                <section className="border-y border-border/60 bg-slate-950/30 py-16">
                    <div className="mx-auto max-w-6xl px-4">
                        <div className="flex flex-col items-center mb-8">
                            <h2 className="text-2xl sm:text-3xl">Everything you need to play</h2>
                            <p className="mt-3 text-sm text-muted-foreground max-w-2xl mx-auto">
                                A complete gaming platform built for board game enthusiasts
                            </p>
                        </div>

                        <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-5">
                            <FeatureCard
                                icon={<MessageCircle className="h-6 w-6" />}
                                title="Chat with friends"
                                description="Voice and text chat built right into every game. Stay connected while you play."
                            />
                            <FeatureCard
                                icon={<Trophy className="h-6 w-6" />}
                                title="Achievements"
                                description="Unlock rewards, track your progress, and compete on global leaderboards."
                            />
                            <FeatureCard
                                icon={<Bot className="h-6 w-6" />}
                                title="Intelligent AI bots"
                                description="Practice against adaptive AI opponents that match your skill level."
                            />
                            <FeatureCard
                                icon={<Gamepad2 className="h-6 w-6" />}
                                title="Big collection"
                                description="Access 50+ classic and modern board games, with new titles added weekly."
                            />
                        </div>
                    </div>
                </section>

                <section className="py-16 md:py-20">
                    <div className="mx-auto max-w-6xl px-4">
                        <div className="text-center mb-12">
                            <h2 className="text-2xl sm:text-3xl">Play free games right now</h2>
                            <p className="mt-3 text-sm text-muted-foreground">
                                Start with our collection of free-to-play titles
                            </p>
                        </div>

                        <GamesCarousel />
                    </div>
                </section>
            </main>

            <footer className="border-t border-border/60 bg-slate-950/60 pt-3">
                <div className="mx-auto max-w-6xl px-4 text-center">
                    <p className="text-xs text-muted-foreground">
                        © {new Date().getFullYear()} BanditGames. All rights reserved.
                    </p>
                </div>
            </footer>
        </div>
    );
}
