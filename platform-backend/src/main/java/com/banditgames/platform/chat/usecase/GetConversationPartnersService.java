package com.banditgames.platform.chat.usecase;

import com.banditgames.platform.chat.port.in.GetConversationPartnersUseCase;
import com.banditgames.platform.chat.port.out.LoadMessagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetConversationPartnersService implements GetConversationPartnersUseCase {
    
    private final LoadMessagePort loadMessagePort;
    
    @Override
    public List<String> getConversationPartners(String userId) {
        return loadMessagePort.findConversationPartners(userId);
    }
}

