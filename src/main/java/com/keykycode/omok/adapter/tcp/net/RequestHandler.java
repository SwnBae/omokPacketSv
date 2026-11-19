package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.dto.Request;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;

/**
 * 연결마다 하나씩 만든다(Connection을 들고 있어서 공유할 수 없다).
 * ChannelPipeline의 두 번째 핸들러: PacketFrameDecoder가 넘긴 Request를 Router로 전달하고, 연결이 닫히면 정리한다.
 */
@RequiredArgsConstructor
public final class RequestHandler extends SimpleChannelInboundHandler<Request> {

    private final Connection connection;
    private final ConnectionEvents events;
    private final ConnectionRegistry registry;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) {
        //라우팅
        events.onRequest(connection, request);
    }

    // 연결이 닫히면 호출된다(상대가 끊음, 우리가 close, I/O 오류 뒤).
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (connection.markClosed()) {
            registry.removeByConnectionId(connection.getId());
            events.onClosed(connection);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
