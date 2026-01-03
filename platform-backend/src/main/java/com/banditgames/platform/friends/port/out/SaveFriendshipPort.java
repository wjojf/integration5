package com.banditgames.platform.friends.port.out;

import com.banditgames.platform.friends.domain.Friendship;

public interface SaveFriendshipPort {
    Friendship save(Friendship friendship);
}

