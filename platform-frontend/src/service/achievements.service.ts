import { ApiService } from './api.service'
import { API_ENDPOINTS } from '../config/api.config'
import type { Achievement, UserUnlockedAchievement } from '../types/app.types'

const { ACHIEVEMENTS } = API_ENDPOINTS

class AchievementsService extends ApiService {
    async getAllAchievements(){
        return this.get<Achievement[]>(ACHIEVEMENTS.ALL)
    }

    async getUserAchievements(userId: string){
        return this.get<UserUnlockedAchievement[]>(ACHIEVEMENTS.BY_USER__ID(userId))
    }
}

export const achievementsService = new AchievementsService();
