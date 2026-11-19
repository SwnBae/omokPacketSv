package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.codec.PacketDecoder;
import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;
import com.keykycode.omok.adapter.tcp.dto.DecodeResult;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

@RequiredArgsConstructor
public final class IoWorker implements Runnable {

    private final ConnectionEvents events;
    private final PacketDecoder decoder;
    private final PacketEncoder encoder;
    private final ConnectionRegistry registry;

//    private final BlockingDeque<Connection> newConnections = new LinkedBlockingDeque<>();
//    private final List<Connection> connections = new ArrayList<>();
//    private int startIndex = 0;

    private final Selector selector = openSelector();
    private final Queue<Connection> writeRequests = new ConcurrentLinkedQueue<>();
    private final Queue<Connection> newConnections = new ConcurrentLinkedQueue<>();

    /* accept 스레드: 새로운 클라이언트 수락 시 호출 */
    public void assign(Connection connection) {
        connection.setOwner(this);
        newConnections.add(connection);
        // 소켓 이벤트 외 스레드의 작업이기 때문에 직접 깨움
        selector.wakeup();
    }

    /* Connection.send()를 호출한 스레드: 쓸 응답이 생겼다. */
    public void requestWrite(Connection connection) {
        writeRequests.add(connection);
        // 소켓 이벤트 외 스레드의 작업이기 때문에 직접 깨움
        selector.wakeup();
    }

    @Override
    public void run() {
        try {
            while (true) {
//                if (connections.isEmpty()) adopt(newConnections.take());
                // 깨울 때까지 sleep
                selector.select();

                // 새 연결이 있다면
                for (Connection c; (c = newConnections.poll()) != null; ) adopt(c);

                // 쓰기 요청이 들어온 커넥션들 flush()
                for (Connection c; (c = writeRequests.poll()) != null; ) {
                    // Connection.send() 가능으로 상태 변경
                    c.clearWriteRequested();
                    flush(c);
                }

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    Connection connection = (Connection) key.attachment();
                    if (key.isValid() && key.isReadable()) read(connection);
                    // OP_WRITE인 상태 -> 쓰다가 버퍼가 부족해서 다 못썼는데, 이어서 버퍼에 쓸 수 있는 상태
                    if (key.isValid() && key.isWritable()) flush(connection);
                }
//                List<Connection> arrived = new ArrayList<>();
//                newConnections.drainTo(arrived);
//                arrived.forEach(this::adopt);

//                serveRound();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void adopt(Connection connection) {
        try {
//            connection.getSocket().setSoTimeout(READ_TIMEOUT_MILLIS);
//            connections.add(connection);
            SocketChannel socketChannel = connection.getSocketChannel();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ, connection);
        } catch (IOException e) {
            close(connection);
        }
    }

//    private void serveRound() {
//        if (connections.isEmpty()) return;
//
//        int size = connections.size();
//        startIndex = (startIndex + 1) % size;
//
//        for (int i = 0; i < size; i++) {
//            Connection connection = connections.get((startIndex + i) % size);
//
//            read(connection);
//            write(connection);
//        }
//        connections.removeIf(Connection::isClosed);
//    }

    private void read(Connection connection) {
        if (connection.isClosed() || connection.isCloseAfterFlush()) return;

        try {
            /**
             * read(): 커널 수신 버퍼 → receiveBuffer로 복사하는 과정
             * -1 -> 상대가 연결을 끊은 상태
             */
            if (connection.getSocketChannel().read(connection.getReceiveBuffer()) < 0) {
                close(connection);
                return;
            }
        } catch (IOException e) {
            close(connection);
            return;
        }

        while (true) {
            switch (decode(connection)) {
                case DecodeResult.Incomplete incomplete -> {
                    return; // 남으면 다음 수신 이벤트에 채움
                }
                case DecodeResult.Decoded decoded -> events.onRequest(connection, decoded.request()); // 라우팅
                case DecodeResult.Malformed malformed -> {
                    // 에러 NFT send()
                    connection.send(encoder.errorNtf());
                    connection.markCloseAfterFlush();
                    // 해당 연결의 OP_READ 관심을 끈다 -> 닫기 직전까지, Selector가 읽기 준비 상태로 계속 주는 것을 막는다.
                    interest(connection, SelectionKey.OP_READ, false);
                    return;
                }
            }
        }
//        DecodeResult result = decode(connection);
//
//        if (result instanceof DecodeResult.Incomplete) {
//            if (!fill(connection)) return;
//
//            result = decode(connection);
//        }
//
//        switch (result) {
//            case DecodeResult.Incomplete incomplete -> { } // 남으면 다음 바퀴에 채움
//            case DecodeResult.Decoded decoded -> events.onRequest(connection, decoded.request()); // 라우팅
//            case DecodeResult.Malformed malformed -> {
//                connection.send(encoder.errorNtf());
//                connection.markCloseAfterFlush();
//            }
//        }
    }

    private DecodeResult decode(Connection connection) {
        ByteBuffer buffer = connection.getReceiveBuffer();
        buffer.flip();
        DecodeResult result = decoder.decode(buffer);
        buffer.compact();
        return result;
    }

    private void flush(Connection connection) {
        if (connection.isClosed()) return;

        try {
            for (ByteBuffer out; (out = connection.nextWrite()) != null; ) {
                connection.getSocketChannel().write(out);

                /**
                 * 소켓 송신 버퍼에 더 이상 쓸 공간이 없다면, OP_WRITE 감시 진행
                 * 이후 버퍼에 공간이 생기면 Selector가 알려준다. -> run의 두 번째 flush와 연관
                 */
                if (out.hasRemaining()) {
                    interest(connection, SelectionKey.OP_WRITE, true);
                    return;
                }
                connection.writeDone();
            }
        } catch (IOException e) {
            close(connection);
            return;
        }

        interest(connection, SelectionKey.OP_WRITE, false);
        if (connection.isCloseAfterFlush()) close(connection);
    }

    private void interest(Connection connection, int operation, boolean on) {
        SelectionKey key = connection.getSocketChannel().keyFor(selector);
        if (key == null || !key.isValid()) return;

        // 해당 옵션(operation)만 on/off
        key.interestOps(on ? key.interestOps() | operation : key.interestOps() & ~operation);
    }

//    private boolean fill(Connection connection) {
//        ByteBuffer buffer = connection.getReceiveBuffer();
//        try {
//            int count = connection.getSocket().getInputStream().read(
//                    buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
//
//            if (count < 0) {
//                close(connection);
//                return false;
//            }
//            buffer.position(buffer.position() + count);
//            return true;
//        } catch (SocketTimeoutException e) {
//            return true;
//        } catch (IOException e) {
//            close(connection);
//            return false;
//        }
//    }

//    private void write(Connection connection) {
//        if (connection.isClosed()) return;
//
//        try {
//            OutputStream out = null;
//
//            // 송신 대기 큐에서 패킷 꺼내서 OutputStream에 적재
//            for (byte[] packet; (packet = connection.pollSend()) != null; ) {
//                if (out == null) out = connection.getSocket().getOutputStream();
//
//                out.write(packet);
//            }
//        } catch (IOException e) {
//            close(connection);
//            return;
//        }
//
//        if (connection.isCloseAfterFlush()) close(connection);
//    }

    private void close(Connection connection) {
        if (!connection.markClosed()) return;

        try {
//            connection.getSocket().close();
            connection.getSocketChannel().close();
        } catch (IOException ignored) {
        }

        registry.removeByConnectionId(connection.getId());
        events.onClosed(connection);
    }

    private static Selector openSelector() {
        try {
            return Selector.open();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
