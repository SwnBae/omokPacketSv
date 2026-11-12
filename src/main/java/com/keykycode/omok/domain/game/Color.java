package com.keykycode.omok.domain.game;

public enum Color {
    BLACK,
    WHITE;

    public Color opposite() {
        return this == BLACK ? WHITE : BLACK;
    }
}
