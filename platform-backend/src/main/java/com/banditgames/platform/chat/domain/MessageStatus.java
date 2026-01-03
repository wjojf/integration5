package com.banditgames.platform.chat.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a message in the chat system")
public enum MessageStatus {
    @Schema(description = "Message has been sent but not yet delivered to the recipient")
    SENT,
    
    @Schema(description = "Message has been delivered to the recipient's device")
    DELIVERED,
    
    @Schema(description = "Message has been read by the recipient")
    READ
}

