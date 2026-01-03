import { ApiService } from './api.service';
import { API_ENDPOINTS } from '../config/api.config';

import type { ChatRequest, ChatbotMessageResponse  } from '../types/api.types';
import type { ChatMessages  } from '../types/app.types';

const { CHATBOT } = API_ENDPOINTS

class ChatbotService extends ApiService {

    async addMessage(payload: ChatRequest): Promise<ChatbotMessageResponse> {
        return this.post<ChatbotMessageResponse>(CHATBOT.CREATE_CHAT, payload)
    }

    async getChatMessages(conversationId: string): Promise<ChatMessages> {
        return this.get<ChatMessages>(CHATBOT.BY_ID(conversationId))
    }
}

export const chatbotService = new ChatbotService()
