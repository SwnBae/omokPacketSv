package com.keykycode.omok.application.port.out;

import com.keykycode.omok.domain.room.RoomSummary;

import java.util.List;

public interface WaitingRoomQuery {

    List<RoomSummary> list(int limit);

}
