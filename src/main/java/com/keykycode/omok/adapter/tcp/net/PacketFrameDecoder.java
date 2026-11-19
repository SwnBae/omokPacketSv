package com.keykycode.omok.adapter.tcp.net;

import com.keykycode.omok.adapter.tcp.codec.PacketDecoder;
import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;
import com.keykycode.omok.adapter.tcp.dto.DecodeResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 연결마다 하나씩 만든다(누적 버퍼를 갖는 핸들러라 공유할 수 없다). 누적과 compact는 ByteToMessageDecoder가 맡는다.
 * ChannelPipeline의 첫 번째 핸들러
 */
@RequiredArgsConstructor
public final class PacketFrameDecoder extends ByteToMessageDecoder {

    private final PacketDecoder decoder;
    private final PacketEncoder encoder;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // nioBuffer()는 readerIndex를 옮기지 않는다. 소비한 만큼은 아래에서 직접 skipBytes로 반영한다.
        ByteBuffer view = in.nioBuffer();
        int start = view.position();

        switch (decoder.decode(view)) {
            case DecodeResult.Incomplete incomplete -> { // 남으면 다음 수신 때 이어서 디코딩
                 }
            case DecodeResult.Decoded decoded -> {
                // readerIndex를 읽은 만큼 넘긴다.
                in.skipBytes(view.position() - start);
                // 파이프라인의 다음 핸들러(RequestHandler)로 결과를 넘긴다.
                out.add(decoded.request());
            }
            case DecodeResult.Malformed malformed -> {
                in.skipBytes(in.readableBytes());
                // 닫기 직전까지 더 읽지 않는다. 에러 NTF가 나간 뒤 연결을 닫는다.
                ctx.channel().config().setAutoRead(false);
                ctx.writeAndFlush(Unpooled.wrappedBuffer(encoder.errorNtf()))
                        .addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}
