package com.keykycode.omok.adapter.tcp.net;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class Connection {

    private static final int MAX_PACKET_SIZE = 22;

    @Getter
    private final long id;

    @Getter(AccessLevel.PACKAGE)
//    private final Socket socket;
    private final SocketChannel socketChannel;

    private final Queue<byte[]> sendQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean writeRequested = new AtomicBoolean();

    @Getter(AccessLevel.PACKAGE)
    private final ByteBuffer receiveBuffer = ByteBuffer.allocate(MAX_PACKET_SIZE);

    @Getter
    private volatile long playerId;

    @Getter(AccessLevel.PACKAGE)
    private boolean closeAfterFlush;

    @Setter(AccessLevel.PACKAGE)
    private volatile IoWorker owner;

    private ByteBuffer pendingBuffer;

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
        /**
         * 해당 커넥션에 쓰기 요청이 발생한 것을 IO스레드에게 알린다.
         * 이미 쓰기 요청이 있을 경우, IOWorker의 writeRequests에 이 커넥션이 있을 것이기 때문에 무시한다.
         */
        if(writeRequested.compareAndSet(false, true)) owner.requestWrite(this);
    }

    // 덜 쓴 패킷이 있다면 주고, 아니라면 큐에서 다음 패킷을 돌려준다.
    public ByteBuffer nextWrite() {
        if(pendingBuffer == null) {
            byte[] packet = sendQueue.poll();
            if(packet != null) pendingBuffer = ByteBuffer.wrap(packet);
        }

        return pendingBuffer;
    }

    public void writeDone() {
        pendingBuffer = null;
    }

    // 큐를 비우기 전 호출한다. -> 이후 send()가 요청을 IOWorker에게 다시 올릴 수 있게 만든다.
    public void clearWriteRequested() {
        writeRequested.set(false);
    }

//    byte[] pollSend() {
//        return sendQueue.poll();
//    }

    void markCloseAfterFlush() {
        closeAfterFlush = true;
    }
}
