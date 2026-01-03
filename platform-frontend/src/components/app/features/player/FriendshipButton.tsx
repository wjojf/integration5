import { Handshake, UserCheck, UserPlus, Ban, XCircle } from "lucide-react";
import {UseMutationResult} from "@tanstack/react-query";
import { toast } from "sonner";

import { SendFriendRequestPayload, ModifyFriendRequest} from '../../../../types/api.types'
import {Friendship, FriendshipStatus } from '../../../../types/app.types'
import {InputComponents} from "../../input";

const { Button } = InputComponents;

type FriendshipButtonProps = {
    userId: string;
    playerId: string;
    friendship?: Friendship;
    onSend: UseMutationResult<Friendship, Error, SendFriendRequestPayload, unknown>
    onUpdate: UseMutationResult<Friendship, Error, ModifyFriendRequest, unknown>
}

export function FriendshipButton(props:FriendshipButtonProps ) {
    const {userId, playerId, friendship, onSend, onUpdate} = props

    if (!friendship) {
        return (
            <Button
                type="button"
                className="gap-2"
                onClick={async () => {
                    if (!playerId) {
                        toast.error("Invalid player ID");
                        return;
                    }
                    // Validate UUID format
                    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
                    if (!uuidRegex.test(playerId)) {
                        toast.error("Invalid player ID format");
                        return;
                    }
                    try {
                        await onSend.mutateAsync({ addresseeId: playerId });
                        toast.success("Friend request sent successfully");
                    } catch (error: any) {
                        console.error('Failed to send friend request:', error);
                        // Extract error message from various possible response formats
                        let errorMessage = "Failed to send friend request. Please try again.";
                        
                        if (error?.response?.data) {
                            const data = error.response.data;
                            // Try different possible error message fields
                            errorMessage = data.message 
                                || data.error 
                                || data.detail
                                || (typeof data === 'string' ? data : errorMessage);
                            
                            // Check for validation errors
                            if (data.details && typeof data.details === 'object') {
                                const validationErrors = Object.values(data.details).join(', ');
                                if (validationErrors) {
                                    errorMessage = validationErrors;
                                }
                            }
                        } else if (error?.message) {
                            errorMessage = error.message;
                        }
                        
                        // Handle specific error cases
                        if (error?.response?.status === 503) {
                            errorMessage = "Service temporarily unavailable. Please try again later.";
                        } else if (error?.response?.status === 400) {
                            if (!errorMessage.includes("Failed to send")) {
                                // Use the extracted message
                            } else {
                                errorMessage = "Invalid request. Please check the player ID and try again.";
                            }
                        }
                        
                        toast.error(errorMessage);
                    }
                }}>
                <UserPlus className="h-4 w-4" />
                Send Friendship Request
            </Button>
        );
    }

    const iAmRequester = friendship.requesterId === userId;
    const iAmAddressee = friendship.addresseeId === userId;

    if (friendship.status === FriendshipStatus.PENDING) {
        if (iAmRequester) {
            return (
                <Button 
                    type="button" 
                    className="gap-2"
                    variant="outline"
                    onClick={async () => {
                        try {
                            await onUpdate.mutateAsync({ id: friendship.id, data: { action: 'CANCEL' } });
                            toast.success("Friend request cancelled successfully");
                        } catch (error: any) {
                            const errorMessage = error?.response?.data?.message 
                                || error?.response?.data?.error 
                                || error?.message 
                                || "Failed to cancel friend request";
                            toast.error(errorMessage);
                        }
                    }}
                >
                    <XCircle className="h-4 w-4" />
                    Cancel Request
                </Button>
            );
        }

        return (
            <div className="inline-flex items-center justify-end gap-2">
                <Button
                    type="button"
                    className="gap-2"
                    onClick={() => onUpdate.mutateAsync({ id: friendship.id, data: { action: 'ACCEPT' } })}
                >
                    <Handshake className="h-4 w-4" />
                    Accept
                </Button>
                <Button
                    type="button"
                    className="gap-2"
                    onClick={() => onUpdate.mutateAsync({ id: friendship.id, data: { action: 'REJECT' } })}
                >
                    <XCircle className="h-4 w-4" />
                    Reject
                </Button>
                <Button
                    type="button"
                    className="gap-2"
                    onClick={() => onUpdate.mutateAsync({ id: friendship.id, data: { action: 'BLOCK' } })}
                >
                    <Ban className="h-4 w-4" />
                    Block
                </Button>
            </div>
        );
    }

    if (friendship.status === FriendshipStatus.ACCEPTED) {
        return (
            <div className="inline-flex items-center justify-end gap-2">
                <Button 
                    type="button" 
                    className="gap-2"
                    variant="outline"
                    onClick={async () => {
                        try {
                            await onUpdate.mutateAsync({ id: friendship.id, data: { action: 'REMOVE' } });
                            toast.success("Friend removed successfully");
                        } catch (error: any) {
                            const errorMessage = error?.response?.data?.message 
                                || error?.response?.data?.error 
                                || error?.message 
                                || "Failed to remove friend";
                            toast.error(errorMessage);
                        }
                    }}
                >
                    <XCircle className="h-4 w-4" />
                    Remove
                </Button>
                <Button 
                    type="button" 
                    className="gap-2"
                    variant="outline"
                    onClick={async () => {
                        try {
                            await onUpdate.mutateAsync({ id: friendship.id, data: { action: 'BLOCK' } });
                            toast.success("Friend blocked successfully");
                        } catch (error: any) {
                            const errorMessage = error?.response?.data?.message 
                                || error?.response?.data?.error 
                                || error?.message 
                                || "Failed to block friend";
                            toast.error(errorMessage);
                        }
                    }}
                >
                    <Ban className="h-4 w-4" />
                    Block
                </Button>
            </div>
        );
    }

    if (friendship.status === FriendshipStatus.REJECTED) {
        return (
            <Button type="button" className="gap-2" disabled>
                <XCircle className="h-4 w-4" />
                {iAmRequester ? "Declined" : "Rejected"}
            </Button>
        );
    }

    if (friendship.status === FriendshipStatus.BLOCKED) {
        if (iAmAddressee) {
            // User who blocked can unblock
            return (
                <Button 
                    type="button" 
                    className="gap-2"
                    variant="outline"
                    onClick={async () => {
                        try {
                            await onUpdate.mutateAsync({ id: friendship.id, data: { action: 'UNBLOCK' } });
                            toast.success("User unblocked successfully");
                        } catch (error: any) {
                            const errorMessage = error?.response?.data?.message 
                                || error?.response?.data?.error 
                                || error?.message 
                                || "Failed to unblock user";
                            toast.error(errorMessage);
                        }
                    }}
                >
                    <UserCheck className="h-4 w-4" />
                    Unblock
                </Button>
            );
        }
        // User who was blocked - can't do anything
        return (
            <Button type="button" className="gap-2" disabled>
                <Ban className="h-4 w-4" />
                Blocked
            </Button>
        );
    }

    return null;
}
