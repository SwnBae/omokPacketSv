package com.keykycode.omok.adapter.tcp.net;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ConnectionRegistry {

    private final ConcurrentMap<Long, Connection> connectionsById = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Connection> connectionsByPlayerId = new ConcurrentHashMap<>();

    /**
     * 연결 번호 기준: accept 스레드가 등록, 담당 I/O 루프가 닫을 때 제거
     */
    public void addByConnectionId(Connection connection) {
        connectionsById.put(connection.getId(), connection);
    }

    public Optional<Connection> findByConnectionId(long connectionId) {
        return Optional.ofNullable(connectionsById.get(connectionId));
    }

    public void removeByConnectionId(long connectionId) {
        connectionsById.remove(connectionId);
    }

    /**
     * playerId 기준: 로그인(bind) 때 등록, Player 제거 때 해제
     */
    public void bindPlayer(long playerId, Connection connection) {
        connectionsByPlayerId.put(playerId, connection);
    }

    public Optional<Connection> findByPlayerId(long playerId) {
        return Optional.ofNullable(connectionsByPlayerId.get(playerId));
    }

    public void unbindPlayer(long playerId) {
        connectionsByPlayerId.remove(playerId);
    }
}
