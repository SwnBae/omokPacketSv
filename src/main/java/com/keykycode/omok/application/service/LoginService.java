package com.keykycode.omok.application.service;

import com.keykycode.omok.application.dto.Outcome;
import com.keykycode.omok.application.port.in.Login;
import com.keykycode.omok.application.port.out.ClientNotifier;
import com.keykycode.omok.application.port.out.ConnectionBinder;
import com.keykycode.omok.application.port.out.PlayerIdGenerator;
import com.keykycode.omok.application.port.out.PlayerRepository;
import com.keykycode.omok.domain.player.Nickname;
import com.keykycode.omok.domain.player.Player;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class LoginService implements Login {

    private final PlayerRepository playerRepository;
    private final PlayerIdGenerator playerIdGenerator;
    private final ConnectionBinder connectionBinder;
    private final ClientNotifier clientNotifier;

    @Override
    public void login(long connectionId, byte[] rawNickname) {
        Optional<Nickname> nickname = Nickname.of(rawNickname);

        // of의 유효성 검사 실패 시 empty
        if (nickname.isEmpty()) {
            clientNotifier.loginRejected(connectionId, Outcome.INVALID_NICKNAME);
            return;
        }

        long playerId = playerIdGenerator.next();
        playerRepository.add(Player.create(playerId, nickname.get()));

        if(!connectionBinder.bind(connectionId, playerId)) {
            playerRepository.remove(playerId);
            return;
        }
        clientNotifier.loginSucceeded(playerId);
    }
}
