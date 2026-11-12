package com.keykycode.omok.domain.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Player {

    public static final long NO_ROOM = 0L;

    @Getter
    private final long playerId;
    @Getter
    private final Nickname nickname;

    private final AtomicLong currentRoomId = new AtomicLong(NO_ROOM);

    public static Player create(long playerId, Nickname nickname) {
        return new Player(playerId, nickname);
    }

    public long getCurrentRoomId() {
        return currentRoomId.get();
    }

    public boolean enterRoom(long roomId) {
        requireRoomId(roomId);
        return currentRoomId.compareAndSet(NO_ROOM, roomId);
    }

    public boolean leaveRoom(long roomId) {
        requireRoomId(roomId);
        return currentRoomId.compareAndSet(roomId, NO_ROOM);
    }

    private static void requireRoomId(long roomId) {
        if (roomId == NO_ROOM) throw new IllegalArgumentException("존재하지 않는 방입니다.");
    }
}
