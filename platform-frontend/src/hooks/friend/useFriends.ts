import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

import {ModifyFriendRequest, SendFriendRequestPayload} from '../../types/api.types';

import {QUERY_KEYS} from '../../config/api.config';
import {friendsService} from '../../service/friend.service';
import {playerService} from '../../service/player.service';

const { FRIENDS,FRIENDSHIPS } = QUERY_KEYS

export const useAllFriendships = () => {
  return useQuery({
    queryKey: FRIENDSHIPS.ALL,
    queryFn: async () => {
      try {
        const response = await friendsService.getAll();
        // Backend returns PageableResponse<FriendInfoResponse>
        // FriendInfoResponse only has the "other" user, not both requesterId and addresseeId
        // We'll need to handle this differently in components that use this data
        if (Array.isArray(response)) {
          return response;
        }
        const content = response?.content || [];
        // Return the FriendInfoResponse as-is, components will need to adapt
        // For now, we'll create a minimal Friendship object for compatibility
        return content.map((item: any) => ({
          id: item.friendshipId || item.id,
          // Note: FriendInfoResponse doesn't have requesterId/addresseeId directly
          // The player field contains the "other" user's info
          // We'll use a placeholder that components can work with
          requesterId: '', // Will be determined by component logic
          addresseeId: item.player?.playerId || '',
          status: item.status,
          createdAt: item.createdAt,
          updatedAt: item.updatedAt,
          // Store the full response for components that need it
          _friendInfo: item,
        }));
      } catch (error: any) {
        // Handle 503 Service Unavailable
        if (error?.response?.status === 503) {
          console.error('Backend service unavailable:', error);
          return [];
        }
        throw error;
      }
    },
  });
};

export const useFriendshipsByStatus = (status?: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'BLOCKED', page: number = 0, size: number = 20) => {
  return useQuery({
    queryKey: [...FRIENDSHIPS.ALL, status, page, size],
    queryFn: async () => {
      try {
        const response = await friendsService.getAll(status);
        // Backend returns PageableResponse<FriendInfoResponse>
        if (Array.isArray(response)) {
          return {
            content: response,
            totalElements: response.length,
            totalPages: 1,
            number: page,
            size: response.length,
          };
        }
        return response;
      } catch (error: any) {
        // Handle 503 Service Unavailable
        if (error?.response?.status === 503) {
          console.error('Backend service unavailable:', error);
          return {
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: page,
            size: 0,
          };
        }
        throw error;
      }
    },
    enabled: true, // Always enabled, status can be undefined to get all
  });
};

export const useFriends = () => {
  return useQuery({
    queryKey: FRIENDS.ALL,
    queryFn: () => playerService.getPlayerFriends(),
  });
}

export const useSendFriendRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: SendFriendRequestPayload) => friendsService.sendRequest(data),
    onSuccess: () => [FRIENDSHIPS.ALL, FRIENDSHIPS.PENDING].forEach(queryKey => queryClient.invalidateQueries({ queryKey }))
  });
};

export const useModifyFriendRequest = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: ModifyFriendRequest) =>
      friendsService.modifyRequest(id, data),
    onSuccess: () => {
      // Invalidate all friendship-related queries
      queryClient.invalidateQueries({ queryKey: FRIENDSHIPS.ALL });
      queryClient.invalidateQueries({ queryKey: FRIENDSHIPS.PENDING });
      queryClient.invalidateQueries({ queryKey: FRIENDSHIPS.ACCEPTED });
      queryClient.invalidateQueries({ queryKey: FRIENDS.ALL });
    }
  });
};
