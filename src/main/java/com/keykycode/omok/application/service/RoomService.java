package com.keykycode.omok.application.service;

import com.keykycode.omok.application.dto.Outcome;
import com.keykycode.omok.application.dto.RequestKind;
import com.keykycode.omok.application.port.in.CreateRoom;
import com.keykycode.omok.application.port.in.DisconnectInRoom;
import com.keykycode.omok.application.port.in.JoinRoom;
import com.keykycode.omok.application.port.in.LeaveRoom;
import com.keykycode.omok.application.port.out.*;
import com.keykycode.omok.domain.game.EndReason;
import com.keykycode.omok.domain.game.Game;
import com.keykycode.omok.domain.player.Player;
import com.keykycode.omok.domain.room.Room;
import com.keykycode.omok.domain.room.RoomState;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class RoomService implements CreateRoom, JoinRoom, LeaveRoom, DisconnectInRoom {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
    private final WaitingRooms waitingRooms;
    private final ConnectionStatus connectionStatus;
    private final ClientNotifier clientNotifier;

    @Override
    public void createRoom(long playerId, long roomId) {
        Player player = findEnterablePlayer(playerId, RequestKind.CREATE_ROOM);
        if (player == null) return;

        if(player.getCurrentRoomId() != Player.NO_ROOM) {
            clientNotifier.rejected(playerId, RequestKind.CREATE_ROOM, Outcome.ALREADY_IN_ROOM);
            return;
        }

        if (!tryEnter(player, roomId, RequestKind.CREATE_ROOM)) return;

        Room room = Room.create(roomId, playerId, player.getNickname());
        roomRepository.add(room);
        waitingRooms.put(room.toSummary());
        clientNotifier.roomCreated(playerId, roomId);
    }

    @Override
    public void joinRoom(long playerId, long roomId) {
        Player player = findEnterablePlayer(playerId, RequestKind.JOIN_ROOM);
        if (player == null) return;

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            clientNotifier.rejected(playerId, RequestKind.JOIN_ROOM, Outcome.ROOM_NOT_FOUND);
            return;
        }
        if(room.getRoomState() == RoomState.PLAYING || room.hasGuest()) {
            clientNotifier.rejected(playerId, RequestKind.JOIN_ROOM, Outcome.ROOM_NOT_JOINABLE);
            return;
        }
        if(!tryEnter(player, roomId, RequestKind.JOIN_ROOM)) return;

        room.addGuest(playerId);
        waitingRooms.put(room.toSummary());
        clientNotifier.roomJoined(playerId, roomId, room.getHostId(), room.getHostNickname());
        clientNotifier.opponentJoined(room.getHostId(), playerId, player.getNickname());
    }

    @Override
    public void leaveRoom(long playerId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if(player == null) return;

        Room room = findJoinedRoom(player).orElse(null);
        if(room == null) {
            clientNotifier.rejected(playerId, RequestKind.LEAVE_ROOM, Outcome.NOT_IN_ROOM);
            return;
        }

        leave(player, room, EndReason.RESIGN);
        clientNotifier.roomLeft(playerId);
    }

    @Override
    public void disconnectInRoom(long playerId) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null) return;

        findJoinedRoom(player).ifPresent(room -> leave(player, room, EndReason.DISCONNECT));
        playerRepository.remove(playerId);
    }


    private boolean tryEnter(Player player, long roomId, RequestKind kind) {
        if (!player.enterRoom(roomId)) {
            clientNotifier.rejected(player.getPlayerId(), kind, Outcome.ALREADY_IN_ROOM);
            return false;
        }

        if (!connectionStatus.isConnected(player.getPlayerId())) {
            player.leaveRoom(roomId);
            return false;
        }

        return true;
    }

    private Player findEnterablePlayer(long playerId, RequestKind kind) {
        Player player = playerRepository.findById(playerId).orElse(null);
        if (player == null) return null;

        if (player.getCurrentRoomId() != Player.NO_ROOM) {
            clientNotifier.rejected(playerId, kind, Outcome.ALREADY_IN_ROOM);
            return null;
        }

        return player;
    }

    private Optional<Room> findJoinedRoom(Player player) {
        return roomRepository.findById(player.getCurrentRoomId())
                .filter(room -> room.isMember(player.getPlayerId()));
    }

    private void leave(Player player, Room room, EndReason reason) {
        if (player.getPlayerId() == room.getHostId()) {
            leaveAsHost(player, room, reason);
            return;
        }

        leaveAsGuest(player, room, reason);
    }

    private void leaveAsHost(Player host, Room room, EndReason reason) {
        long roomId = room.getRoomId();
        long hostId = host.getPlayerId();
        long guestId = room.getGuestId();
        boolean hasGuest = room.hasGuest();
        Game game = room.getGame();

        if (hasGuest) playerRepository.findById(guestId).ifPresent(guest -> guest.leaveRoom(roomId));

        host.leaveRoom(roomId);
        waitingRooms.remove(roomId);
        roomRepository.remove(roomId);

        if (game != null) clientNotifier.gameOver(guestId, reason, game.colorOf(guestId));
        if (hasGuest) clientNotifier.opponentLeft(guestId, hostId);
    }

    private void leaveAsGuest(Player guest, Room room, EndReason reason) {
        long hostId = room.getHostId();
        Game game = room.getGame();

        if (game != null) {
            clientNotifier.gameOver(hostId, reason, game.colorOf(hostId));
            room.endGame();
        }

        room.removeGuest();
        guest.leaveRoom(room.getRoomId());
        waitingRooms.put(room.toSummary());
        clientNotifier.opponentLeft(hostId, guest.getPlayerId());
    }
}
