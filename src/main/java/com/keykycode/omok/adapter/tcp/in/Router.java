package com.keykycode.omok.adapter.tcp.in;

import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;
import com.keykycode.omok.adapter.tcp.codec.ResultCodes;
import com.keykycode.omok.adapter.tcp.concurrent.Worker;
import com.keykycode.omok.adapter.tcp.dto.Request;
import com.keykycode.omok.adapter.tcp.net.Connection;
import com.keykycode.omok.adapter.tcp.net.ConnectionEvents;
import com.keykycode.omok.adapter.tcp.net.ConnectionRegistry;
import com.keykycode.omok.application.port.in.CurrentRoomQuery;
import com.keykycode.omok.application.port.in.DisconnectOutOfRoom;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class Router implements ConnectionEvents {

    private final ConnectionRegistry connectionRegistry;

    private final Worker managementWorker;
    private final ManagementRequestHandler managementRequestHandler;

    private final List<Worker> roomWorkers;
    private final RoomRequestHandler roomRequestHandler;

    private final CurrentRoomQuery currentRoomQuery;
    private final DisconnectOutOfRoom disconnectOutOfRoom;

    private final PacketEncoder packetEncoder;

    // 방 번호는 1부터 순서대로 발급한다(0은 "방 없음")
    private final AtomicLong lastRoomId = new AtomicLong();

    // 스레드 분배
    @Override
    public void onRequest(Connection connection, Request request) {
        boolean isLogin = request instanceof Request.LoginRequest;
        if (isLogin && connection.isLoggedIn()) {
            connection.send(packetEncoder.failure(request, ResultCodes.ALREADY_LOGGED_IN));
            return;
        }
        if (!isLogin && !connection.isLoggedIn()) {
            connection.send(packetEncoder.failure(request, ResultCodes.LOGIN_REQUIRED));
            return;
        }

        switch (request) {
            case Request.LoginRequest r -> managementWorker.post(() -> managementRequestHandler.onLogin(connection, r));
            case Request.RoomListRequest r -> managementWorker.post(() -> managementRequestHandler.onRoomList(connection));
            case Request.RoomCreateRequest r -> {
                long roomId = lastRoomId.incrementAndGet();
                postToRoomWorker(roomId, () -> roomRequestHandler.onCreate(connection, roomId));
            }
            case Request.RoomJoinRequest r -> postToRoomWorker(r.roomId(), () -> roomRequestHandler.onRequest(connection, r));
            case Request.RoomLeaveRequest r -> postToRoomWorker(roomOf(connection), () -> roomRequestHandler.onRequest(connection, r));
            case Request.GameStartRequest r -> postToRoomWorker(roomOf(connection), () -> roomRequestHandler.onRequest(connection, r));
            case Request.PlaceRequest r -> postToRoomWorker(roomOf(connection), () -> roomRequestHandler.onRequest(connection, r));
            case Request.ResignRequest r -> postToRoomWorker(roomOf(connection), () -> roomRequestHandler.onRequest(connection, r));
        }
    }

    @Override
    public void onClosed(Connection connection) {
        if(!connection.isLoggedIn()) return;

        long playerId = connection.getPlayerId();
        long roomId = currentRoomQuery.currentRoomOf(playerId);

        if(roomId == CurrentRoomQuery.NO_ROOM) {
            disconnectOutOfRoom.disconnectOutOfRoom(playerId);
        } else {
            postToRoomWorker(roomId, () -> roomRequestHandler.onDisconnectInRoom(playerId));
        }

        connectionRegistry.unbindPlayer(playerId);
    }

    // 방 번호를 방 스레드 수로 나눈 나머지가 그 방을 맡은 방 스레드다
    private void postToRoomWorker(long roomId, Runnable task) {
        roomWorkers.get(Math.floorMod(roomId, roomWorkers.size())).post(task);
    }

    // 방이 없으면 NO_ROOM(0)이라 방 스레드 0으로 간다
    private long roomOf(Connection connection) {
        return currentRoomQuery.currentRoomOf(connection.getPlayerId());
    }
}
