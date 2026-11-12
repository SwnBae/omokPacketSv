package com.keykycode.omok.adapter.tcp.out;

import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;
import com.keykycode.omok.adapter.tcp.codec.ResultCodes;
import com.keykycode.omok.adapter.tcp.net.Connection;
import com.keykycode.omok.adapter.tcp.net.ConnectionRegistry;
import com.keykycode.omok.application.dto.Outcome;
import com.keykycode.omok.application.dto.RequestKind;
import com.keykycode.omok.application.port.out.ClientNotifier;
import com.keykycode.omok.application.port.out.ConnectionStatus;
import com.keykycode.omok.domain.game.Color;
import com.keykycode.omok.domain.game.EndReason;
import com.keykycode.omok.domain.player.Nickname;
import com.keykycode.omok.domain.room.RoomSummary;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TcpClientNotifier implements ClientNotifier, ConnectionStatus {

    private final ConnectionRegistry connectionRegistry;
    private final PacketEncoder packetEncoder;

    // ---------- ConnectionStatus ----------

    @Override
    public boolean isConnected(long playerId) {
        return connectionRegistry.findByPlayerId(playerId)
                .map(connection -> !connection.isClosed())
                .orElse(false);
    }

    // ---------- RES ----------

    @Override
    public void loginSucceeded(long playerId) {
        sendToPlayer(playerId, packetEncoder.loginRes(playerId));
    }

    @Override
    public void loginRejected(long connectionId, Outcome outcome) {
        Connection connection = connectionRegistry.findByConnectionId(connectionId).orElse(null);
        deliver(connection, packetEncoder.loginFailure(ResultCodes.of(outcome)));
    }

    @Override
    public void roomCreated(long recipientId, long roomId) {
        sendToPlayer(recipientId, packetEncoder.roomCreateRes(roomId));
    }

    @Override
    public void roomJoined(long recipientId, long roomId, long hostId, Nickname hostNickname) {
        sendToPlayer(recipientId, packetEncoder.roomJoinRes(roomId, hostId, hostNickname));
    }

    @Override
    public void roomLeft(long recipientId) {
        sendToPlayer(recipientId, packetEncoder.roomLeaveRes());
    }

    @Override
    public void roomList(long recipientId, List<RoomSummary> summaries) {
        sendToPlayer(recipientId, packetEncoder.roomListRes(summaries));
    }

    @Override
    public void rejected(long recipientId, RequestKind kind, Outcome outcome) {
        sendToPlayer(recipientId, packetEncoder.failure(kind, ResultCodes.of(outcome)));
    }

    // ---------- NTF ----------

    @Override
    public void opponentJoined(long recipientId, long newcomerId, Nickname nickname) {
        sendToPlayer(recipientId, packetEncoder.roomJoinNtf(newcomerId, nickname));
    }

    @Override
    public void opponentLeft(long recipientId, long leaverId) {
        sendToPlayer(recipientId, packetEncoder.roomLeaveNtf(leaverId));
    }

    @Override
    public void gameStarted(long recipientId, long blackId, long whiteId) {
        sendToPlayer(recipientId, packetEncoder.gameStartNtf(blackId, whiteId));
    }

    @Override
    public void stonePlaced(long recipientId, int moveNo, Color color, int x, int y) {
        sendToPlayer(recipientId, packetEncoder.placeNtf(moveNo, color, x, y));
    }

    @Override
    public void gameOver(long recipientId, EndReason reason, Color winner) {
        sendToPlayer(recipientId, packetEncoder.gameOverNtf(reason, winner));
    }

    // ---------- 공통 ----------

    private void sendToPlayer(long playerId, byte[] packet) {
        Connection connection = connectionRegistry.findByPlayerId(playerId).orElse(null);
        deliver(connection, packet);
    }

    private void deliver(Connection connection, byte[] packet) {
        if (connection == null || connection.isClosed()) return;

        connection.send(packet);
    }
}
