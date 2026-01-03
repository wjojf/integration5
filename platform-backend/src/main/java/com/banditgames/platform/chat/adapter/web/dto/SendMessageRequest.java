package com.banditgames.platform.chat.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send a message to another user")
public class SendMessageRequest {
    
    @NotNull(message = "Receiver ID is required")
    @Schema(
            description = "UUID of the user to send the message to. Must be a valid UUID format. Cannot be null or the same as the authenticated user's ID.",
            example = "987fcdeb-51a2-43f1-b789-123456789abc",
            format = "uuid"
    )
    private UUID receiverId;
    
    @NotBlank(message = "Message content cannot be blank")
    @Size(min = 1, max = 2000, message = "Message content must be between 1 and 2000 characters")
    @Schema(
            description = "Content of the message. Must not be blank and must be between 1 and 2000 characters.",
            example = "Hey! Are you ready for the game tonight?",
            minLength = 1,
            maxLength = 2000
    )
    private String content;
}

