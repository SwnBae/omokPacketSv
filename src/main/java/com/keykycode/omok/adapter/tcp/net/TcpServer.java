package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.codec.PacketDecoder;
import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

public class TcpServer {

    private final int port;
    private final int ioThreads;
    private final ConnectionRegistry connectionRegistry;
    private final ConnectionEvents connectionEvents;
    private final PacketDecoder packetDecoder;
    private final PacketEncoder packetEncoder;

    // 기존 acceptThread에서 지정 -> Worker Thread에서 지정, 여러 스레드에서 다루기 때문에 Atomic으로 변경
    private final AtomicLong nextConnectionId = new AtomicLong(1);
    private Channel serverChannel;

    public TcpServer(int port, int ioThreads,
                     ConnectionRegistry connectionRegistry,
                     ConnectionEvents connectionEvents,
                     PacketDecoder packetDecoder,
                     PacketEncoder packetEncoder) {
        this.port = port;
        this.ioThreads = ioThreads;
        this.connectionRegistry = connectionRegistry;
        this.connectionEvents = connectionEvents;
        this.packetDecoder = packetDecoder;
        this.packetEncoder = packetEncoder;
    }

    public void start() throws IOException {
        EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(ioThreads, NioIoHandler.newFactory());

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // worker EventLoop 스레드에서 실행
                        Connection connection = new Connection(nextConnectionId.getAndIncrement(), ch);
                        connectionRegistry.addByConnectionId(connection);
                        ch.pipeline().addLast(
                                new PacketFrameDecoder(packetDecoder, packetEncoder),
                                new RequestHandler(connection, connectionEvents, connectionRegistry));
                    }
                });

        ChannelFuture bindFuture = bootstrap.bind(port).awaitUninterruptibly();
        if (!bindFuture.isSuccess()) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw new IOException("bind 실패: port=" + port, bindFuture.cause());
        }
        serverChannel = bindFuture.channel();
    }

    public int getPort() {
        return ((InetSocketAddress) serverChannel.localAddress()).getPort();
    }
}