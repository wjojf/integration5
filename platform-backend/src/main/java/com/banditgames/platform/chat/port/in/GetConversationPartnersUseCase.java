package com.banditgames.platform.chat.port.in;

import java.util.List;

public interface GetConversationPartnersUseCase {
    /**
     * Get list of all users the authenticated user has exchanged messages with
     * @param userId The authenticated user's ID
     * @return List of user IDs
     */
    List<String> getConversationPartners(String userId);
}

