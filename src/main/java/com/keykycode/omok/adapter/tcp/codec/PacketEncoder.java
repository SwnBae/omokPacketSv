package com.keykycode.omok.adapter.tcp.codec;

import com.keykycode.omok.adapter.tcp.dto.Request;
import com.keykycode.omok.application.dto.RequestKind;
import com.keykycode.omok.domain.game.Color;
import com.keykycode.omok.domain.game.EndReason;
import com.keykycode.omok.domain.player.Nickname;
import com.keykycode.omok.domain.room.RoomSummary;

import java.util.List;

public class PacketEncoder {

    // ---------- RES ----------

    public byte[] loginRes(long playerId) {
        return new PacketWriter(PacketType.LOGIN_RES).u8(ResultCodes.OK).u32(playerId).build();
    }

    public byte[] roomCreateRes(long roomId) {
        return new PacketWriter(PacketType.ROOM_CREATE_RES).u8(ResultCodes.OK).u32(roomId).build();
    }

    public byte[] roomJoinRes(long roomId, long hostId, Nickname hostNickname) {
        return new PacketWriter(PacketType.ROOM_JOIN_RES)
                .u8(ResultCodes.OK).u32(roomId).u32(hostId).nick(hostNickname).build();
    }

    public byte[] roomLeaveRes() {
        return resultOnly(PacketType.ROOM_LEAVE_RES, ResultCodes.OK);
    }

    public byte[] roomListRes(List<RoomSummary> rooms) {
        PacketWriter writer = new PacketWriter(PacketType.ROOM_LIST_RES)
                .u8(ResultCodes.OK).u16(rooms.size());
        for (RoomSummary room : rooms) {
            writer.u32(room.roomId()).nick(room.hostNickname()).u8(room.playerCount());
        }
        return writer.build();
    }

    // ---------- NTF ----------

    public byte[] roomJoinNtf(long playerId, Nickname nickname) {
        return new PacketWriter(PacketType.ROOM_JOIN_NTF).u32(playerId).nick(nickname).build();
    }

    public byte[] roomLeaveNtf(long playerId) {
        return new PacketWriter(PacketType.ROOM_LEAVE_NTF).u32(playerId).build();
    }

    public byte[] gameStartNtf(long blackId, long whiteId) {
        return new PacketWriter(PacketType.GAME_START_NTF).u32(blackId).u32(whiteId).build();
    }

    public byte[] placeNtf(int moveNo, Color color, int x, int y) {
        return new PacketWriter(PacketType.PLACE_NTF)
                .u16(moveNo).u8(colorCode(color)).u8(x).u8(y).build();
    }

    // winner가 null이면 무승부(0)
    public byte[] gameOverNtf(EndReason reason, Color winner) {
        int winnerCode = winner == null ? 0 : colorCode(winner);
        return new PacketWriter(PacketType.GAME_OVER_NTF).u8(reasonCode(reason)).u8(winnerCode).build();
    }

    public byte[] errorNtf() {
        return new PacketWriter(PacketType.ERROR_NTF).u8(ResultCodes.FORMAT_ERROR).build();
    }

    // ---------- 실패 RES(result != 0) ----------

    /** 로그인 실패. 로그인 전이라 RequestKind가 없음 */
    public byte[] loginFailure(int result) {
        return failureOnly(PacketType.LOGIN_RES, result);
    }

    public byte[] failure(Request request, int result) {
        return failureOnly(responseTypeOf(request), result);
    }

    public byte[] failure(RequestKind kind, int result) {
        return failureOnly(responseTypeOf(kind), result);
    }

    private static PacketType responseTypeOf(Request request) {
        return switch (request) {
            case Request.LoginRequest r -> PacketType.LOGIN_RES;
            case Request.RoomCreateRequest r -> PacketType.ROOM_CREATE_RES;
            case Request.RoomJoinRequest r -> PacketType.ROOM_JOIN_RES;
            case Request.RoomLeaveRequest r -> PacketType.ROOM_LEAVE_RES;
            case Request.RoomListRequest r -> PacketType.ROOM_LIST_RES;
            case Request.GameStartRequest r -> PacketType.GAME_START_RES;
            case Request.PlaceRequest r -> PacketType.PLACE_RES;
            case Request.ResignRequest r -> PacketType.RESIGN_RES;
        };
    }

    private static PacketType responseTypeOf(RequestKind kind) {
        return switch (kind) {
            case CREATE_ROOM -> PacketType.ROOM_CREATE_RES;
            case JOIN_ROOM -> PacketType.ROOM_JOIN_RES;
            case LEAVE_ROOM -> PacketType.ROOM_LEAVE_RES;
            case START_GAME -> PacketType.GAME_START_RES;
            case PLACE_STONE -> PacketType.PLACE_RES;
            case RESIGN -> PacketType.RESIGN_RES;
        };
    }

    // ---------- 공통 ----------

    private static byte[] failureOnly(PacketType type, int result) {
        if (result == ResultCodes.OK) {
            throw new IllegalArgumentException("잘못된 응답입니다.");
        }
        return resultOnly(type, result);
    }

    private static byte[] resultOnly(PacketType type, int result) {
        return new PacketWriter(type).u8(result).build();
    }

    private static int colorCode(Color color) {
        return switch (color) {
            case BLACK -> 1;
            case WHITE -> 2;
        };
    }

    private static int reasonCode(EndReason reason) {
        return switch (reason) {
            case FIVE_OR_MORE -> 1;
            case RESIGN -> 2;
            case DISCONNECT -> 3;
            case DRAW -> 4;
        };
    }
}
