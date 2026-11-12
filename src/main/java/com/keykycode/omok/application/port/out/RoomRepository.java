package com.keykycode.omok.application.port.out;

import com.keykycode.omok.domain.room.Room;

import java.util.Optional;

public interface RoomRepository {

    void add(Room room);

    Optional<Room> findById(long roomId);

    void remove(long roomId);

}
