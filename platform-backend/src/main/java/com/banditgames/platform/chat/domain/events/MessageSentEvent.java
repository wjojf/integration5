package com.banditgames.platform.chat.domain.events;

import com.banditgames.platform.chat.domain.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageSentEvent {
    private final Message message;
}

