package com.banditgames.platform.lobby.adapter.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity for storing external game instance mappings.
 * 
 * This table stores the relationship between platform lobbies and external game instances.
 * Supports multiple external game types (chess, checkers, etc.) without polluting the lobby table.
 */
@Entity
@Table(name = "lobby_external_game_instances", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"lobby_id"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalGameInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID lobbyId;

    @Column(nullable = false)
    private UUID gameId; // Platform game ID

    @Column(nullable = false, length = 50)
    private String externalGameType; // e.g., "chess", "checkers"

    @Column(nullable = false)
    private UUID externalGameInstanceId; // The actual game instance ID in the external service
}








