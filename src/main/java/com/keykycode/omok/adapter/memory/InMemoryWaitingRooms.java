package com.keykycode.omok.adapter.memory;

import com.keykycode.omok.application.port.out.WaitingRoomQuery;
import com.keykycode.omok.application.port.out.WaitingRooms;
import com.keykycode.omok.domain.room.RoomSummary;

import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class InMemoryWaitingRooms implements WaitingRooms, WaitingRoomQuery {

    private final ConcurrentNavigableMap<Long, RoomSummary> summariesByRoomId = new ConcurrentSkipListMap<>();

    @Override
    public void put(RoomSummary summary) {
        summariesByRoomId.put(summary.roomId(), summary);
    }

    @Override
    public void remove(long roomId) {
        summariesByRoomId.remove(roomId);
    }

    @Override
    public List<RoomSummary> list(int limit) {
        return summariesByRoomId.descendingMap().values().stream()
                .limit(limit)
                .toList();
    }
}
