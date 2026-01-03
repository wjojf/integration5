import type { LucideIcon } from "lucide-react"
import {
    BrainCircuit, Gamepad2, MessageSquare, Search, Trophy, User, Users, Star, Sparkles, Gem, Crown, Award
} from "lucide-react"


export type NavItem = (typeof NAV_ITEMS)[number];
export type NavItemId = NavItem["id"];

type NavItemProps = { id: string, label: string, icon: LucideIcon }
export const NAV_ITEMS = [
    {id: "games", label: "Games", icon: Gamepad2},
    {id: "lobbies", label: "Lobbies", icon: Search},
    {id: "players", label: "Players", icon: Users},
    {id: "chat", label: "Chat", icon: MessageSquare},
    {id: "achievements", label: "Achievements", icon: Trophy},
    {id: "profile", label: "Profile", icon: User},
    {id: "analytics", label: "Analytics", icon: BrainCircuit},
] as const satisfies ReadonlyArray<NavItemProps>


type AchievementIconProps = { label: string, icon: LucideIcon, priority: number }

export const ACHIEVEMENT_ICONS = [
    { label: "COMMON", icon: Star, priority: 1 },
    { label: "UNCOMMON", icon: Sparkles, priority: 2 },
    { label: "RARE", icon: Gem, priority: 3 },
    { label: "EPIC", icon: Crown, priority: 4 },
    { label: "LEGENDARY", icon: Award, priority: 5 },
] as const satisfies ReadonlyArray<AchievementIconProps>

export const ACHIEVEMENT_RARITY_COLOR = {
    COMMON: "bg-gray-500",
    UNCOMMON: "bg-green-500",
    RARE: "bg-blue-500",
    EPIC: "bg-purple-500",
    LEGENDARY: "bg-yellow-500",
} as const satisfies Readonly<Record<string, string>>

export interface UserAchievement {
    achievementId: string,
    gameId?: string
    name: string,
    description: string,
    category: "PROGRESSION" | "TIME" | "DIFFICULTY" | "SOCIAL"
    rarity: "COMMON" | "UNCOMMON" | "RARE" | "EPIC" | "LEGENDARY"
    unlockedAt?: string,
    icon?: LucideIcon,
    priority?: number
}


export const USER_RARITY_COLOR = {
    BRONZE: "bg-orange-900/20 text-orange-400",
    SILVER: "bg-gray-400/20 text-gray-300",
    GOLD: "bg-yellow-500/20 text-yellow-400",
    PLATINUM: "bg-cyan-500/20 text-cyan-400",
    DIAMOND: "bg-blue-500/20 text-blue-400"
} as const satisfies Readonly<Record<string, string>>
