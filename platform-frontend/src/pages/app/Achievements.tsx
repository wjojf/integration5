import { Trophy, Star, Crown, History } from "lucide-react"
import {useKeycloak} from "@react-keycloak/web"

import type {Game} from "../../types/app.types"
import { DisplayComponents } from '../../components/app'
import { Badge } from '../../components/shared'
import { ACHIEVEMENT_RARITY_COLOR, ACHIEVEMENT_ICONS, UserAchievement } from '../../config/app.config'
import { useAllAchievements, useGetUserAchievements } from '../../hooks/achievements/useAchievements'
import { formatDate } from '../../utils'
import { useGetAllGames } from '../../hooks/game/useGame'

const { Card } = DisplayComponents

export const Achievements = () => {
  const {keycloak} = useKeycloak()
  const userId = keycloak.tokenParsed?.sub as string
  const { data: achievements = [] } = useAllAchievements()
  const { data: gamesResponse = { games: [] }  } = useGetAllGames()
  const userAchievements = useGetUserAchievements(userId)

  const unlockedAchievements = userAchievements.filter((item) => item.unlockedAt)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="mb-2">Achievements</h1>
        <p className="text-muted-foreground">Track your gaming milestones and progress</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card className="p-6 bg-card border-border">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-full bg-primary/10">
              <Trophy className="w-6 h-6 text-primary" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Unlocked</p>
              <p className="text-2xl">{unlockedAchievements.length}/{achievements.length}</p>
            </div>
          </div>
        </Card>

        <Card className="p-6 bg-card border-border">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-full bg-purple-500/10">
              <Crown className="w-6 h-6 text-purple-500" />
            </div>
            <div>
              <p className="text-sm text-muted-foreground">Completion</p>
              <p className="text-2xl">{Math.round((unlockedAchievements.length / achievements.length) * 100) || 0}%</p>
            </div>
          </div>
        </Card>
      </div>


      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {AttachmentItems(userAchievements, gamesResponse?.games)}
      </div>
    </div>
  );
}

const AttachmentItems = (achievements: UserAchievement[], games: Game[]) => {
  const sortedAchievements = achievements
      .map(item => {
        const iconDetails = ACHIEVEMENT_ICONS.find(icon => icon.label === item.rarity)
        return { ...item, icon: iconDetails?.icon || Star, priority: iconDetails?.priority || 0 }
      })
      .sort((a, b) => a.priority - b.priority)

  return sortedAchievements.map((achievement) => {
    const achievementGame = games.find(item => String(item.id) === achievement.gameId)
    const AchievementIcon = achievement.icon
    return (
      <Card
        key={achievement.achievementId}
        className={`p-6 bg-card border-border transition-all ${achievement.unlockedAt ? "border-l-4 border-l-primary" : "opacity-45"
          }`}
      >
        <div className="flex gap-4">
          <div
            className={`w-14 h-14 flex items-center justify-center rounded-full ${achievement.unlockedAt
              ? "bg-primary/10 text-primary"
              : "bg-muted text-muted-foreground"
              }`}
          >
          < AchievementIcon />
          </div>

          <div className="flex-1 space-y-3">
            <div className="flex justify-between items-start">
              <div>
                <h3 className="text-base leading-tight">{achievement.name}</h3>
                <p className="text-sm text-muted-foreground mt-1">
                  {achievement.description}
                </p>
              </div>

              <Badge className={`${ACHIEVEMENT_RARITY_COLOR[achievement.rarity]} text-white uppercase border-0 text-xs rounded px-2 py-1`}>
                {achievement.rarity}
              </Badge>
            </div>

            <div className="flex items-center">
              <Badge
                  className={`flex items-center gap-1 text-xs px-2 py-1 rounded-full bg-muted text-muted-foreground`}>
                {achievementGame ? <span>{achievementGame.title}</span> : <span>BanditGames</span> }
              </Badge>
            </div>
            {
              achievement.unlockedAt
                  ? (
                      <Badge variant="secondary" className="gap-1 rounded-full px-2 py-1 text-xs text-muted-foreground">
                        <History className="h-3.5 w-3.5" aria-hidden="true" />
                        <span className="tabular-nums">{formatDate(achievement.unlockedAt)}</span>
                      </Badge>
                  )
                  : null
            }
          </div>
        </div>
      </Card>
    );
  });
}
