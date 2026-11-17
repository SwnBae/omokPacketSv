package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.codec.PacketDecoder;
import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public final class TcpServer {

    private final int port;
    private final ConnectionRegistry connectionRegistry;
    private final List<IoWorker> ioWorkerList = new ArrayList<>();

//    private ServerSocket serverSocket;
    private ServerSocketChannel serverSocketChannel;

    // accept 스레드 전용
    private long nextConnectionId = 1;
    private int nextLoopIndex = 0;

    public TcpServer(int port, int ioThreads, ConnectionRegistry connectionRegistry,
                     ConnectionEvents connectionEvents, PacketDecoder packetDecoder, PacketEncoder packetEncoder) {
        this.port = port;
        this.connectionRegistry = connectionRegistry;

        for (int i = 0; i < ioThreads; i++) {
            ioWorkerList.add(new IoWorker(connectionEvents, packetDecoder, packetEncoder, connectionRegistry));
        }
    }

    public void start() throws IOException {
//        serverSocket = new ServerSocket(port);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));

        // IO 스레드
        for (int i = 0; i < ioWorkerList.size(); i++) {
            new Thread(ioWorkerList.get(i), "io-" + i).start();
        }

        // accept 스레드
        new Thread(this::acceptLoop, "accept").start();
    }


    public int getPort() {
//        return serverSocket.getLocalPort();
        return serverSocketChannel.socket().getLocalPort();
    }

    private void acceptLoop() {
        while (true) {
//            Socket socket;
            SocketChannel socketChannel;
            try {
//                socket = serverSocket.accept();
                socketChannel = serverSocketChannel.accept();
            } catch (IOException e) {
//                if (serverSocket.isClosed()) return;
                if(!serverSocketChannel.isOpen()) return;
                continue;
            }

//            Connection connection = new Connection(nextConnectionId++, socket);
            Connection connection = new Connection(nextConnectionId++, socketChannel);
            connectionRegistry.addByConnectionId(connection);
            ioWorkerList.get(nextLoopIndex).assign(connection);

            nextLoopIndex = (nextLoopIndex + 1) % ioWorkerList.size(); // -> 공평하게, 적은 커넥션 쪽으로, AtomicInteger
        }
    }
}
