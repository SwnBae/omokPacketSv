package com.keykycode.omok;

import com.keykycode.omok.adapter.memory.InMemoryPlayerRepository;
import com.keykycode.omok.adapter.memory.InMemoryRoomRepository;
import com.keykycode.omok.adapter.memory.InMemoryWaitingRooms;
import com.keykycode.omok.adapter.memory.RandomHostColorPicker;
import com.keykycode.omok.adapter.memory.generator.SequencePlayerIdGenerator;
import com.keykycode.omok.adapter.tcp.codec.PacketDecoder;
import com.keykycode.omok.adapter.tcp.codec.PacketEncoder;
import com.keykycode.omok.adapter.tcp.concurrent.Worker;
import com.keykycode.omok.adapter.tcp.in.ManagementRequestHandler;
import com.keykycode.omok.adapter.tcp.in.RoomRequestHandler;
import com.keykycode.omok.adapter.tcp.in.Router;
import com.keykycode.omok.adapter.tcp.net.ConnectionRegistry;
import com.keykycode.omok.adapter.tcp.net.TcpServer;
import com.keykycode.omok.adapter.tcp.out.TcpClientNotifier;
import com.keykycode.omok.adapter.tcp.out.TcpConnectionBinder;
import com.keykycode.omok.application.port.out.PlayerRepository;
import com.keykycode.omok.application.port.out.RoomRepository;
import com.keykycode.omok.application.service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OmokServerApplication {

    public static void main(String[] args) throws IOException{
        start(ServerConfig.fromArgs(args));
    }

    public static TcpServer start(ServerConfig config) throws IOException {

        ConnectionRegistry registry = new ConnectionRegistry();
        PacketEncoder encoder = new PacketEncoder();
        TcpClientNotifier notifier = new TcpClientNotifier(registry, encoder);
        TcpConnectionBinder binder = new TcpConnectionBinder(registry);

        PlayerRepository playerRepository = new InMemoryPlayerRepository();
        InMemoryWaitingRooms waitingRooms = new InMemoryWaitingRooms();
        RoomRepository roomRepository = new InMemoryRoomRepository();

        RoomService roomService = new RoomService(playerRepository, roomRepository, waitingRooms, notifier, notifier);
        GameService gameService = new GameService(playerRepository, roomRepository, waitingRooms, notifier, new RandomHostColorPicker());
        LoginService loginService = new LoginService(playerRepository, new SequencePlayerIdGenerator(), binder, notifier);
        PlayerService playerService = new PlayerService(playerRepository);
        ListRoomsService listRoomsService = new ListRoomsService(waitingRooms, notifier);

        // 관리, 방 스레드 및 핸들러
        Worker managementWorker = new Worker("management");
        ManagementRequestHandler managementHandler = new ManagementRequestHandler(loginService, listRoomsService);
        RoomRequestHandler roomHandler = new RoomRequestHandler(roomService, roomService, roomService, gameService, gameService, gameService, roomService);

        List<Worker> roomWorkers = new ArrayList<>();
        for (int i = 0; i < config.roomThreads(); i++) {
            roomWorkers.add(new Worker("room-" + i));
        }

        Router router = new Router(registry, managementWorker, managementHandler, roomWorkers, roomHandler, playerService, playerService, encoder);
        TcpServer server = new TcpServer(config.port(), config.ioThreads(), registry, router, new PacketDecoder(), encoder);

        // 일을 받는 쪽(관리, 방 스레드)을 먼저, 연결을 받는 쪽(I/O 루프, accept)을 나중에 띄운다.
        managementWorker.start();
        roomWorkers.forEach(Worker::start);
        server.start();
        return server;
    }
}