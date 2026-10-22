package io.github.badrtechie.tcpchat.client.business;

import io.github.badrtechie.tcpchat.server.business.ChatServer;
import io.github.badrtechie.tcpchat.server.business.ServerEventListener;
import io.github.badrtechie.tcpchat.server.business.ServerSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatIntegrationTest {
    private ChatServer server;
    private final List<ChatClient> clients = new ArrayList<>();

    @BeforeEach
    void startServer() throws Exception {
        server = new ChatServer(new ServerEventListener() { });
        server.start(new ServerSettings("127.0.0.1", 0));
    }

    @AfterEach
    void stopEverything() {
        clients.forEach(ChatClient::close);
        server.close();
    }

    @Test
    void broadcastsOnceAndKeepsActiveUsersPrivate() throws Exception {
        RecordingListener aliceEvents = new RecordingListener();
        RecordingListener bobEvents = new RecordingListener();
        RecordingListener anonymousEvents = new RecordingListener();
        ChatClient alice = connect("Alice", aliceEvents);
        ChatClient bob = connect("Bob", bobEvents);
        ChatClient anonymous = connect("", anonymousEvents);

        await(() -> server.activeUsers().size() == 3);
        assertTrue(anonymous.isReadOnly());
        assertThrows(IllegalStateException.class, () -> anonymous.sendMessage("not allowed"));

        alice.sendMessage("hello\nwith safe framing");
        String aliceMessage = aliceEvents.messages.poll(2, TimeUnit.SECONDS);
        String bobMessage = bobEvents.messages.poll(2, TimeUnit.SECONDS);
        String anonymousMessage = anonymousEvents.messages.poll(2, TimeUnit.SECONDS);
        assertNotNull(aliceMessage);
        assertEquals(aliceMessage, bobMessage);
        assertEquals(aliceMessage, anonymousMessage);
        assertTrue(aliceMessage.matches("\\[\\d{2}:\\d{2}:\\d{2}] Alice: hello\\nwith safe framing"));
        assertNull(aliceEvents.messages.poll(200, TimeUnit.MILLISECONDS));
        assertNull(bobEvents.messages.poll(200, TimeUnit.MILLISECONDS));
        assertNull(anonymousEvents.messages.poll(200, TimeUnit.MILLISECONDS));

        anonymous.requestActiveUsers();
        List<String> active = anonymousEvents.userLists.poll(2, TimeUnit.SECONDS);
        assertNotNull(active);
        assertEquals(3, active.size());
        assertTrue(active.contains("Alice"));
        assertTrue(active.contains("Bob"));
        assertTrue(active.stream().anyMatch(name -> name.startsWith("Anonymous-")));
        assertNull(aliceEvents.userLists.poll(200, TimeUnit.MILLISECONDS));
        assertNull(bobEvents.userLists.poll(200, TimeUnit.MILLISECONDS));
    }

    @Test
    void rejectsDuplicateNamesAndCleansNormalAndAbruptDisconnects() throws Exception {
        ChatClient original = connect("SameName", new RecordingListener());
        IOException duplicate = assertThrows(IOException.class,
                () -> new ChatClient(new RecordingListener()).connect(settings(), "samename"));
        assertTrue(duplicate.getMessage().contains("already connected"));
        assertTrue(original.isConnected());
        await(() -> server.activeUsers().size() == 1);

        original.disconnect();
        await(() -> server.activeUsers().isEmpty());

        Socket abrupt = new Socket("127.0.0.1", server.localPort());
        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(abrupt.getOutputStream()));
        DataInputStream input = new DataInputStream(new BufferedInputStream(abrupt.getInputStream()));
        writeFrame(output, "HELLO", "Abrupt");
        assertEquals("WELCOME", readFrame(input).type);
        await(() -> server.activeUsers().size() == 1);
        abrupt.close();
        await(() -> server.activeUsers().isEmpty());
    }

    @Test
    void serverRejectsAnonymousChatEvenIfClientSideGuardIsBypassed() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.localPort())) {
            DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            writeFrame(output, "HELLO", "");
            assertEquals("WELCOME", readFrame(input).type);
            writeFrame(output, "CHAT", "bypassed");
            TestFrame error = readFrame(input);
            assertEquals("ERROR", error.type);
            assertTrue(error.fields.getFirst().contains("READ-ONLY MODE"));
        }
    }

    @Test
    void serverShutdownNotifiesAndDisconnectsClients() throws Exception {
        RecordingListener events = new RecordingListener();
        ChatClient client = connect("ShutdownTest", events);
        server.stop();
        String reason = events.disconnects.poll(2, TimeUnit.SECONDS);
        assertNotNull(reason);
        assertTrue(reason.contains("Server stopped"));
        assertFalse(client.isConnected());
    }

    @Test
    void typedCommandsRequestUsersAndDisconnectUsingBothAliases() throws Exception {
        RecordingListener firstEvents = new RecordingListener();
        ChatClient first = connect("CommandOne", firstEvents);
        first.sendMessage("allUsers");
        assertNotNull(firstEvents.userLists.poll(2, TimeUnit.SECONDS));
        first.sendMessage("bye");
        await(() -> server.activeUsers().isEmpty());

        ChatClient second = connect("CommandTwo", new RecordingListener());
        second.sendMessage("end");
        await(() -> server.activeUsers().isEmpty());
        assertFalse(second.isConnected());
    }

    private ChatClient connect(String username, RecordingListener listener) throws Exception {
        ChatClient client = new ChatClient(listener);
        clients.add(client);
        client.connect(settings(), username);
        return client;
    }

    private ClientSettings settings() {
        return new ClientSettings("127.0.0.1", server.localPort());
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean(), "condition was not met before timeout");
    }

    private static void writeFrame(DataOutputStream output, String type, String... fields) throws IOException {
        StringBuilder text = new StringBuilder(type);
        for (String field : fields) {
            text.append('\t').append(Base64.getEncoder().encodeToString(field.getBytes(StandardCharsets.UTF_8)));
        }
        byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    private static TestFrame readFrame(DataInputStream input) throws IOException {
        int length = input.readInt();
        String[] parts = new String(input.readNBytes(length), StandardCharsets.UTF_8).split("\\t", -1);
        List<String> fields = new ArrayList<>();
        for (int index = 1; index < parts.length; index++) {
            fields.add(new String(Base64.getDecoder().decode(parts[index]), StandardCharsets.UTF_8));
        }
        return new TestFrame(parts[0], fields);
    }

    private record TestFrame(String type, List<String> fields) {
    }

    private static final class RecordingListener implements ClientEventListener {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final BlockingQueue<List<String>> userLists = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> disconnects = new LinkedBlockingQueue<>();

        @Override
        public void onMessage(String formattedMessage) {
            messages.add(formattedMessage);
        }

        @Override
        public void onActiveUsers(List<String> users) {
            userLists.add(users);
        }

        @Override
        public void onDisconnected(String reason) {
            disconnects.add(reason);
        }
    }
}
