package com.banditgames.platform.chat.adapter.persistence;

import com.banditgames.platform.chat.domain.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    
    Page<MessageEntity> findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderBySentAtDesc(
        String senderId1, String receiverId1,
        String senderId2, String receiverId2,
        Pageable pageable
    );
    
    List<MessageEntity> findByReceiverIdAndSenderIdAndStatusNot(
        String receiverId,
        String senderId,
        MessageStatus status
    );
    
    List<MessageEntity> findDistinctBySenderId(String senderId);
    
    List<MessageEntity> findDistinctByReceiverId(String receiverId);
}
