package com.keykycode.omok.domain.game;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Game {

    @Getter
    private final long blackId;
    @Getter
    private final long whiteId;

    private final Board board = new Board();
    private Color turn = Color.BLACK;

    public static Game create(long blackId, long whiteId) {
        return new Game(blackId, whiteId);
    }

    public Color colorOf(long playerId) {
        if (playerId == blackId) return Color.BLACK;
        if (playerId == whiteId) return Color.WHITE;

        throw new IllegalArgumentException("대국의 참가자가 아닙니다." + playerId);
    }

    public PlaceResult place(long playerId, int x, int y) {
        if (colorOf(playerId) != turn) return new PlaceResult.Rejected(PlaceResult.Rejection.NOT_YOUR_TURN);
        if (!board.isInRange(x, y)) return new PlaceResult.Rejected(PlaceResult.Rejection.OUT_OF_RANGE);
        if (!board.isEmptyAt(x, y)) return new PlaceResult.Rejected(PlaceResult.Rejection.OCCUPIED);

        Color placed = turn;
        board.placeStone(x, y, placed);
        turn = placed.opposite();

        if (board.isWinningMove(x, y)) return new PlaceResult.Placed(board.getStoneCount(), placed, x, y, EndReason.FIVE_OR_MORE, placed);
        if (board.isFull()) return new PlaceResult.Placed(board.getStoneCount(), placed, x, y, EndReason.DRAW, null);
        return new PlaceResult.Placed(board.getStoneCount(), placed, x, y, null, null);
    }
}
