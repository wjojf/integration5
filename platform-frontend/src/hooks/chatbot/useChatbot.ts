import { useMutation, useQuery, useQueryClient, skipToken } from "@tanstack/react-query"

import type { ChatRequest, ChatbotMessageResponse } from "../../types/api.types"

import { QUERY_KEYS } from "../../config/api.config"
import { chatbotService } from "../../service/chatbot.service"
import { getConversationCookie, setConversationCookie } from "../../lib/app/chat.storage"

const { CHATBOT } = QUERY_KEYS

export const useChatHistory = (options?: { enabled?: boolean }) => {
    const conversationId = getConversationCookie()

    return useQuery({
        queryKey: conversationId ? CHATBOT.HISTORY(conversationId) : ["chatbot", "history", "no-id"],
        queryFn: Boolean(conversationId) ? () => chatbotService.getChatMessages(conversationId as string) : skipToken,
        enabled: options?.enabled ?? true,
        staleTime: 30 * 60 * 1000,
        retry: 1,
    })
}

export const useChatbotMessage = () => {
    const queryClient = useQueryClient();
    const conversationId = getConversationCookie()

    return useMutation<ChatbotMessageResponse, unknown, ChatRequest>({
        mutationFn: (payload: ChatRequest) => {
            if (conversationId) {
                payload.conversation_id = conversationId;
            }
            return chatbotService.addMessage(payload)
        },
        onSuccess: async (response, variables) => {
            const newConversationId = response.conversation_id
            const oldConversationId = conversationId
            
            // Update conversation cookie
            setConversationCookie(newConversationId)
            
            // If conversation ID changed, remove old query data
            if (oldConversationId && oldConversationId !== newConversationId) {
                queryClient.removeQueries({ queryKey: CHATBOT.HISTORY(oldConversationId) })
            }
            
            // Optimistically update the cache with the new messages
            const queryKey = CHATBOT.HISTORY(newConversationId)
            const existingData = queryClient.getQueryData<{ conversation_id: string; history: Array<{ role: string; content: string }> }>(queryKey)
            
            // Add user message and bot response to history
            const newMessages = [
                ...(existingData?.history ?? []),
                { role: "user" as const, content: variables.message },
                { role: "assistant" as const, content: response.response }
            ]
            
            // Update cache optimistically
            queryClient.setQueryData(queryKey, {
                conversation_id: newConversationId,
                history: newMessages
            })
            
            // Refetch in background to ensure we have the latest from server (but don't wait)
            queryClient.invalidateQueries({ queryKey })
            queryClient.refetchQueries({ queryKey })
        }
    })
}
