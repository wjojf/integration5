package com.banditgames.platform.chat.adapter.web;

import com.banditgames.platform.chat.adapter.web.dto.MessageResponse;
import com.banditgames.platform.chat.adapter.web.dto.SendMessageRequest;
import com.banditgames.platform.chat.domain.Message;
import com.banditgames.platform.chat.port.in.GetConversationPartnersUseCase;
import com.banditgames.platform.chat.port.in.GetConversationUseCase;
import com.banditgames.platform.chat.port.in.SendMessageUseCase;
import com.banditgames.platform.shared.security.AuthUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Chat", description = "API for direct messaging between users")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetConversationUseCase getConversationUseCase;
    private final GetConversationPartnersUseCase getConversationPartnersUseCase;

    @Operation(
            summary = "Send a message",
            description = "Send a direct message to another user. The authenticated user becomes the sender. " +
                    "Validation rules: receiverId must be a valid UUID and cannot be null or the same as the authenticated user's ID. " +
                    "Content must not be blank and must be between 1 and 2000 characters."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent successfully",
                    content = @Content(
                            schema = @Schema(implementation = MessageResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful message sent",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "senderId": "123e4567-e89b-12d3-a456-426614174001",
                                              "receiverId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                              "content": "Hey! Are you ready for the game tonight?",
                                              "status": "SENT",
                                              "sentAt": "2023-12-06T10:15:30",
                                              "readAt": null
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "400", description = "Invalid request - cannot send message to yourself, content is blank, or content exceeds 2000 characters",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Bad request example",
                                    value = """
                                            {
                                              "message": "Cannot send message to yourself"
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @PostMapping("/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Message details containing receiver ID and message content. Validation: receiverId is required (UUID), content is required (1-2000 chars).",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SendMessageRequest.class),
                            examples = @ExampleObject(
                                    name = "Send message",
                                    value = """
                                            {
                                              "receiverId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                              "content": "Hey! Are you ready for the game tonight?"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody SendMessageRequest request) {
        String senderId = AuthUtils.getCurrentUserId().toString();
        Message message = sendMessageUseCase.sendMessage(senderId, request.getReceiverId(), request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.fromDomain(message));
    }

    @Operation(
            summary = "Get conversation with a user (paginated)",
            description = "Retrieve paginated messages from a conversation between the authenticated user and another user. " +
                    "Returns the most recent messages first (sorted by sentAt descending). " +
                    "**Important: This endpoint automatically marks any unread messages (where the authenticated user is the receiver) as READ before returning the response.** " +
                    "Default page size is 20 messages, maximum is 50. " +
                    "Validation rules: userId must be a valid UUID, page must be >= 0, size must be between 1 and 50."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation retrieved successfully. Unread messages in the batch have been marked as READ. Returns a paginated response.",
                    content = @Content(
                            schema = @Schema(implementation = Page.class),
                            examples = @ExampleObject(
                                    name = "Paginated conversation",
                                    value = """
                                            {
                                              "content": [
                                                {
                                                  "id": "660e8400-e29b-41d4-a716-446655440001",
                                                  "senderId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                                  "receiverId": "123e4567-e89b-12d3-a456-426614174001",
                                                  "content": "Yes! See you at 8pm",
                                                  "status": "READ",
                                                  "sentAt": "2023-12-06T10:20:15",
                                                  "readAt": "2023-12-06T10:25:30"
                                                },
                                                {
                                                  "id": "550e8400-e29b-41d4-a716-446655440000",
                                                  "senderId": "123e4567-e89b-12d3-a456-426614174001",
                                                  "receiverId": "987fcdeb-51a2-43f1-b789-123456789abc",
                                                  "content": "Hey! Are you ready for the game tonight?",
                                                  "status": "SENT",
                                                  "sentAt": "2023-12-06T10:15:30",
                                                  "readAt": null
                                                }
                                              ],
                                              "pageable": {
                                                "pageNumber": 0,
                                                "pageSize": 20
                                              },
                                              "totalElements": 2,
                                              "totalPages": 1,
                                              "last": true,
                                              "first": true,
                                              "numberOfElements": 2
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "400", description = "Invalid request - invalid UUID format or invalid pagination parameters",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Invalid pagination",
                                    value = """
                                            {
                                              "message": "Page size must not exceed 50"
                                            }
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<Page<MessageResponse>> getConversation(
            @Parameter(
                    description = "UUID of the other user in the conversation. Validation: Must be a valid UUID format.",
                    required = true,
                    example = "987fcdeb-51a2-43f1-b789-123456789abc",
                    schema = @Schema(format = "uuid")
            )
            @PathVariable String userId,
            @Parameter(
                    description = "Page number (zero-based). Default is 0. Validation: Must be >= 0.",
                    example = "0",
                    schema = @Schema(minimum = "0", defaultValue = "0")
            )
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(
                    description = "Number of messages per page. Default is 20, maximum is 50. Validation: Must be between 1 and 50.",
                    example = "20",
                    schema = @Schema(minimum = "1", maximum = "50", defaultValue = "20")
            )
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        String currentUserId = AuthUtils.getCurrentUserId().toString();
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = getConversationUseCase.getConversation(currentUserId, userId, pageable);
        Page<MessageResponse> response = messages.map(MessageResponse::fromDomain);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all conversation partners",
            description = "Retrieve a list of all user IDs that the authenticated user has exchanged messages with. " +
                    "This includes users the authenticated user has sent messages to or received messages from."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation partners retrieved successfully. Returns an array of user IDs.",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = String.class, format = "uuid")),
                            examples = @ExampleObject(
                                    name = "List of conversation partners",
                                    value = """
                                            [
                                              "987fcdeb-51a2-43f1-b789-123456789abc",
                                              "123e4567-e89b-12d3-a456-426614174002",
                                              "456e7890-e89b-12d3-a456-426614174003"
                                            ]
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    @GetMapping("/conversations")
    public ResponseEntity<List<String>> getConversationPartners() {
        String currentUserId = AuthUtils.getCurrentUserId().toString();
        List<String> partners = getConversationPartnersUseCase.getConversationPartners(currentUserId);
        return ResponseEntity.ok(partners);
    }
}

