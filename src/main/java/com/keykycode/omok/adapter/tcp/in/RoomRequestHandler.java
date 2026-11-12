package com.keykycode.omok.adapter.tcp.in;

import com.keykycode.omok.adapter.tcp.dto.Request;
import com.keykycode.omok.adapter.tcp.net.Connection;
import com.keykycode.omok.application.port.in.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoomRequestHandler {

    private final CreateRoom createRoom;
    private final JoinRoom joinRoom;
    private final LeaveRoom leaveRoom;

    private final StartGame startGame;
    private final PlaceStone placeStone;
    private final Resign resign;

    private final DisconnectInRoom disconnectInRoom;

    public void onCreate(Connection connection, long roomId) {
        if(connection.isClosed()) return;

        createRoom.createRoom(connection.getPlayerId(), roomId);
    }

    public void onRequest(Connection connection, Request request) {
        if(connection.isClosed()) return;

        long playerId = connection.getPlayerId();

        switch (request) {
            case Request.RoomJoinRequest r -> joinRoom.joinRoom(playerId, r.roomId());
            case Request.RoomLeaveRequest r -> leaveRoom.leaveRoom(playerId);
            case Request.GameStartRequest r -> startGame.startGame(playerId);
            case Request.PlaceRequest r -> placeStone.placeStone(playerId, r.x(), r.y());
            case Request.ResignRequest r -> resign.resign(playerId);
            case Request.RoomCreateRequest r -> throw new IllegalStateException("onCreate로 받을 요청입니다." + r);
            case Request.LoginRequest r -> throw new IllegalStateException("잘못된 요청입니다." + r);
            case Request.RoomListRequest r -> throw new IllegalStateException("잘못된 요청입니다." + r);
        }
    }

    public void onDisconnectInRoom(long playerId) {
        disconnectInRoom.disconnectInRoom(playerId);
    }
}
