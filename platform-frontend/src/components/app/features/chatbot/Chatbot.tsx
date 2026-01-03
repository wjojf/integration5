import React, {JSX} from "react"
import {Dialog} from "radix-ui"
import { toast } from "sonner"
import {Bot, Send, X, Loader2} from "lucide-react"
import {useKeycloak} from "@react-keycloak/web"

import type {ChatRequest} from "../../../../types/api.types"
import {useChatHistory, useChatbotMessage} from "../../../../hooks/chatbot/useChatbot"
import {getConversationCookie} from "../../../../lib/app/chat.storage"

export function Chatbot(): JSX.Element {
    const {keycloak} = useKeycloak()
    const [open, setOpen] = React.useState(false);
    const [input, setInput] = React.useState("");

    const userId = keycloak.tokenParsed?.sub as string
    const {data: chatHistory, isFetching: isFetchingHistory, refetch: reFetchFn} = useChatHistory({enabled: false});
    const sendMutation = useChatbotMessage();

    React.useEffect(() => {
        if (!open) return;
        reFetchFn()
    }, [open]);

    async function handleSend(event?: React.FormEvent) {
        event?.preventDefault();

        const message = input.trim();
        if (!message || sendMutation.isPending || isFetchingHistory) return;

        setInput("");
        const payload: ChatRequest = {message, user_id: userId}

        try {
            await sendMutation.mutateAsync(payload);
            // Wait for the history to be refetched to show the new message
            await reFetchFn();
        } catch (error) {
            toast.error("Could not send message. Please try again.");
        }
    }

    const messages = chatHistory?.history ?? [];
    const activeConversationId = getConversationCookie();
    const isLoadingHistory = open && Boolean(activeConversationId) && isFetchingHistory;
    const isSending = sendMutation.isPending;

    return (
        <Dialog.Root open={open} onOpenChange={setOpen}>
            <Dialog.Trigger asChild>
                <button
                    type="button"
                    className="fixed bottom-6 left-6 z-50 h-14 w-14 rounded-full bg-[var(--color-surface)] text-[var(--color-text-primary)] shadow-[var(--shadow-soft)] border border-[var(--color-border-subtle)] hover:border-[var(--color-border-strong)] hover:bg-[var(--color-surface-alt)] active:scale-[0.98] flex items-center justify-center transition-[transform,background-color,border-color] duration-150"
                    aria-label="Open chatbot"
                >
                    <Bot className="h-6 w-6"/>
                </button>
            </Dialog.Trigger>

            <Dialog.Portal>
                <Dialog.Overlay className="fixed inset-0 z-[9998] bg-black/40 backdrop-blur-[1px]"/>

                <Dialog.Content
                    className="fixed z-[9999] bottom-24 left-6 w-[380px] max-w-[calc(100vw-3rem)] h-[520px] max-h-[calc(100vh-8rem)] rounded-[var(--radius-lg)] bg-[var(--color-surface)] shadow-[var(--shadow-soft)] border border-[var(--color-border-subtle)] flex flex-col overflow-hidden">
                    <div
                        className="flex items-center justify-between px-4 py-3 border-b border-[var(--color-border-subtle)]">
                        <div className="flex items-centent gap-2 justify-center">
                            <div
                                className="h-9 w-9 rounded-[var(--radius-pill)] bg-[var(--color-surface-alt)] text-[var(--color-text-primary)] border border-[var(--color-border-subtle)] flex items-center justify-center">
                                <Bot className="h-5 w-5"/>
                            </div>
                            <Dialog.Title className="mb-0 text-sm font-semibold text-[var(--color-text-primary)]">
                                Chatbot
                            </Dialog.Title>
                        </div>

                        <Dialog.Close asChild>
                            <button
                                type="button"
                                className="h-9 w-9 rounded-[var(--radius-pill)] border border-transparent hover:border-[var(--color-border-subtle)] hover:bg-[var(--color-surface-alt)] flex items-center justify-center transition-colors"
                                aria-label="Close"
                            >
                                <X className="h-5 w-5 text-[var(--color-text-secondary)]"/>
                            </button>
                        </Dialog.Close>
                    </div>

                    {/* Messages */}
                    <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2 bg-[var(--color-bg)]">
                        {isLoadingHistory && (
                            <div className="text-xs text-[var(--color-text-secondary)] px-2">Loading history…</div>
                        )}

                        {!isLoadingHistory && messages.length === 0 && (
                            <div className="text-sm text-[var(--color-text-secondary)] px-2 py-2">
                                Ask a question to get started.
                            </div>
                        )}

                        {messages.map((message, index) => {
                            const isUser = message.role === "user";
                            return (
                                <div key={index} className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
                                    <div
                                        className={`max-w-[82%] rounded-[var(--radius-lg)] px-3 py-2 text-sm leading-relaxed border ${
                                            isUser
                                                ? "bg-[var(--color-accent)] text-[var(--primary-foreground)] border-transparent"
                                                : "bg-[var(--color-surface-alt)] text-[var(--color-text-primary)] border-[var(--color-border-subtle)]"
                                        }`}
                                    >
                                        {message.content}
                                    </div>
                                </div>
                            );
                        })}

                        {(isSending || isFetchingHistory) && (
                            <div className="flex items-center gap-2 px-2 py-2">
                                <Loader2 className="h-4 w-4 animate-spin text-[var(--color-text-secondary)]" />
                                <div className="text-xs text-[var(--color-text-secondary)]">Thinking…</div>
                            </div>
                        )}
                    </div>

                    {/* Input */}
                    <form onSubmit={handleSend}
                          className="border-t border-[var(--color-border-subtle)] p-3 bg-[var(--color-surface)]">
                        <div className="flex justify-center items-center gap-2">
                            <div className="flex-1 flex items-center">
                                <label className="sr-only" htmlFor="chatbot-input">Message</label>
                                <textarea
                                    id="chatbot-input"
                                    rows={1}
                                    value={input}
                                    onChange={(e) => setInput(e.target.value)}
                                    placeholder="Type a message…"
                                    className="w-full resize-none rounded-[var(--radius-md)] border border-[var(--color-border-subtle)] bg-[#050b18] text-[var(--color-text-primary)] px-3 py-2 text-sm outline-none placeholder:text-[var(--color-text-secondary)] focus:border-[var(--color-accent)] focus:ring-2 focus:ring-[var(--color-accent-subtle)]"
                                    onKeyDown={(e) => {
                                        if (e.key === "Enter" && !e.shiftKey) {
                                            e.preventDefault();
                                            void handleSend();
                                        }
                                    }}
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={!input.trim() || isSending || isFetchingHistory}
                                className="h-9 w-9 rounded-[var(--radius-md)] flex items-center justify-center bg-[var(--color-accent)] text-[var(--primary-foreground)] hover:bg-[var(--color-accent-hover)] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                aria-label="Send"
                            >
                                {isSending || isFetchingHistory ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                    <Send className="h-4 w-4"/>
                                )}
                            </button>
                        </div>
                    </form>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    )
}
