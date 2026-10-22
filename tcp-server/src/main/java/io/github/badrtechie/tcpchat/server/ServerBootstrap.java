package io.github.badrtechie.tcpchat.server;

import io.github.badrtechie.tcpchat.server.presentation.ServerApplication;

public final class ServerBootstrap {
    private ServerBootstrap() {
    }

    public static void start(String[] args) {
        if (args.length != 0) {
            System.err.println("Usage: java -jar TCPServer.jar");
            return;
        }
        ServerApplication.launchApplication();
    }
}
