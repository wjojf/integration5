package com.banditgames.platform.chat.adapter.persistence;

import com.banditgames.platform.chat.domain.Message;
import com.banditgames.platform.chat.domain.MessageStatus;
import com.banditgames.platform.chat.port.out.LoadMessagePort;
import com.banditgames.platform.chat.port.out.SaveMessagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MessagePersistenceAdapter implements SaveMessagePort, LoadMessagePort {
    
    private final MessageRepository messageRepository;
    
    @Override
    public Message save(Message message) {
        MessageEntity entity = toEntity(message);
        MessageEntity saved = messageRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Page<Message> findConversationBetweenUsers(String userId1, String userId2, Pageable pageable) {
        Page<MessageEntity> entities = messageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderBySentAtDesc(
            userId1, userId2, userId2, userId1, pageable
        );
        return entities.map(this::toDomain);
    }
    
    @Override
    public List<String> findConversationPartners(String userId) {
        Set<String> partners = new HashSet<>();
        
        messageRepository.findDistinctBySenderId(userId).forEach(msg -> partners.add(msg.getReceiverId()));
        messageRepository.findDistinctByReceiverId(userId).forEach(msg -> partners.add(msg.getSenderId()));
        
        return partners.stream().collect(Collectors.toList());
    }
    
    @Override
    public List<Message> findUnreadMessages(String receiverId, String senderId) {
        return messageRepository.findByReceiverIdAndSenderIdAndStatusNot(receiverId, senderId, MessageStatus.READ)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    private MessageEntity toEntity(Message message) {
        return MessageEntity.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .status(message.getStatus())
                .sentAt(message.getSentAt())
                .readAt(message.getReadAt())
                .build();
    }
    
    private Message toDomain(MessageEntity entity) {
        return Message.builder()
                .id(entity.getId())
                .senderId(entity.getSenderId())
                .receiverId(entity.getReceiverId())
                .content(entity.getContent())
                .status(entity.getStatus())
                .sentAt(entity.getSentAt())
                .readAt(entity.getReadAt())
                .build();
    }
}
