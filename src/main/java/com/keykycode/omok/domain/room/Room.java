package com.keykycode.omok.domain.room;

import com.keykycode.omok.domain.game.Game;
import com.keykycode.omok.domain.player.Nickname;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Room {

    public static final long NO_GUEST = 0L;

    private final long roomId;
    private final long hostId;
    private final Nickname hostNickname;
    private long guestId = NO_GUEST;
    private RoomState roomState = RoomState.WAITING;

    /** PLAYING 중에만 존재, 아니면 null*/
    private Game game;

    public static Room create(long roomId, long hostId, Nickname hostNickname) {
        return new Room(roomId, hostId, hostNickname);
    }

    public boolean hasGuest() {
        return guestId != NO_GUEST;
    }

    public int playerCount() {
        return hasGuest() ? 2 : 1;
    }

    public boolean isMember(long playerId) {
        return playerId == hostId || (hasGuest() && playerId == guestId);
    }

    public void addGuest(long newGuestId) {
        if (newGuestId == NO_GUEST) throw new IllegalArgumentException("참가자 ID는 " + NO_GUEST + "일 수 없습니다.");
        if (hasGuest()) throw new IllegalStateException("이미 꽉찬 방입니다.");

        this.guestId = newGuestId;
    }

    /** PLAYING 이라면 endGame()으로 먼저 게임을 끝내야함 */
    public void removeGuest() {
        if (!hasGuest()) throw new IllegalStateException("참가자가 없습니다.");
        if (roomState == RoomState.PLAYING) throw new IllegalStateException("대국 중 참가자를 제거할 수 없습니다.");

        this.guestId = NO_GUEST;
    }

    public void startGame(Game game) {
        if (roomState != RoomState.WAITING) throw new IllegalStateException("대국 중엔 게임을 시작할 수 없습니다.");
        if (!hasGuest()) throw new IllegalStateException("참가자가 존재해야 대국을 시작할 수 있습니다.");

        this.game = game;
        this.roomState = RoomState.PLAYING;
    }

    public void endGame() {
        if (roomState != RoomState.PLAYING) throw new IllegalStateException("대국 중이 아닙니다.");

        this.game = null;
        roomState = RoomState.WAITING;
    }

    public RoomSummary toSummary() {
        return new RoomSummary(roomId, hostNickname, playerCount());
    }
}
