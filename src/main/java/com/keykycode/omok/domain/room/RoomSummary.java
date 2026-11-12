package com.keykycode.omok.domain.room;

import com.keykycode.omok.domain.player.Nickname;

public record RoomSummary(long roomId, Nickname hostNickname, int playerCount) {
}
