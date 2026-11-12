package com.keykycode.omok.adapter.tcp.codec;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum PacketType {
    LOGIN_REQ(0x01, true),
    LOGIN_RES(0x02, false),

    ROOM_CREATE_REQ(0x10, true),
    ROOM_CREATE_RES(0x11, false),
    ROOM_JOIN_REQ(0x12, true),
    ROOM_JOIN_RES(0x13, false),
    ROOM_JOIN_NTF(0x14, false),
    ROOM_LEAVE_REQ(0x15, true),
    ROOM_LEAVE_RES(0x16, false),
    ROOM_LEAVE_NTF(0x17, false),
    ROOM_LIST_REQ(0x18, true),
    ROOM_LIST_RES(0x19, false),

    GAME_START_REQ(0x20, true),
    GAME_START_RES(0x21, false),
    GAME_START_NTF(0x22, false),

    PLACE_REQ(0x30, true),
    PLACE_RES(0x31, false),
    PLACE_NTF(0x32, false),
    RESIGN_REQ(0x33, true),
    RESIGN_RES(0x34, false),
    GAME_OVER_NTF(0x35, false),

    ERROR_NTF(0x7F, false);

    private final int code;
    private final boolean request;

    public static Optional<PacketType> fromCode(int code) {
        for (PacketType type : values()) {
            if (type.code == code) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
