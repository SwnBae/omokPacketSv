package com.keykycode.omok.adapter.tcp.codec;

import com.keykycode.omok.adapter.tcp.dto.DecodeResult;
import com.keykycode.omok.adapter.tcp.dto.Request;

import java.nio.ByteBuffer;
import java.util.Optional;

public class PacketDecoder {

    private static final int HEADER_SIZE = 3;
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 22;
    private static final int LOGIN_LENGTH = 22;
    private static final int JOIN_LENGTH = 7;
    private static final int PLACE_LENGTH = 5;
    private static final int NICK_BYTES = 18;

    private static final DecodeResult INCOMPLETE = new DecodeResult.Incomplete();
    private static final DecodeResult MALFORMED = new DecodeResult.Malformed();

    /* buffer: 읽기 모드(position ~ limit이 수신된 바이트). Decoded일 때만 position이 패킷 길이만큼 전진한다. */
    public DecodeResult decode(ByteBuffer buffer) {
        int base = buffer.position();
        int available = buffer.remaining();

        // 2바이트 미만
        if (available < 2) return INCOMPLETE;

        // len이 3 ~ 22 밖 -> 잘못된 패킷으로 판단
        int length = u16(buffer, base);
        if (length < MIN_LENGTH || length > MAX_LENGTH) return MALFORMED;

        // 헤더 3바이트 확인
        if (available < HEADER_SIZE) return INCOMPLETE;

        // 타입 확인 (요청 타입인지, 길이가 올바른지)
        Optional<PacketType> type = PacketType.fromCode(u8(buffer, base + 2));
        if (type.isEmpty() || !type.get().isRequest()) return MALFORMED;

        if (length != expectedLength(type.get())) return MALFORMED;

        // 패킷 전부 도착했는지 확인
        if (available < length) return INCOMPLETE;

        Request request = parse(type.get(), buffer, base + HEADER_SIZE);
        buffer.position(base + length); // position 패킷 끝으로 이동
        return new DecodeResult.Decoded(request);
    }

    private static Request parse(PacketType type, ByteBuffer buffer, int payload) {
        return switch (type) {
            case LOGIN_REQ -> new Request.LoginRequest(nickname(buffer, payload));
            case ROOM_CREATE_REQ -> new Request.RoomCreateRequest();
            case ROOM_JOIN_REQ -> new Request.RoomJoinRequest(u32(buffer, payload));
            case ROOM_LEAVE_REQ -> new Request.RoomLeaveRequest();
            case ROOM_LIST_REQ -> new Request.RoomListRequest();
            case GAME_START_REQ -> new Request.GameStartRequest();
            case PLACE_REQ -> new Request.PlaceRequest(u8(buffer, payload), u8(buffer, payload + 1));
            case RESIGN_REQ -> new Request.ResignRequest();
            default -> throw new IllegalStateException("요청 타입이 아닙니다: " + type);
        };
    }

    private static int expectedLength(PacketType type) {
        return switch (type) {
            case LOGIN_REQ -> LOGIN_LENGTH;
            case ROOM_JOIN_REQ -> JOIN_LENGTH;
            case PLACE_REQ -> PLACE_LENGTH;
            default -> HEADER_SIZE;
        };
    }

    // nickLen이 18을 넘으면 그 길이의 빈 배열을 돌려 Nickname 검증(result 12)에서 걸리게 한다.
    private static byte[] nickname(ByteBuffer buffer, int payload) {
        int nickLen = u8(buffer, payload);
        byte[] nick = new byte[nickLen];
        if (nickLen <= NICK_BYTES) {
            for (int i = 0; i < nickLen; i++) {
                nick[i] = buffer.get(payload + 1 + i);
            }
        }
        return nick;
    }

    private static int u8(ByteBuffer buffer, int index) {
        return buffer.get(index) & 0xFF;
    }

    private static int u16(ByteBuffer buffer, int index) {
        return (u8(buffer, index) << 8) | u8(buffer, index + 1);
    }

    private static long u32(ByteBuffer buffer, int index) {
        return ((long) u16(buffer, index) << 16) | u16(buffer, index + 2);
    }
}
