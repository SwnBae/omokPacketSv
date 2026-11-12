package com.keykycode.omok.application.port.out;

import com.keykycode.omok.domain.player.Player;

import java.util.Optional;

public interface PlayerRepository {

    void add(Player player);

    Optional<Player> findById(long playerId);

    void remove(long playerId);

}
