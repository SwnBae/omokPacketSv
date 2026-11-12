package com.keykycode.omok.application.port.out;

import com.keykycode.omok.application.dto.Outcome;
import com.keykycode.omok.application.dto.RequestKind;
import com.keykycode.omok.domain.game.Color;
import com.keykycode.omok.domain.game.EndReason;
import com.keykycode.omok.domain.player.Nickname;
import com.keykycode.omok.domain.room.RoomSummary;

import java.util.List;

public interface ClientNotifier {

    // RES
    void loginSucceeded(long playerId);

    /** 아직 playerId가 없어서 수신자를 연결 번호로 지정한다. */
    void loginRejected(long connectionId, Outcome outcome);

    void roomCreated(long recipientId, long roomId);

    void roomJoined(long recipientId, long roomId, long hostId, Nickname hostNickname);

    void roomLeft(long recipientId);

    void roomList(long recipientId, List<RoomSummary> summaries);

    void rejected(long recipientId, RequestKind kind, Outcome outcome);


    // NTF
    void opponentJoined(long recipientId, long newcomerId, Nickname nickname);

    void opponentLeft(long recipientId, long leaverId);

    void gameStarted(long recipientId, long blackId, long whiteId);

    void stonePlaced(long recipientId, int moveNo, Color color, int x, int y);

    /** 무승부면 winner는 null. */
    void gameOver(long recipientId, EndReason reason, Color winner);
}
