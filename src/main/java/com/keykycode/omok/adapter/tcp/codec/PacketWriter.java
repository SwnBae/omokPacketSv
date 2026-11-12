package com.keykycode.omok.adapter.tcp.codec;

import com.keykycode.omok.domain.player.Nickname;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/** 패킷 만드는 헬퍼 클래스*/
class PacketWriter {
    private static final int HEADER_SIZE = 3;

    private final PacketType type;
    private final ByteArrayOutputStream payload = new ByteArrayOutputStream();

    PacketWriter(PacketType type) {
        this.type = type;
    }

    PacketWriter u8(int value) {
        payload.write(value);
        return this;
    }

    PacketWriter u16(int value) {
        return u8(value >>> 8).u8(value);
    }

    PacketWriter u32(long value) {
        return u16((int) (value >>> 16)).u16((int) value);
    }

    /** nickLen(1B) + nickBytes(18B, 뒤는 00 패딩) */
    PacketWriter nick(Nickname nickname) {
        byte[] bytes = nickname.toBytes();
        u8(bytes.length);
        payload.writeBytes(bytes);
        payload.writeBytes(new byte[Nickname.MAX_BYTES - bytes.length]);
        return this;
    }

    byte[] build() {
        byte[] body = payload.toByteArray();
        return ByteBuffer.allocate(HEADER_SIZE + body.length)
                .putShort((short) (HEADER_SIZE + body.length))
                .put((byte) type.getCode())
                .put(body)
                .array();
    }
}
