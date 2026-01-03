import type { Player, Message } from '../../../../types/app.types'
import {Badge} from "../../../shared";

type MessagesProps = { profile: Player, messages: Message[] }
export const Messages = (props: MessagesProps) => {
    const { profile, messages } = props
    // Ensure messages is always an array
    const messagesArray = Array.isArray(messages) ? messages : []
    return messagesArray.sort((a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime())
        .map((message) => {
        const isOwnMessage = message.senderId === profile?.playerId;

        return (
            <div key={message.id} className={`flex ${isOwnMessage ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[70%] rounded-lg p-3 
                    ${isOwnMessage ? 'bg-primary text-primary-foreground' : 'bg-muted'}`
                }>
                    <p className="text-sm">{message.content}</p>
                    <div className="flex items-center justify-between gap-2 mt-1">
                        <p className={`text-xs 
                        ${isOwnMessage ? 'text-primary-foreground/70' : 'text-muted-foreground'}`}>
                            {new Date(message.sentAt).toLocaleTimeString([], {
                                hour: '2-digit', minute: '2-digit'
                            })}
                        </p>
                        {message.status && (
                            <Badge className="text-xs text-secondary-foreground">
                                {message.status}
                            </Badge>
                        )}
                    </div>
                </div>
            </div>
        );
    })
}
