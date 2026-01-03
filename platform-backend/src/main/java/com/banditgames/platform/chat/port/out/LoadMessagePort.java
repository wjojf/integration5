package com.banditgames.platform.chat.port.out;

import com.banditgames.platform.chat.domain.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoadMessagePort {
    /**
     * Load paginated conversation between two users
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @param pageable Pagination information
     * @return Page of messages
     */
    Page<Message> findConversationBetweenUsers(String userId1, String userId2, Pageable pageable);
    
    /**
     * Find all conversation partners for a user
     * @param userId The user ID
     * @return List of user IDs
     */
    List<String> findConversationPartners(String userId);
    
    /**
     * Find unread messages for a specific receiver from a specific sender
     * @param receiverId The receiver's ID
     * @param senderId The sender's ID
     * @return List of unread messages
     */
    List<Message> findUnreadMessages(String receiverId, String senderId);
}

