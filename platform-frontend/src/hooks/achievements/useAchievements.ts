import { useQuery } from '@tanstack/react-query';

import { QUERY_KEYS } from '../../config/api.config';
import { achievementsService } from '../../service/achievements.service';

const { ACHIEVEMENTS } = QUERY_KEYS

export const useAllAchievements = () =>
    useQuery({
        queryKey: ACHIEVEMENTS.ALL,
        queryFn: () => achievementsService.getAllAchievements(),
        staleTime: 5 * 60_000
    })

export const useUserUnlockedAchievement = (userId: string) =>
    useQuery({
        queryKey: ACHIEVEMENTS.BY_USER_ID(userId),
        enabled: Boolean(userId),
        queryFn: () => achievementsService.getUserAchievements(userId!),
        staleTime: 5 * 60_000
    })

export const useGetUserAchievements = (userId: string) => {
    const { data: achievements = [] } = useAllAchievements()
    const { data: userAchievements = [] } = useUserUnlockedAchievement(userId)

    return achievements.map(achievement  => {
        const { id: achievementId, gameId, name, description, category, rarity } = achievement
        const unlockedAchievement = userAchievements.find(item => item.achievementId === achievementId)

        return {
            achievementId, gameId, name, description, category, rarity,
            unlockedAt: unlockedAchievement?.unlockedAt
        }
    })
}
