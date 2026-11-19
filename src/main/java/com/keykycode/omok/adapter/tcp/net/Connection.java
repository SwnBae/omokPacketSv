package com.keykycode.omok.adapter.tcp.net;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class Connection {

    @Getter
    private final long id;

    private final Channel channel;

    private final AtomicBoolean closed = new AtomicBoolean();

    @Getter
    private volatile long playerId;

    public boolean isLoggedIn() { return playerId != 0; }

    public void markLoggedIn(long playerId) { this.playerId = playerId; }

    public boolean isClosed() { return closed.get(); }

    public boolean markClosed() { return closed.compareAndSet(false, true); }

    // 다른 스레드(Worker)에서 불러도 된다. 쓰기는 이 채널의 EventLoop에서 실행된다.
    public void send(byte[] packet) {
        channel.writeAndFlush(Unpooled.wrappedBuffer(packet));
    }
}