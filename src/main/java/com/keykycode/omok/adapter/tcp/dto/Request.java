package com.keykycode.omok.adapter.tcp.dto;

public sealed interface Request {

    record LoginRequest(byte[] nickname) implements Request {
    }

    record RoomCreateRequest() implements Request {
    }

    record RoomJoinRequest(long roomId) implements Request {
    }

    record RoomLeaveRequest() implements Request {
    }

    record RoomListRequest() implements Request {
    }

    record GameStartRequest() implements Request {
    }

    record PlaceRequest(int x, int y) implements Request {
    }

    record ResignRequest() implements Request {
    }
}
