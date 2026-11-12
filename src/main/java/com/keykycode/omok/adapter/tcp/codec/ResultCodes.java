package com.keykycode.omok.adapter.tcp.codec;

import com.keykycode.omok.application.dto.Outcome;

public class ResultCodes {
    /**
     * 1번, 10번 (로그인 관련)은 서비스가 판단하지 않고, 어뎁터가 판단합니다.
     * 나머지 에러는 어플리케이션 레벨에서 판단합니다.
     */
    public static final int OK = 0;
    public static final int ALREADY_LOGGED_IN = 1;
    public static final int LOGIN_REQUIRED = 10;
    public static final int FORMAT_ERROR = 1;

    private ResultCodes() {
    }

    public static int of(Outcome outcome) {
        return switch (outcome) {
            case ROOM_NOT_FOUND -> 2;
            case ROOM_NOT_JOINABLE -> 3;
            case NOT_YOUR_TURN -> 4;
            case OCCUPIED -> 5;
            case OUT_OF_RANGE -> 6;
            case NOT_PLAYING -> 7;
            case ALREADY_IN_ROOM -> 8;
            case NOT_IN_ROOM -> 9;
            case ALREADY_PLAYING -> 11;
            case INVALID_NICKNAME -> 12;
            case NOT_HOST -> 13;
            case NO_OPPONENT -> 14;
        };
    }
}
