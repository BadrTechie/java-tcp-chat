package io.github.badrtechie.tcpchat.client.business;

import java.util.List;

public interface ClientEventListener {
    default void onConnected(String displayName, boolean readOnly) {
    }

    default void onMessage(String formattedMessage) {
    }

    default void onActiveUsers(List<String> users) {
    }

    default void onError(String message) {
    }

    default void onDisconnected(String reason) {
    }
}
