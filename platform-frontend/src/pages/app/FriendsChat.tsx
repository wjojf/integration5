import { useState, useEffect } from "react";
import { Send, Search, UserPlus, Wifi, WifiOff } from "lucide-react";
import { toast } from "sonner";

import type { PageableResponse } from '../../types/api.types'
import type {Message, Player} from '../../types/app.types'
import { InputComponents, DisplayComponents, FeatureComponents } from '../../components/app'
import { useFriends } from "../../hooks/friend/useFriends";
import { useConversation, useSendMessage } from "../../hooks/chat/useChat";
import { useProfile } from "../../hooks/player/useProfile";
import { useChatWebSocketContext } from "../../contexts/ChatWebSocketContext";

const { Button, Input } = InputComponents
const { Card, AvatarFallback, Avatar, ScrollArea } = DisplayComponents
const { ChatMessages } = FeatureComponents

export const FriendsChat = () => {
  const { data: profile = {} as Player } = useProfile();
  const { data: friendsData, isLoading: isFriendsLoading } = useFriends();
  const [selectedFriend, setSelectedFriend] = useState<Player | undefined>();
  const conversationData = useConversation(selectedFriend?.playerId || "", { page: 0, size: 50 });
  const [newMessage, setNewMessage] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const sendMessage = useSendMessage();
  // Use global connection from context (toast notifications handled by ChatWebSocketProvider)
  const { connected } = useChatWebSocketContext();

  // Ensure friends is always an array
  const friends = Array.isArray(friendsData) ? friendsData : [];

  // Ensure messages is always an array
  const messages = Array.isArray(conversationData?.data?.content) 
    ? conversationData.data.content 
    : [];

  useEffect(() => {
    const firstFriend = friends?.[0]

    firstFriend && !selectedFriend && setSelectedFriend(firstFriend);
  }, [friends, selectedFriend, profile]);

  const handleSendMessage = async () => {
    if (newMessage.trim() && selectedFriend) {
      await sendMessage.mutateAsync({ receiverId: selectedFriend.playerId, content: newMessage })
          .catch(() => toast.error("Failed to send message"))
      setNewMessage("")
    }
  };

  const searchedFriends = friends?.filter((friend) => {
    return friend.username?.toLowerCase().includes(searchQuery?.toLowerCase());
  }) || [];


  if (isFriendsLoading) {
    return <div className="text-center py-12">Loading friends...</div>;
  }

  if (!friends?.length) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="mb-2">Friends & Chat</h1>
          <p className="text-muted-foreground">Connect and chat with your gaming friends</p>
        </div>
        <Card className="p-12 text-center">
          <UserPlus className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
          <h3 className="mb-2">No friends yet</h3>
          <p className="text-muted-foreground">
            Go to the Players tab to search and add friends!
          </p>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="mb-2">Friends & Chat</h1>
        <p className="text-muted-foreground">Connect and chat with your gaming friends</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-1 bg-card border-border">
          <div className="p-4 border-b border-border">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
              <Input
                placeholder="Search friends..."
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                className="pl-9"
              />
            </div>
          </div>

          <ScrollArea className="h-[600px]">
            <div className="p-2">
              {searchedFriends.map((friend) => {
                const isSelected = friend.playerId === selectedFriend?.playerId;

                return (
                  <button
                    key={friend.playerId}
                    onClick={() => setSelectedFriend(friend)}
                    className={`w-full p-3 rounded-lg flex items-center gap-3 hover:bg-accent transition-colors ${
                      isSelected ? 'bg-accent' : ''
                    }`}
                  >
                    <Avatar>
                      <AvatarFallback>{friend.username[0].toUpperCase()}</AvatarFallback>
                    </Avatar>
                    <div className="flex-1 text-left">
                      <div className="text-sm font-medium">{friend.username}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </ScrollArea>
        </Card>

        <Card className="lg:col-span-2 bg-card border-border flex flex-col">
          <div className="p-4 border-b border-border">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Avatar>
                  <AvatarFallback>{selectedFriend?.username[0].toUpperCase()}</AvatarFallback>
                </Avatar>
                <div>
                  <div className="font-medium">{selectedFriend?.username}</div>
                </div>
              </div>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                {connected ? (
                  <>
                    <Wifi className="h-3 w-3 text-green-500" />
                    <span>Connected</span>
                  </>
                ) : (
                  <>
                    <WifiOff className="h-3 w-3 text-red-500" />
                    <span>Disconnected</span>
                  </>
                )}
              </div>
            </div>
          </div>

          <ScrollArea className="flex-1 h-[500px] p-4">
            <div className="space-y-4">
              {messages.length === 0 ? (
                <div className="text-center py-12 text-muted-foreground">
                  No messages yet. Start a conversation!
                </div>
              ) : (
                <ChatMessages profile={profile} messages={messages} />
              )}
            </div>
          </ScrollArea>

          <div className="p-4 border-t border-border">
            <div className="flex gap-2">
              <Input
                placeholder="Type a message..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleSendMessage()}
              />
              <Button onClick={handleSendMessage} size="icon" disabled={sendMessage.isPending}>
                <Send className="w-4 h-4"/>
              </Button>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
