import { useState } from "react";
import { Users, UserCheck, Clock, XCircle, Ban, Search } from "lucide-react";
import { toast } from "sonner";
import { useKeycloak } from "@react-keycloak/web";
import ReactPaginate from "react-paginate";

import type { PageableResponse } from '../../types/api.types';
import { InputComponents, DisplayComponents } from '../../components/app';
import { useFriendshipsByStatus, useModifyFriendRequest } from "../../hooks/friend/useFriends";
import { FriendshipStatus } from '../../types/app.types';

const { Button, Input } = InputComponents;
const { Card, CardContent, CardHeader, Avatar, AvatarFallback } = DisplayComponents;

type FriendshipStatusFilter = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'BLOCKED' | undefined;

export const Friends = () => {
  const { keycloak } = useKeycloak();
  const userId = keycloak.tokenParsed?.sub as string;
  const [activeTab, setActiveTab] = useState<FriendshipStatusFilter>(undefined);
  const [currentPage, setCurrentPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const pageSize = 20;

  const { data: friendshipsResponse, isLoading } = useFriendshipsByStatus(activeTab, currentPage, pageSize);
  const modifyFriendship = useModifyFriendRequest();

  const friendships = friendshipsResponse?.content || [];
  const totalPages = friendshipsResponse?.totalPages || 0;
  const totalElements = friendshipsResponse?.totalElements || 0;

  const handleAction = async (friendshipId: string, action: 'ACCEPT' | 'REJECT' | 'BLOCK' | 'CANCEL' | 'REMOVE' | 'UNBLOCK') => {
    try {
      await modifyFriendship.mutateAsync({ id: friendshipId, data: { action } });
      const actionMessages: Record<string, string> = {
        'ACCEPT': 'Friend request accepted successfully',
        'REJECT': 'Friend request rejected successfully',
        'BLOCK': 'User blocked successfully',
        'CANCEL': 'Friend request cancelled successfully',
        'REMOVE': 'Friend removed successfully',
        'UNBLOCK': 'User unblocked successfully',
      };
      toast.success(actionMessages[action] || 'Action completed successfully');
    } catch (error: any) {
      const errorMessage = error?.response?.data?.message 
        || error?.response?.data?.error 
        || error?.message 
        || `Failed to ${action.toLowerCase()} friend request`;
      toast.error(errorMessage);
    }
  };

  const filteredFriendships = friendships.filter((friendship: any) => {
    if (!searchQuery) return true;
    const player = friendship.player || friendship._friendInfo?.player;
    const username = player?.username || '';
    return username.toLowerCase().includes(searchQuery.toLowerCase());
  });

  const tabs = [
    { id: undefined, label: 'All', icon: Users, count: null },
    { id: 'PENDING' as FriendshipStatusFilter, label: 'Pending', icon: Clock, count: null },
    { id: 'ACCEPTED' as FriendshipStatusFilter, label: 'Friends', icon: UserCheck, count: null },
    { id: 'REJECTED' as FriendshipStatusFilter, label: 'Rejected', icon: XCircle, count: null },
    { id: 'BLOCKED' as FriendshipStatusFilter, label: 'Blocked', icon: Ban, count: null },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="mb-2">Friends</h1>
        <p className="text-muted-foreground">Manage your friendships and friend requests</p>
      </div>

      <Card className="w-full border-border bg-card">
        <CardHeader className="space-y-4">
          {/* Tabs */}
          <div className="flex flex-wrap gap-2 border-b border-border pb-2">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id || 'all'}
                  onClick={() => {
                    setActiveTab(tab.id);
                    setCurrentPage(0);
                  }}
                  className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-primary text-primary-foreground'
                      : 'hover:bg-accent text-muted-foreground hover:text-foreground'
                  }`}
                >
                  <Icon className="h-4 w-4" />
                  <span>{tab.label}</span>
                  {tab.count !== null && (
                    <span className={`text-xs ${isActive ? 'text-primary-foreground/70' : 'text-muted-foreground'}`}>
                      ({tab.count})
                    </span>
                  )}
                </button>
              );
            })}
          </div>

          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              type="text"
              placeholder="Search friends..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
            />
          </div>
        </CardHeader>

        <CardContent>
          {isLoading ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground">Loading friendships...</p>
            </div>
          ) : filteredFriendships.length === 0 ? (
            <div className="text-center py-12">
              <Users className="w-12 h-12 mx-auto mb-4 text-muted-foreground" />
              <h3 className="mb-2">No friendships found</h3>
              <p className="text-muted-foreground">
                {activeTab 
                  ? `No ${activeTab.toLowerCase()} friendships found.`
                  : "You don't have any friendships yet. Go to the Players tab to add friends!"}
              </p>
            </div>
          ) : (
            <>
              <div className="space-y-2">
                {filteredFriendships.map((friendship: any) => {
                  const player = friendship.player || friendship._friendInfo?.player;
                  const friendshipId = friendship.friendshipId || friendship.id;
                  const status = friendship.status as FriendshipStatus;
                  const isPending = status === FriendshipStatus.PENDING;
                  const isAccepted = status === FriendshipStatus.ACCEPTED;
                  const isBlocked = status === FriendshipStatus.BLOCKED;

                  return (
                    <div
                      key={friendshipId}
                      className="flex items-center justify-between p-4 rounded-lg border border-border hover:bg-accent/50 transition-colors"
                    >
                      <div className="flex items-center gap-3 flex-1">
                        <Avatar className="h-10 w-10">
                          <AvatarFallback>
                            {player?.username?.[0]?.toUpperCase() || '?'}
                          </AvatarFallback>
                        </Avatar>
                        <div className="flex-1 min-w-0">
                          <div className="font-medium text-foreground truncate">
                            {player?.username || 'Unknown User'}
                          </div>
                          {player?.rank && (
                            <div className="text-sm text-muted-foreground">
                              {player.rank} • {player.exp || 0} EXP
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-2 flex-wrap">
                        {isPending && (
                          <>
                            {/* Show all possible actions - backend will validate permissions */}
                            <Button
                              onClick={() => handleAction(friendshipId, 'ACCEPT')}
                              disabled={modifyFriendship.isPending}
                              className="gap-2"
                            >
                              <UserCheck className="h-4 w-4" />
                              Accept
                            </Button>
                            <Button
                              variant="outline"
                              onClick={() => handleAction(friendshipId, 'REJECT')}
                              disabled={modifyFriendship.isPending}
                              className="gap-2"
                            >
                              <XCircle className="h-4 w-4" />
                              Reject
                            </Button>
                            <Button
                              variant="outline"
                              onClick={() => handleAction(friendshipId, 'CANCEL')}
                              disabled={modifyFriendship.isPending}
                              className="gap-2"
                            >
                              <XCircle className="h-4 w-4" />
                              Cancel
                            </Button>
                            <Button
                              variant="outline"
                              onClick={() => handleAction(friendshipId, 'BLOCK')}
                              disabled={modifyFriendship.isPending}
                              className="gap-2 text-destructive hover:text-destructive"
                            >
                              <Ban className="h-4 w-4" />
                              Block
                            </Button>
                          </>
                        )}
                        {isAccepted && (
                          <>
                            <Button
                              variant="outline"
                              onClick={() => handleAction(friendshipId, 'REMOVE')}
                              disabled={modifyFriendship.isPending}
                              className="gap-2"
                            >
                              <XCircle className="h-4 w-4" />
                              Remove
                            </Button>
                            <Button
                              variant="outline"
                              onClick={() => handleAction(friendshipId, 'BLOCK')}
                              disabled={modifyFriendship.isPending}
                              className="gap-2 text-destructive hover:text-destructive"
                            >
                              <Ban className="h-4 w-4" />
                              Block
                            </Button>
                          </>
                        )}
                        {isBlocked && (
                          <Button
                            variant="outline"
                            onClick={() => handleAction(friendshipId, 'UNBLOCK')}
                            disabled={modifyFriendship.isPending}
                            className="gap-2"
                          >
                            <UserCheck className="h-4 w-4" />
                            Unblock
                          </Button>
                        )}
                        {status === FriendshipStatus.REJECTED && (
                          <span className="text-sm text-muted-foreground">Rejected</span>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>

              {totalPages > 1 && (
                <div className="border-t border-border mt-4 pt-4">
                  <ReactPaginate
                    forcePage={currentPage}
                    pageCount={totalPages}
                    onPageChange={(e) => setCurrentPage(e.selected)}
                    previousLabel="Prev"
                    nextLabel="Next"
                    breakLabel="…"
                    marginPagesDisplayed={1}
                    pageRangeDisplayed={3}
                    disabledClassName="opacity-50 pointer-events-none"
                    containerClassName="flex items-center justify-center gap-2"
                    pageLinkClassName="h-9 min-w-[2.25rem] px-3 border border-border rounded-md flex items-center justify-center text-sm hover:bg-accent"
                    activeLinkClassName="bg-primary text-primary-foreground font-semibold"
                    previousLinkClassName="h-9 px-3 border border-border rounded-md flex items-center justify-center text-sm hover:bg-accent"
                    nextLinkClassName="h-9 px-3 border border-border rounded-md flex items-center justify-center text-sm hover:bg-accent"
                    breakLinkClassName="h-9 px-3 border border-border rounded-md flex items-center justify-center text-sm"
                  />
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

