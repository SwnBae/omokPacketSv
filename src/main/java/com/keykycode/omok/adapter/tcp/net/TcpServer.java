package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.codec.PacketDecoder;
import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public final class TcpServer {

    private final int port;
    private final ConnectionRegistry connectionRegistry;
    private final List<IoLoop> loops = new ArrayList<>();

    private ServerSocket serverSocket;

    // accept 스레드 전용
    private long nextConnectionId = 1;
    private int nextLoopIndex = 0;

    public TcpServer(int port, int ioThreads, ConnectionRegistry connectionRegistry,
                     ConnectionEvents connectionEvents, PacketDecoder packetDecoder, PacketEncoder packetEncoder) {
        this.port = port;
        this.connectionRegistry = connectionRegistry;

        for (int i = 0; i < ioThreads; i++) {
            loops.add(new IoLoop(connectionEvents, packetDecoder, packetEncoder, connectionRegistry));
        }
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);

        // IO 스레드
        for (int i = 0; i < loops.size(); i++) {
            new Thread(loops.get(i), "io-loop-" + i).start();
        }

        // accept 스레드
        new Thread(this::acceptLoop, "accept").start();
    }


    public int getPort() {
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                if (serverSocket.isClosed()) return;
                continue;
            }

            Connection connection = new Connection(nextConnectionId++, socket);
            connectionRegistry.addByConnectionId(connection);
            loops.get(nextLoopIndex).assign(connection);

            nextLoopIndex = (nextLoopIndex + 1) % loops.size(); // -> 공평하게, 적은 커넥션 쪽으로, AtomicInteger
        }
    }
}
