import { ApiService } from './api.service';
import { API_ENDPOINTS, API_CONFIG } from '../config/api.config';
import type { SendMessageRequest, ConversationParams, PageableResponse } from '../types/api.types';
import type { Message } from '../types/app.types';

const { PAGINATION } = API_CONFIG
const { CHAT } = API_ENDPOINTS

class ChatService extends ApiService {

  async getConversations(): Promise<string[]> {
    return this.get<string[]>(CHAT.CONVERSATIONS)
  }

  async getConversation(friendId: string, params?: ConversationParams): Promise<PageableResponse<Message>> {
    const requestParams = params || { page: PAGINATION.DEFAULT_PAGE, size: PAGINATION.DEFAULT_SIZE }

    return this.get<PageableResponse<Message>>(CHAT.CONVERSATION(friendId), requestParams as Record<string, unknown>)
  }

  async getUnreadCount(): Promise<number> {
    return this.get<number>(CHAT.UNREAD_COUNT)
  }

  async sendMessage(data: SendMessageRequest): Promise<Message> {
    return this.post<Message>(CHAT.MESSAGES, data)
  }

  async markAsRead(messageId: string): Promise<Message> {
    return this.patch<Message>(CHAT.MARK_READ(messageId))
  }
}

export const chatService = new ChatService();
