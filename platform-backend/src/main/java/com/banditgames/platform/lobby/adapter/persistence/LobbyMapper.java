package com.banditgames.platform.lobby.adapter.persistence;

import com.banditgames.platform.lobby.domain.Lobby;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LobbyMapper {
    
    Lobby toDomain(LobbyEntity entity);
    
    LobbyEntity toEntity(Lobby lobby);
}

