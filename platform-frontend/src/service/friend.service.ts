import { ApiService } from './api.service'
import { API_ENDPOINTS } from '../config/api.config'
import type { SendFriendRequestPayload, ModifyFriendRequestPayload, PageableResponse } from '../types/api.types'
import {Friendship } from '../types/app.types'

const { FRIENDS } = API_ENDPOINTS

class FriendService extends ApiService {

  async getAll(status?: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'BLOCKED'): Promise<PageableResponse<any>> {
    const params = status ? { status } : {};
    return this.get<PageableResponse<any>>(FRIENDS.ALL, params);
  }

  async sendRequest(data: SendFriendRequestPayload): Promise<Friendship> {
    return this.post<Friendship>(FRIENDS.SEND_REQUEST, data);
  }

  async modifyRequest(id: string, data: ModifyFriendRequestPayload): Promise<Friendship> {
    return this.patch<Friendship>(FRIENDS.BY_ID(id), data);
  }
}

export const friendsService = new FriendService();
