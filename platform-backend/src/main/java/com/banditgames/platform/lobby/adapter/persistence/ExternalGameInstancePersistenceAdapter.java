package com.banditgames.platform.lobby.adapter.persistence;

import com.banditgames.platform.lobby.domain.ExternalGameInstance;
import com.banditgames.platform.lobby.port.out.LoadExternalGameInstancePort;
import com.banditgames.platform.lobby.port.out.SaveExternalGameInstancePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalGameInstancePersistenceAdapter 
        implements LoadExternalGameInstancePort, SaveExternalGameInstancePort {
    
    private final ExternalGameInstanceRepository repository;
    private final ExternalGameInstanceMapper mapper;
    
    @Override
    public Optional<ExternalGameInstance> findByLobbyId(UUID lobbyId) {
        return repository.findByLobbyId(lobbyId)
                .map(mapper::toDomain);
    }
    
    @Override
    public Optional<ExternalGameInstance> findByLobbyIdAndGameType(UUID lobbyId, String gameType) {
        return repository.findByLobbyIdAndExternalGameType(lobbyId, gameType)
                .map(mapper::toDomain);
    }
    
    @Override
    public ExternalGameInstance save(ExternalGameInstance externalGameInstance) {
        ExternalGameInstanceEntity entity = mapper.toEntity(externalGameInstance);
        ExternalGameInstanceEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public void deleteByLobbyId(UUID lobbyId) {
        repository.deleteByLobbyId(lobbyId);
    }
}



