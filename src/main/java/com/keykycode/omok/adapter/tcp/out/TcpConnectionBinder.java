package com.keykycode.omok.adapter.tcp.out;

import com.keykycode.omok.adapter.tcp.net.Connection;
import com.keykycode.omok.adapter.tcp.net.ConnectionRegistry;
import com.keykycode.omok.application.port.out.ConnectionBinder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TcpConnectionBinder implements ConnectionBinder {

    private final ConnectionRegistry connectionRegistry;

    /**
     * 등록, 로그인 표시, 닫힘 확인 순서다. 담당 I/O 루프는 닫을 때 로그인 표시를 읽으므로,
     * 끊김이 먼저면 여기서 닫힘을 보고, 로그인 표시가 먼저면 I/O 루프가 끊김을 처리한다.
     */
    @Override
    public boolean bind(long connectionId, long playerId) {
        Connection connection = connectionRegistry.findByConnectionId(connectionId).orElse(null);
        if (connection == null) return false;

        connectionRegistry.bindPlayer(playerId, connection);
        connection.markLoggedIn(playerId);

        if (connection.isClosed()) {
            connectionRegistry.unbindPlayer(playerId);
            return false;
        }

        return true;
    }
}

