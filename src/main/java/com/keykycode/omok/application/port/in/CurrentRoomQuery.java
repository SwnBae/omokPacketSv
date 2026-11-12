package com.keykycode.omok.application.port.in;

public interface CurrentRoomQuery {

    long NO_ROOM = 0L;

    long currentRoomOf(long playerId);

}
