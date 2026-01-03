import { UseMutationResult } from "@tanstack/react-query"
import { JSX } from "react"

import { SendFriendRequestPayload, ModifyFriendRequest } from "../../../../types/api.types";
import { Friendship, Player } from "../../../../types/app.types";
import { USER_RARITY_COLOR } from "../../../../config/app.config";
import { DisplayComponents } from "../../display";
import { Badge } from '../../../shared'
import { FriendshipButton } from './FriendshipButton'

const { Avatar, AvatarFallback } = DisplayComponents;

type PlayerRowProps = {
    players: Player[],
    friendshipRequests: Friendship[],
    sendFriendshipRequest: UseMutationResult<Friendship, Error, SendFriendRequestPayload, unknown>,
    updateFriendshipRequest: UseMutationResult<Friendship, Error, ModifyFriendRequest, unknown>,
    userId: string
}

export const PlayerRows = (props: PlayerRowProps): JSX.Element[] => {
    const {
        players, friendshipRequests, sendFriendshipRequest: onSend, updateFriendshipRequest: onUpdate, userId
    } = props

    return players.map((player) => {
        const { playerId, username, rank, exp } = player;
        // FriendInfoResponse only contains the "other" user, so we match by playerId
        const friendship = friendshipRequests.find((item) => {
            // Check if this friendship involves the current player
            // FriendInfoResponse.player.playerId is the "other" user in the friendship
            const otherPlayerId = item.addresseeId || (item as any)?._friendInfo?.player?.playerId;
            return otherPlayerId === playerId;
        })

        return (
            <tr key={playerId} className="border-t border-border transition-colors hover:bg-secondary/60">
                <td className="px-4 py-4">
                    <div className="flex items-center gap-3">
                        <Avatar className="h-9 w-9">
                            <AvatarFallback className="bg-secondary text-foreground">
                                {username[0]?.toUpperCase()}
                            </AvatarFallback>
                        </Avatar>

                        <div className="min-w-0">
                            <div className="truncate font-semibold text-foreground">{username}</div>
                        </div>
                    </div>
                </td>

                <td className="px-4 py-4">
                    <Badge className={`${USER_RARITY_COLOR[rank]} border-0`}>
                        {String(rank)}
                    </Badge>
                </td>

                <td className="px-4 py-4">
                    <span className="font-medium text-foreground">{exp}</span>
                </td>

                <td className="px-4 py-4 text-right">
                    {FriendshipButton({ userId, playerId, friendship, onSend, onUpdate })}
                </td>
            </tr>
        );
    });
}
