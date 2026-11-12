package com.keykycode.omok.application.port.in;

public interface Login {

    void login(long connectionId, byte[] rawNickname);

}