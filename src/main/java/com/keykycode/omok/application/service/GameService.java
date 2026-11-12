package com.keykycode.omok.application.service;

import com.keykycode.omok.application.dto.Outcome;
import com.keykycode.omok.application.dto.RequestKind;
import com.keykycode.omok.application.port.in.PlaceStone;
import com.keykycode.omok.application.port.in.Resign;
import com.keykycode.omok.application.port.in.StartGame;
import com.keykycode.omok.application.port.out.*;
import com.keykycode.omok.domain.game.Color;
import com.keykycode.omok.domain.game.EndReason;
import com.keykycode.omok.domain.game.Game;
import com.keykycode.omok.domain.game.PlaceResult;
import com.keykycode.omok.domain.game.PlaceResult.Rejection;
import com.keykycode.omok.domain.player.Player;
import com.keykycode.omok.domain.room.Room;
import com.keykycode.omok.domain.room.RoomState;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GameService implements StartGame, PlaceStone, Resign {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
    private final WaitingRooms waitingRooms;
    private final ClientNotifier clientNotifier;
    private final HostColorPicker hostColorPicker;


    @Override
    public void startGame(long playerId) {
        Room room = findJoinedRoom(playerId, RequestKind.START_GAME);
        if (room == null) return;

        if (room.getRoomState() == RoomState.PLAYING) {
            clientNotifier.rejected(playerId, RequestKind.START_GAME, Outcome.ALREADY_PLAYING);
            return;
        }
        if (playerId != room.getHostId()) {
            clientNotifier.rejected(playerId, RequestKind.START_GAME, Outcome.NOT_HOST);
            return;
        }
        if (!room.hasGuest()) {
            clientNotifier.rejected(playerId, RequestKind.START_GAME, Outcome.NO_OPPONENT);
            return;
        }

        long hostId = room.getHostId();
        long guestId = room.getGuestId();
        Color hostColor = hostColorPicker.pickHostColor();
        long blackId = hostColor == Color.BLACK ? hostId : guestId;
        long whiteId = hostColor == Color.BLACK ? guestId : hostId;

        room.startGame(Game.create(blackId, whiteId));
        waitingRooms.remove(room.getRoomId());
        clientNotifier.gameStarted(hostId, blackId, whiteId);
        clientNotifier.gameStarted(guestId, blackId, whiteId);
    }

    @Override
    public void placeStone(long playerId, int x, int y) {
        Room room = findPlayingRoom(playerId, RequestKind.PLACE_STONE);
        if (room == null) return;

        switch (room.getGame().place(playerId, x, y)) {
            case PlaceResult.Rejected rejected ->
                    clientNotifier.rejected(playerId, RequestKind.PLACE_STONE, outcomeOf(rejected.reason()));
            case PlaceResult.Placed placed -> {
                clientNotifier.stonePlaced(room.getHostId(), placed.moveNo(), placed.color(), x, y);
                clientNotifier.stonePlaced(room.getGuestId(), placed.moveNo(), placed.color(), x, y);

                if (placed.ended()) endGame(room, placed.endReason(), placed.winner());
            }
        }
    }

    @Override
    public void resign(long playerId) {
        Room room = findPlayingRoom(playerId, RequestKind.RESIGN);
        if (room == null) return;

        Color winner = room.getGame().colorOf(playerId).opposite();
        endGame(room, EndReason.RESIGN, winner);

    }

    private Room findJoinedRoom(long playerId, RequestKind kind) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null) return null;

        Room room = roomRepository.findById(player.getCurrentRoomId())
                .filter(found -> found.isMember(playerId))
                .orElse(null);
        if (room == null) {
            clientNotifier.rejected(playerId, kind, Outcome.NOT_IN_ROOM);
            return null;
        }
        
        return room;
    }

    /** 대국 중인 방을 찾는다. 방이 없으면 9, 대국 중이 아니면 7로 거절하고 null. */
    private Room findPlayingRoom(long playerId, RequestKind kind) {
        Room room = findJoinedRoom(playerId, kind);
        if (room == null) {
            return null;
        }
        if (room.getRoomState() != RoomState.PLAYING) {
            clientNotifier.rejected(playerId, kind, Outcome.NOT_PLAYING);
            return null;
        }
        return room;
    }

    /** 대국 종료 공통 절차. 양쪽에 gameOver, 방을 대기 상태로 되돌리고 대기방 목록에 다시 올린다. winner가 null이면 무승부. */
    private void endGame(Room room, EndReason reason, Color winner) {
        clientNotifier.gameOver(room.getHostId(), reason, winner);
        clientNotifier.gameOver(room.getGuestId(), reason, winner);
        room.endGame();
        waitingRooms.put(room.toSummary());
    }

    private Outcome outcomeOf(Rejection rejection) {
        return switch (rejection) {
            case NOT_YOUR_TURN -> Outcome.NOT_YOUR_TURN;
            case OUT_OF_RANGE -> Outcome.OUT_OF_RANGE;
            case OCCUPIED -> Outcome.OCCUPIED;
        };
    }
}
