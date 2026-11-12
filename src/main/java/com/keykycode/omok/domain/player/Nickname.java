package com.keykycode.omok.domain.player;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Nickname {

    public static final int MIN_BYTES = 2;
    public static final int MAX_BYTES = 18;

    private final byte[] bytes;

    public static Optional<Nickname> of(byte[] raw) {
        if (raw == null || raw.length < MIN_BYTES || raw.length > MAX_BYTES) {
            return Optional.empty();
        }
        return Optional.of(new Nickname(raw));
    }

    public int length() {
        return bytes.length;
    }

    public byte[] toBytes() {
        return bytes.clone();
    }
}
