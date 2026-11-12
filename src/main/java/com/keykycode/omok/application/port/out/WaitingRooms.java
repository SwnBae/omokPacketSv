package com.keykycode.omok.application.port.out;

import com.keykycode.omok.domain.room.RoomSummary;

public interface WaitingRooms {

    /** 같은 방이 이미 있으면 통째로 교체한다. */
    void put(RoomSummary summary);

    void remove(long roomId);

}
