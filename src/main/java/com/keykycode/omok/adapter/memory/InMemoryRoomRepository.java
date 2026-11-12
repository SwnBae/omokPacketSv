package com.keykycode.omok.adapter.memory;

import com.keykycode.omok.application.port.out.RoomRepository;
import com.keykycode.omok.domain.room.Room;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRoomRepository implements RoomRepository {

    private final Map<Long, Room> roomsById = new ConcurrentHashMap<>();

    @Override
    public void add(Room room) {
        roomsById.put(room.getRoomId(), room);
    }

    @Override
    public Optional<Room> findById(long roomId) {
        return Optional.ofNullable(roomsById.get(roomId));
    }

    @Override
    public void remove(long roomId) {
        roomsById.remove(roomId);
    }
}
