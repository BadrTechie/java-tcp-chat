package io.github.badrtechie.tcpchat.client.business;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatClient implements AutoCloseable {
    private final ClientEventListener listener;
    private final Object writeLock = new Object();
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicBoolean disconnectNotified = new AtomicBoolean();
    private volatile Socket socket;
    private volatile DataInputStream input;
    private volatile DataOutputStream output;
    private volatile String displayName = "";
    private volatile boolean readOnly;

    public ChatClient(ClientEventListener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    public synchronized void connect(ClientSettings settings, String requestedUsername) throws IOException {
        Objects.requireNonNull(settings);
        if (connected.get() || socket != null) {
            throw new IllegalStateException("Client is already connected or connecting.");
        }
        String username = Inputs.username(requestedUsername);
        Socket candidate = new Socket();
        try {
            candidate.connect(new InetSocketAddress(settings.host(), settings.port()), 5_000);
            candidate.setTcpNoDelay(true);
            candidate.setSoTimeout(5_000);
            DataInputStream candidateInput = new DataInputStream(
                    new BufferedInputStream(candidate.getInputStream()));
            DataOutputStream candidateOutput = new DataOutputStream(
                    new BufferedOutputStream(candidate.getOutputStream()));
            WireProtocol.write(candidateOutput, new WireProtocol.Frame(WireProtocol.Type.HELLO, username));
            WireProtocol.Frame response = WireProtocol.read(candidateInput);
            if (response.type() == WireProtocol.Type.ERROR && response.fields().size() == 1) {
                throw new IOException(response.fields().getFirst());
            }
            if (response.type() != WireProtocol.Type.WELCOME || response.fields().size() != 2) {
                throw new IOException("Server returned an invalid welcome response.");
            }
            this.socket = candidate;
            this.input = candidateInput;
            this.output = candidateOutput;
            this.displayName = response.fields().get(0);
            this.readOnly = Boolean.parseBoolean(response.fields().get(1));
            candidate.setSoTimeout(0);
            disconnectNotified.set(false);
            connected.set(true);
            listener.onConnected(displayName, readOnly);
            Thread.startVirtualThread(this::readLoop);
        } catch (IOException | RuntimeException ex) {
            closeQuietly(candidate);
            this.socket = null;
            this.input = null;
            this.output = null;
            throw ex;
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public String displayName() {
        return displayName;
    }

    public void sendMessage(String rawMessage) throws IOException {
        ensureConnected();
        String command = rawMessage == null ? "" : rawMessage.trim();
        if ("allUsers".equals(command)) {
            requestActiveUsers();
            return;
        }
        if ("end".equalsIgnoreCase(command) || "bye".equalsIgnoreCase(command)
                || "leave".equalsIgnoreCase(command)) {
            disconnect();
            return;
        }
        if (readOnly) {
            throw new IllegalStateException("READ-ONLY MODE: anonymous clients cannot send messages.");
        }
        send(new WireProtocol.Frame(WireProtocol.Type.CHAT, Inputs.message(rawMessage)));
    }

    public void requestActiveUsers() throws IOException {
        ensureConnected();
        send(new WireProtocol.Frame(WireProtocol.Type.USERS_REQUEST));
    }

    private void ensureConnected() {
        if (!connected.get()) {
            throw new IllegalStateException("Client is offline.");
        }
    }

    private void send(WireProtocol.Frame frame) throws IOException {
        synchronized (writeLock) {
            if (!connected.get() || output == null) {
                throw new IOException("Connection is closed.");
            }
            WireProtocol.write(output, frame);
        }
    }

    private void readLoop() {
        String reason = "Connection closed by server.";
        try {
            while (connected.get()) {
                WireProtocol.Frame frame = WireProtocol.read(input);
                switch (frame.type()) {
                    case MESSAGE -> listener.onMessage(oneField(frame));
                    case USERS -> listener.onActiveUsers(List.copyOf(frame.fields()));
                    case ERROR -> listener.onError(oneField(frame));
                    case SHUTDOWN -> {
                        reason = frame.fields().isEmpty() ? "Server stopped." : frame.fields().getFirst();
                        return;
                    }
                    default -> listener.onError("Unexpected server frame: " + frame.type());
                }
            }
        } catch (EOFException ex) {
            reason = "Connection closed by server.";
        } catch (IOException ex) {
            if (connected.get()) {
                reason = "Connection lost: " + ex.getMessage();
            }
        } finally {
            closeTransport(reason);
        }
    }

    private static String oneField(WireProtocol.Frame frame) throws IOException {
        if (frame.fields().size() != 1) {
            throw new IOException("Invalid " + frame.type() + " frame.");
        }
        return frame.fields().getFirst();
    }

    public void disconnect() {
        if (!connected.compareAndSet(true, false)) {
            closeQuietly(socket);
            socket = null;
            return;
        }
        synchronized (writeLock) {
            if (output != null) {
                try {
                    WireProtocol.write(output, new WireProtocol.Frame(WireProtocol.Type.DISCONNECT));
                } catch (IOException ignored) {
                    // The socket may already have failed.
                }
            }
        }
        closeTransport("Disconnected.");
    }

    private synchronized void closeTransport(String reason) {
        connected.set(false);
        Socket current = socket;
        socket = null;
        input = null;
        output = null;
        closeQuietly(current);
        if (disconnectNotified.compareAndSet(false, true)) {
            listener.onDisconnected(reason);
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Closing is idempotent and best effort.
            }
        }
    }
}
