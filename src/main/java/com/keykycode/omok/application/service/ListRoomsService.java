package com.keykycode.omok.application.service;

import com.keykycode.omok.application.port.in.ListRooms;
import com.keykycode.omok.application.port.out.ClientNotifier;
import com.keykycode.omok.application.port.out.WaitingRoomQuery;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListRoomsService implements ListRooms {

    private final WaitingRoomQuery waitingRoomQuery;
    private final ClientNotifier clientNotifier;


    @Override
    public void listRooms(long playerId, int limit) {
        clientNotifier.roomList(playerId, waitingRoomQuery.list(limit));
    }
}
