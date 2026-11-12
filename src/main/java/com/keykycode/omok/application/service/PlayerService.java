package com.keykycode.omok.application.service;

import com.keykycode.omok.application.port.in.CurrentRoomQuery;
import com.keykycode.omok.application.port.in.DisconnectOutOfRoom;
import com.keykycode.omok.application.port.out.PlayerRepository;
import com.keykycode.omok.domain.player.Player;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerService implements CurrentRoomQuery, DisconnectOutOfRoom {

    private final PlayerRepository playerRepository;

    @Override
    public long currentRoomOf(long playerId) {
        return playerRepository.findById(playerId)
                .map(Player::getCurrentRoomId)
                .orElse(NO_ROOM);
    }

    @Override
    public void disconnectOutOfRoom(long playerId) {
        playerRepository.remove(playerId);
    }
}
