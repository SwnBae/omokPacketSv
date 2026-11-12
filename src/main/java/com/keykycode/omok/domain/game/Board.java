package com.keykycode.omok.domain.game;

import lombok.Getter;

public class Board {

    public static final int SIZE = 15;

    private static final int[][] directions = new int[][]{{0, 1}, {1, 0}, {1, 1}, {1, -1}};

    private final Color[][] cells = new Color[SIZE][SIZE];

    @Getter
    private int stoneCount;

    public boolean isInRange(int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE;
    }

    public boolean isEmptyAt(int x, int y) {
        return cells[x][y] == null;
    }

    public void placeStone(int x, int y, Color color) {
        cells[x][y] = color;
        stoneCount++;
    }

    public boolean isFull() {
        return stoneCount == SIZE * SIZE;
    }

    public boolean isWinningMove(int x, int y) {
        Color color = cells[x][y];
        for (int[] direction : directions) {
            int count = 1 + countSameColor(x, y, direction[0], direction[1], color)
                    + countSameColor(x, y, -direction[0], -direction[1], color);
            if (count >= 5) return true;
        }
        return false;
    }

    private int countSameColor(int x, int y, int dx, int dy, Color color) {
        int count = 0;
        int cx = x + dx;
        int cy = y + dy;

        while (isInRange(cx, cy) && cells[cx][cy] == color) {
            count++;
            cx += dx;
            cy += dy;
        }
        return count;
    }
}
