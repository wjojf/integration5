package com.banditgames.platform.player.adapter.persistence;

import com.banditgames.platform.player.domain.Player;
import com.banditgames.platform.player.domain.Rank;
import com.banditgames.platform.player.port.out.DeletePlayerPort;
import com.banditgames.platform.player.port.out.LoadPlayerPort;
import com.banditgames.platform.player.port.out.SavePlayerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlayerPersistenceAdapter implements LoadPlayerPort, SavePlayerPort, DeletePlayerPort {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;

    @Override
    public Optional<Player> findById(UUID playerId) {
        return playerRepository.findById(playerId)
                .map(playerMapper::toDomain);
    }

    @Override
    public List<Player> findPlayerFriends(UUID playerId){
        return playerRepository.findPlayerFriends(playerId).stream()
                .map(playerMapper::toDomain).toList();
    }

    @Override
    public Page<Player> searchPlayers(String username, Rank rank, Pageable pageable) {
        return playerRepository.findAllByUsernameOrRank(username, rank, pageable).map(playerMapper::toDomain);
    }

    @Override
    public Page<Player> searchPlayersForUser(UUID userId, String username, Rank rank, Pageable pageable) {
        return playerRepository.searchPlayersForUser(userId, username, rank, pageable).map(playerMapper::toDomain);
    }

    @Override
    public Player save(Player player) {
        PlayerEntity entity = playerMapper.toEntity(player);
        PlayerEntity saved = playerRepository.save(entity);
        return playerMapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID playerId) {
        playerRepository.deleteById(playerId);
    }
}

