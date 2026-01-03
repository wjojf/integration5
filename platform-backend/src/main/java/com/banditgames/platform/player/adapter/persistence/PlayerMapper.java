package com.banditgames.platform.player.adapter.persistence;

import com.banditgames.platform.player.domain.Player;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class PlayerMapper {

    public Player toDomain(PlayerEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return Player.builder()
                .playerId(entity.getPlayerId())
                .username(entity.getUsername())
                .bio(entity.getBio())
                .gamePreferences(entity.getGamePreferences() != null ? 
                    new ArrayList<>(entity.getGamePreferences()) : new ArrayList<>())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .rank(entity.getRank())
                .exp(entity.getExp())
                .build();
    }

    public PlayerEntity toEntity(Player player) {
        if (player == null) {
            return null;
        }
        
        return PlayerEntity.builder()
                .playerId(player.getPlayerId())
                .username(player.getUsername())
                .bio(player.getBio())
                .gamePreferences(player.getGamePreferences() != null ? 
                    new ArrayList<>(player.getGamePreferences()) : new ArrayList<>())
                .email(player.getEmail())
                .address(player.getAddress())
                .rank(player.getRank())
                .exp(player.getExp())
                .build();
    }
}

