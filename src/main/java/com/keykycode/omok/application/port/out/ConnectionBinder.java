package com.keykycode.omok.application.port.out;

public interface ConnectionBinder {

    /** 커넥션 ID의 연결에 playerId를 묶는다. 이미 닫혀 있으면 묶지 않고 false. */
    boolean bind(long connectionId, long playerId);

}
