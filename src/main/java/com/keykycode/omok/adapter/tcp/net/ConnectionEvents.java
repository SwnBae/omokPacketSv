package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.dto.Request;

/* Router 구현 */
public interface ConnectionEvents {

    void onRequest(Connection connection, Request request);

    void onClosed(Connection connection);
}
