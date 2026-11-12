package com.keykycode.omok.adapter.tcp.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Worker {

    private final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private final Thread thread;


    public Worker(String name) {
        this.thread = new Thread(this::run, name);
    }

    public void start() {
        thread.start();
    }

    public void post(Runnable task) {
        tasks.add(task);
    }

    private void run() {
        try {
            while (true) {
                Runnable task = tasks.take();
                try {
                    task.run();
                } catch (Throwable e) {
                    // 에러 및 모든 예외를 핸들러에 던지고, 스레드는 계속 일감 실행
                    thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
