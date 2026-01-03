import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import type { SendMessageRequest, ConversationParams } from '../../types/api.types';

import { QUERY_KEYS } from '../../config/api.config';
import { chatService } from '../../service/chat.service';

const { CHAT } = QUERY_KEYS

export const useConversations = () => {
  return useQuery({
    queryKey: CHAT.CONVERSATIONS,
    queryFn: () => chatService.getConversations(),
  });
};

export const useConversation = (friendId: string, params?: ConversationParams) => {
  return useQuery({
    queryKey: params ? [...CHAT.CONVERSATION(friendId), params] : CHAT.CONVERSATION(friendId),
    queryFn: () => chatService.getConversation(friendId, params),
    enabled: Boolean(friendId),
  });
};

export const useUnreadCount = () => {
  return useQuery({
    queryKey: CHAT.UNREAD_COUNT,
    queryFn: () => chatService.getUnreadCount(),
    refetchInterval: 30000,
  });
};

export const useSendMessage = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: SendMessageRequest) => chatService.sendMessage(data),
    onSuccess: (message, variables) => {
      // Optimistically update the conversation cache
      queryClient.setQueriesData(
        { queryKey: CHAT.CONVERSATION(variables.receiverId) },
        (old: PageableResponse<Message> | undefined) => {
          if (!old) return old;
          
          const existing = old.content ?? [];
          // Check if message already exists (from WebSocket)
          const existingMessage = existing.some((item) => item.id === message.id);
          if (existingMessage) return old;
          
          // Add the new message to the beginning (most recent first)
          return { 
            ...old, 
            content: [message, ...existing],
            totalElements: old.totalElements + 1
          };
        }
      );
      
      // Invalidate to ensure we have the latest data
      queryClient.invalidateQueries({ queryKey: CHAT.CONVERSATION(variables.receiverId) });
      queryClient.invalidateQueries({ queryKey: CHAT.CONVERSATIONS });
    },
  });
};

export const useMarkAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (messageId: string) => chatService.markAsRead(messageId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CHAT.UNREAD_COUNT })
  });
};
