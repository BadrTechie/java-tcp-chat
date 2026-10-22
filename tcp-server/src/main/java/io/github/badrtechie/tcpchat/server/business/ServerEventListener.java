package io.github.badrtechie.tcpchat.server.business;

public interface ServerEventListener {
    default void onActivity(String text) {
    }

    default void onUserConnected(ConnectedUser user) {
    }

    default void onUserDisconnected(ConnectedUser user) {
    }

    default void onRunningChanged(boolean running, String endpoint) {
    }

    record ConnectedUser(String displayName, boolean readOnly) {
    }
}
