package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.codec.PacketDecoder;
import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;
import com.keykycode.omok.adapter.tcp.dto.DecodeResult;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@RequiredArgsConstructor
public final class IoLoop implements Runnable {

    private static final int READ_TIMEOUT_MILLIS = 1;

    private final ConnectionEvents events;
    private final PacketDecoder decoder;
    private final PacketEncoder encoder;
    private final ConnectionRegistry registry;

    private final BlockingDeque<Connection> newConnections = new LinkedBlockingDeque<>();

    private final List<Connection> connections = new ArrayList<>();
    private int startIndex = 0;

    /* accept 스레드: 새로운 클라이언트 수락 시 호출 */
    public void assign(Connection connection) {
        newConnections.add(connection);
    }

    @Override
    public void run() {
        try {
            while (true) {
                // 블로킹 큐의 take 연산이므로, 비어있다면 newConnections에 새 연결이 올 때 까지 대기한다.
                if (connections.isEmpty()) adopt(newConnections.take());

                List<Connection> arrived = new ArrayList<>();
                newConnections.drainTo(arrived);
                arrived.forEach(this::adopt);

                serveRound();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void adopt(Connection connection) {
        try {
            connection.getSocket().setSoTimeout(READ_TIMEOUT_MILLIS);
            connections.add(connection);
        } catch (IOException e) {
            close(connection);
        }
    }

    private void serveRound() {
        if (connections.isEmpty()) return;

        int size = connections.size();
        startIndex = (startIndex + 1) % size;

        for (int i = 0; i < size; i++) {
            Connection connection = connections.get((startIndex + i) % size);

            read(connection);
            write(connection);
        }
        connections.removeIf(Connection::isClosed);
    }

    private void read(Connection connection) {
        if (connection.isClosed() || connection.isCloseAfterFlush()) return;

        DecodeResult result = decode(connection);

        if (result instanceof DecodeResult.Incomplete) {
            if (!fill(connection)) return;

            result = decode(connection);
        }

        switch (result) {
            case DecodeResult.Incomplete incomplete -> { } // 남으면 다음 바퀴에 채움
            case DecodeResult.Decoded decoded -> events.onRequest(connection, decoded.request()); // 라우팅
            case DecodeResult.Malformed malformed -> {
                connection.send(encoder.errorNtf());
                connection.markCloseAfterFlush();
            }
        }
    }

    private DecodeResult decode(Connection connection) {
        ByteBuffer buffer = connection.getReceiveBuffer();
        buffer.flip();
        DecodeResult result = decoder.decode(buffer);
        buffer.compact();
        return result;
    }

    private boolean fill(Connection connection) {
        ByteBuffer buffer = connection.getReceiveBuffer();
        try {
            int count = connection.getSocket().getInputStream().read(
                    buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());

            if (count < 0) {
                close(connection);
                return false;
            }
            buffer.position(buffer.position() + count);
            return true;
        } catch (SocketTimeoutException e) {
            return true;
        } catch (IOException e) {
            close(connection);
            return false;
        }
    }

    private void write(Connection connection) {
        if (connection.isClosed()) return;

        try {
            OutputStream out = null;

            // 송신 대기 큐에서 패킷 꺼내서 OutputStream에 적재
            for (byte[] packet; (packet = connection.pollSend()) != null; ) {
                if (out == null) out = connection.getSocket().getOutputStream();

                out.write(packet);
            }
        } catch (IOException e) {
            close(connection);
            return;
        }

        if (connection.isCloseAfterFlush()) close(connection);
    }

    private void close(Connection connection) {
        if (!connection.markClosed()) return;

        try {
            connection.getSocket().close();
        } catch (IOException ignored) {
        }

        registry.removeByConnectionId(connection.getId());
        events.onClosed(connection);
    }
}
