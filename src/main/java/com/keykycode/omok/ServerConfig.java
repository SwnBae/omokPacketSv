package com.keykycode.omok;

public record ServerConfig(int port, int ioThreads, int roomThreads) {
    private static final int DEFAULT_PORT = 23456;
    private static final int DEFAULT_IO_THREADS = 8;
    private static final int DEFAULT_ROOM_THREADS = 8;

    /** 첫 번째 인자가 있으면 포트로 쓴다. */
    public static ServerConfig fromArgs(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        return new ServerConfig(port, DEFAULT_IO_THREADS, DEFAULT_ROOM_THREADS);
    }
}
