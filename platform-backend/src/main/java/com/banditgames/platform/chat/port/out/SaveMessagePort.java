package com.banditgames.platform.chat.port.out;

import com.banditgames.platform.chat.domain.Message;

public interface SaveMessagePort {
    /**
     * Save a message to the database
     * @param message The message to save
     * @return The saved message
     */
    Message save(Message message);
}

