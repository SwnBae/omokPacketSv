package com.keykycode.omok.domain.game;

public sealed interface PlaceResult {

    enum Rejection {
        NOT_YOUR_TURN,
        OUT_OF_RANGE,
        OCCUPIED
    }

    record Rejected(Rejection reason) implements PlaceResult {
    }

    record Placed(int moveNo, Color color, int x, int y, EndReason endReason, Color winner) implements PlaceResult {
        public boolean ended() {
            return endReason != null;
        }
    }
}
