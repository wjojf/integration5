import Cookies from "js-cookie"

import { STORAGE_CONFIG } from '../../config/storage.config'

const { CHATBOT } = STORAGE_CONFIG

export function setConversationCookie(conversationId: string): void {
    Cookies.set(CHATBOT.COOKIE_NAME, conversationId, {
        expires: new Date(Date.now() + CHATBOT.TTL_SECONDS * 1000),
        sameSite: "lax",
        secure: true,
        path: "/",
    })
}

export function getConversationCookie(): string | undefined {
    return Cookies.get(CHATBOT.COOKIE_NAME)
}
