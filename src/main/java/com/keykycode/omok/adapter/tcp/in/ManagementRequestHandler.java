package com.keykycode.omok.adapter.tcp.in;

import com.keykycode.omok.adapter.tcp.dto.Request.LoginRequest;
import com.keykycode.omok.adapter.tcp.net.Connection;
import com.keykycode.omok.application.port.in.ListRooms;
import com.keykycode.omok.application.port.in.Login;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ManagementRequestHandler {

    private static final int ROOM_LIST_LIMIT = 1000;

    private final Login login;
    private final ListRooms listRooms;

    public void onLogin(Connection connection, LoginRequest loginRequest) {
        login.login(connection.getId(), loginRequest.nickname());
    }

    public void onRoomList(Connection connection) {
        listRooms.listRooms(connection.getPlayerId(), ROOM_LIST_LIMIT);
    }
}
