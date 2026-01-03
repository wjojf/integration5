package com.banditgames.platform.chat.port.in;

import com.banditgames.platform.chat.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetConversationUseCase {
    /**
     * Get paginated conversation between two users and mark unread messages as read
     * @param userId The authenticated user's ID
     * @param otherUserId The other user's ID
     * @param pageable Pagination information
     * @return Page of messages in the conversation
     */
    Page<Message> getConversation(String userId, String otherUserId, Pageable pageable);
}

