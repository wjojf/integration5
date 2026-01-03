package com.banditgames.platform.acl.adapter;

import com.banditgames.platform.acl.adapter.dto.PlayerInfo;
import com.banditgames.platform.acl.port.out.PlayerContextPort;
import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.port.in.GetPlayerUseCase;
import com.banditgames.platform.player.port.in.SearchPlayersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Anti-Corruption Layer (ACL) adapter for Player context.
 *
 * This adapter translates between the Player context's domain model
 * and consuming contexts' needs, ensuring loose coupling.
 *
 * The ACL pattern protects consuming contexts from changes in the Player
 * context's internal structure and API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerContextAdapter implements PlayerContextPort {

    private final SearchPlayersUseCase searchPlayersUseCase;
    private final GetPlayerUseCase getPlayerUseCase;

    @Override
    public List<UUID> findPlayerIdsByUsername(String username) {
        log.debug("ACL: Searching for players by username: {}", username);

        // Translate Player context's domain model to consuming context's needs
        Page<Player> playersPage = searchPlayersUseCase.searchPlayers(username, null, Pageable.unpaged());

        return playersPage.getContent().stream()
                .map(Player::getPlayerId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean playerExists(UUID playerId) {
        log.debug("ACL: Checking if player exists: {}", playerId);

        return getPlayerUseCase.getPlayer(playerId).isPresent();
    }

    @Override
    public Optional<PlayerInfo> getPlayerInfo(UUID playerId) {
        log.debug("ACL: Getting player info for player: {}", playerId);

        return getPlayerUseCase.getPlayer(playerId)
                .map(this::toPlayerInfo);
    }

    @Override
    public List<PlayerInfo> getPlayerInfos(List<UUID> playerIds) {
        log.debug("ACL: Getting player infos for {} players", playerIds.size());

        return playerIds.stream()
                .map(getPlayerUseCase::getPlayer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toPlayerInfo)
                .collect(Collectors.toList());
    }

    /**
     * Translates Player domain model to PlayerInfo DTO.
     * Excludes private information (email, address).
     */
    private PlayerInfo toPlayerInfo(Player player) {
        return PlayerInfo.builder()
                .playerId(player.getPlayerId())
                .username(player.getUsername())
                .bio(player.getBio())
                .gamePreferences(player.getGamePreferences())
                .rank(player.getRank())
                .exp(player.getExp())
                .build();
    }
}

