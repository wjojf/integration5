package com.banditgames.platform.lobby.adapter.persistence;

import com.banditgames.platform.lobby.domain.ExternalGameInstance;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExternalGameInstanceMapper {
    
    ExternalGameInstance toDomain(ExternalGameInstanceEntity entity);
    
    ExternalGameInstanceEntity toEntity(ExternalGameInstance domain);
}





