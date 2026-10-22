# Three-minute demo script

Target duration: exactly 180 seconds. Prepare one PowerShell window, the server, two named clients, one blank-name client, the source files below, and the rendered UML before recording. Keep the windows arranged so actions require minimal dragging.

## 0:00-0:12 — Introduction (12 seconds)

**Show:** Repository root and the two Maven modules.

**Say:** “This is a Java 21 multi-client TCP chat with separate server and client JavaFX applications. It uses TCP sockets, virtual threads, external configuration, and exactly two architectural layers: business and presentation.”

## 0:12-0:35 — Build and structure (23 seconds)

**Show:** `pom.xml`, then `tcp-server/src/main/java/io/github/badrtechie/tcpchat/server` and `tcp-client/src/main/java/io/github/badrtechie/tcpchat/client`.

**Say:** “The Maven reactor contains only `tcp-server` and `tcp-client`. Each application has a `business` package for configuration, validation, framing, sockets, users, commands, and lifecycle, plus a `presentation` package for JavaFX. The default-package `TCPServer` and `TCPClient` classes are only thin launchers.”

## 0:35-1:05 — Source walkthrough (30 seconds)

**Show in order:**

1. `tcp-server/.../business/ChatServer.java` at `start(ServerSettings)` and `acceptLoop()`.
2. `tcp-client/.../business/ChatClient.java` at `connect(...)` and `sendMessage(...)`.
3. `config/server.properties` and `config/client.properties`.
4. `ClientApplication.java` at `runNetwork(...)` and `onMessage(...)`.

**Say:** “`ChatServer.start` creates the socket and `Executors.newVirtualThreadPerTaskExecutor` handles client sessions. `ChatClient.connect` performs the HELLO/WELCOME handshake, while `sendMessage` maps `allUsers` and disconnect aliases before sending frames. Properties supply editable defaults, and two client arguments override host and port. Network work uses virtual threads; presentation callbacks use `Platform.runLater`.”

## 1:05-1:25 — Start the live system (20 seconds)

**Show:** Run the server JAR and three client JAR commands from the README. Click **START** on the server. Connect clients as `Alice`, `Bob`, and blank username.

**Say:** “The server binds to the editable address and port. Alice and Bob connect as named users. Leaving the third username blank produces a unique anonymous identity in READ-ONLY MODE. The server list and activity log come only from live connections.”

## 1:25-2:00 — Messaging (35 seconds)

**Show:** Alice sends with **SEND**. Bob sends with Enter. Point to all three histories and the server log.

**Say:** “Alice sends the first message and Bob sends the second with Enter. The server validates each message, adds the time and display name, then broadcasts once to every active connection, including the sender. Clients do not add an optimistic local copy, so each accepted message appears exactly once.”

## 2:00-2:22 — Anonymous read-only access (22 seconds)

**Show:** Anonymous client receiving both messages; point to disabled composer/SEND and enabled **ACTIVE USERS**.

**Say:** “The anonymous client receives normal broadcasts but cannot send. The official client disables sending, and `ChatServer.acceptChat` independently rejects anonymous CHAT frames, so read-only mode is enforced on both sides.”

## 2:22-2:40 — Private users and disconnect (18 seconds)

**Show:** Click **ACTIVE USERS** on the anonymous client or send exact `allUsers` from Alice. Point out that only the requester receives the list. Enter `bye` from Bob.

**Say:** “The active-user request returns a private USERS frame only to its requester. Bob then enters `bye`, which calls `disconnect`, sends DISCONNECT, closes resources, and removes Bob from the server list.”

## 2:40-3:00 — UML and conclusion (20 seconds)

**Show:** `docs/uml/class-diagram.svg`, then `deployment-diagram.svg` and the two sequence diagrams.

**Say:** “The diagrams document the same two-layer implementation, the server and multiple client JVMs, and the connection, broadcast, active-user, and disconnect flows. The repository also includes bounded loopback tests, executable Windows JARs, the protocol specification, and this technical article draft.”

## Recording checklist

- Rehearse once and stop at 3:00; shorten pauses before removing narration.
- Prebuild with `.\mvnw.cmd clean verify` and prearrange all four windows.
- Use `localhost:3000` for the one-PC demo and avoid exposing unrelated notifications.
- Do not say the technical article is published, the repository was pushed, or the final ZIP exists until those pending steps are completed.
