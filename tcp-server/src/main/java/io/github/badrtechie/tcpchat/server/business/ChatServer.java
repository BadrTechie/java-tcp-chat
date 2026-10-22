package io.github.badrtechie.tcpchat.server.business;

import io.github.badrtechie.tcpchat.server.business.ServerEventListener.ConnectedUser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChatServer implements AutoCloseable {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ServerEventListener listener;
    private final Clock clock;
    private final Map<UUID, ClientSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> namedUsers = new ConcurrentHashMap<>();
    private final AtomicInteger anonymousSequence = new AtomicInteger();
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ServerSocket serverSocket;
    private volatile ExecutorService tasks;
    private volatile int localPort;

    public ChatServer(ServerEventListener listener) {
        this(listener, Clock.systemDefaultZone());
    }

    public ChatServer(ServerEventListener listener, Clock clock) {
        this.listener = Objects.requireNonNull(listener);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized void start(ServerSettings settings) throws IOException {
        Objects.requireNonNull(settings);
        if (running.get()) {
            throw new IllegalStateException("Server is already running.");
        }
        ServerSocket candidate = new ServerSocket();
        try {
            candidate.bind(new InetSocketAddress(settings.host(), settings.port()));
        } catch (IOException ex) {
            candidate.close();
            throw ex;
        }
        serverSocket = candidate;
        localPort = candidate.getLocalPort();
        tasks = Executors.newVirtualThreadPerTaskExecutor();
        running.set(true);
        String endpoint = settings.host() + ":" + localPort;
        listener.onActivity("Server Started on " + endpoint);
        listener.onActivity("Waiting for Client");
        listener.onRunningChanged(true, endpoint);
        Thread.startVirtualThread(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                tasks.submit(() -> handle(socket));
            } catch (IOException ex) {
                if (running.get()) {
                    listener.onActivity("Accept failed: " + ex.getMessage());
                }
                break;
            }
        }
    }

    private void handle(Socket socket) {
        ClientSession session = null;
        try {
            socket.setSoTimeout(10_000);
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            WireProtocol.Frame hello = WireProtocol.read(input);
            if (hello.type() != WireProtocol.Type.HELLO || hello.fields().size() != 1) {
                WireProtocol.write(output, new WireProtocol.Frame(WireProtocol.Type.ERROR, "Expected HELLO."));
                return;
            }
            String requestedName = Inputs.username(hello.fields().getFirst());
            UUID id = UUID.randomUUID();
            boolean readOnly = requestedName.isBlank();
            String displayName;
            if (readOnly) {
                displayName = "Anonymous-" + anonymousSequence.incrementAndGet() + " (read-only)";
            } else {
                String key = requestedName.toLowerCase(Locale.ROOT);
                if (namedUsers.putIfAbsent(key, id) != null) {
                    WireProtocol.write(output, new WireProtocol.Frame(
                            WireProtocol.Type.ERROR, "Username '" + requestedName + "' is already connected."));
                    return;
                }
                displayName = requestedName;
            }
            session = new ClientSession(id, displayName, readOnly, socket, input, output);
            sessions.put(id, session);
            session.send(new WireProtocol.Frame(
                    WireProtocol.Type.WELCOME, displayName, Boolean.toString(readOnly)));
            socket.setSoTimeout(0);
            listener.onUserConnected(session.user());
            listener.onActivity("Welcome " + displayName);
            readFrames(session);
        } catch (EOFException ignored) {
            // Cleanup below handles orderly and abrupt peer closure.
        } catch (IllegalArgumentException ex) {
            listener.onActivity("Rejected client: " + ex.getMessage());
        } catch (IOException ex) {
            if (running.get()) {
                listener.onActivity("Client connection error: " + ex.getMessage());
            }
        } finally {
            if (session != null) {
                remove(session);
            } else {
                closeQuietly(socket);
            }
        }
    }

    private void readFrames(ClientSession session) throws IOException {
        while (running.get() && !session.closed.get()) {
            WireProtocol.Frame frame = WireProtocol.read(session.input);
            switch (frame.type()) {
                case CHAT -> acceptChat(session, oneField(frame, "CHAT"));
                case USERS_REQUEST -> sendUsers(session);
                case DISCONNECT -> {
                    return;
                }
                default -> session.send(new WireProtocol.Frame(
                        WireProtocol.Type.ERROR, "Unsupported client frame: " + frame.type()));
            }
        }
    }

    private static String oneField(WireProtocol.Frame frame, String label) throws IOException {
        if (frame.fields().size() != 1) {
            throw new IOException(label + " requires exactly one field.");
        }
        return frame.fields().getFirst();
    }

    private void acceptChat(ClientSession sender, String rawMessage) throws IOException {
        if (sender.readOnly) {
            sender.send(new WireProtocol.Frame(
                    WireProtocol.Type.ERROR, "READ-ONLY MODE: anonymous clients cannot send messages."));
            return;
        }
        String message;
        try {
            message = Inputs.message(rawMessage);
        } catch (IllegalArgumentException ex) {
            sender.send(new WireProtocol.Frame(WireProtocol.Type.ERROR, ex.getMessage()));
            return;
        }
        String formatted = "[" + LocalTime.now(clock).format(TIME) + "] "
                + sender.displayName + ": " + message;
        broadcast(new WireProtocol.Frame(WireProtocol.Type.MESSAGE, formatted));
        listener.onActivity("Message from " + sender.displayName);
    }

    private void sendUsers(ClientSession requester) throws IOException {
        List<String> users = sessions.values().stream()
                .map(session -> session.displayName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        requester.send(new WireProtocol.Frame(WireProtocol.Type.USERS, users));
    }

    private void broadcast(WireProtocol.Frame frame) {
        for (ClientSession session : List.copyOf(sessions.values())) {
            try {
                session.send(frame);
            } catch (IOException ex) {
                remove(session);
            }
        }
    }

    private void remove(ClientSession session) {
        if (!session.closed.compareAndSet(false, true)) {
            return;
        }
        sessions.remove(session.id, session);
        if (!session.readOnly) {
            namedUsers.remove(session.displayName.toLowerCase(Locale.ROOT), session.id);
        }
        closeQuietly(session.socket);
        listener.onUserDisconnected(session.user());
        listener.onActivity(session.displayName + " disconnected");
    }

    public boolean isRunning() {
        return running.get();
    }

    public int localPort() {
        return localPort;
    }

    public List<ConnectedUser> activeUsers() {
        return sessions.values().stream()
                .map(ClientSession::user)
                .sorted(Comparator.comparing(ConnectedUser::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        closeQuietly(serverSocket);
        for (ClientSession session : new ArrayList<>(sessions.values())) {
            try {
                session.send(new WireProtocol.Frame(WireProtocol.Type.SHUTDOWN, "Server stopped."));
            } catch (IOException ignored) {
                // Cleanup still closes the socket.
            }
            remove(session);
        }
        ExecutorService currentTasks = tasks;
        if (currentTasks != null) {
            currentTasks.shutdownNow();
        }
        listener.onActivity("Server Stopped");
        listener.onRunningChanged(false, "");
    }

    @Override
    public void close() {
        stop();
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

    private static final class ClientSession {
        private final UUID id;
        private final String displayName;
        private final boolean readOnly;
        private final Socket socket;
        private final DataInputStream input;
        private final DataOutputStream output;
        private final Object writeLock = new Object();
        private final AtomicBoolean closed = new AtomicBoolean();

        private ClientSession(UUID id, String displayName, boolean readOnly, Socket socket,
                              DataInputStream input, DataOutputStream output) {
            this.id = id;
            this.displayName = displayName;
            this.readOnly = readOnly;
            this.socket = socket;
            this.input = input;
            this.output = output;
        }

        private ConnectedUser user() {
            return new ConnectedUser(displayName, readOnly);
        }

        private void send(WireProtocol.Frame frame) throws IOException {
            synchronized (writeLock) {
                if (closed.get()) {
                    throw new IOException("Connection is closed.");
                }
                WireProtocol.write(output, frame);
            }
        }
    }
}
