package com.keykycode.omok.application.port.out;

public interface ConnectionStatus {

    /** 등록된 연결이 없거나 닫혀 있으면 false. */
    boolean isConnected(long playerId);

}
