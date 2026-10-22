package io.github.badrtechie.tcpchat.client;

import io.github.badrtechie.tcpchat.client.presentation.ClientApplication;

public final class ClientBootstrap {
    private ClientBootstrap() {
    }

    public static void start(String[] args) {
        ClientApplication.setArguments(args);
        ClientApplication.launchApplication();
    }
}
