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
        onSuccess: async (response) => {
            const newConversationId = response.conversation_id
            setConversationCookie(newConversationId)
            // Invalidate and refetch the conversation history
            await queryClient.invalidateQueries({ queryKey: CHATBOT.HISTORY(newConversationId) })
            // Refetch to ensure we have the latest messages
            await queryClient.refetchQueries({ queryKey: CHATBOT.HISTORY(newConversationId) })
        }
    })
}
