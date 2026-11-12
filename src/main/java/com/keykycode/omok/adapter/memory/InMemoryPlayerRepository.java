package com.keykycode.omok.adapter.memory;

import com.keykycode.omok.application.port.out.PlayerRepository;
import com.keykycode.omok.domain.player.Player;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPlayerRepository implements PlayerRepository {

    private final Map<Long, Player> playersById = new ConcurrentHashMap<>();

    @Override
    public void add(Player player) {
        playersById.put(player.getPlayerId(), player);
    }

    @Override
    public Optional<Player> findById(long playerId) {
        return Optional.ofNullable(playersById.get(playerId));
    }

    @Override
    public void remove(long playerId) {
        playersById.remove(playerId);
    }
}
