package com.keykycode.omok.adapter.tcp.net;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class Connection {

    private static final int MAX_PACKET_SIZE = 22;

    @Getter
    private final long id;

    @Getter(AccessLevel.PACKAGE)
    private final Socket socket;

    private final Queue<byte[]> sendQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    @Getter(AccessLevel.PACKAGE)
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE);

    @Getter
    private volatile long playerId;

    @Getter(AccessLevel.PACKAGE)
    private boolean closeAfterFlush;

    public boolean isLoggedIn() {
        return playerId != 0;
    }

    public void markLoggedIn(long playerId) {
        this.playerId = playerId;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean markClosed() {
        return closed.compareAndSet(false, true);
    }

    public void send(byte[] packet) {
        sendQueue.add(packet);
    }

    byte[] pollSend() {
        return sendQueue.poll();
    }

    void markCloseAfterFlush() {
        closeAfterFlush = true;
    }
}
