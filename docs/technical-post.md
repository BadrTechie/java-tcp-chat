# Building a Multi-Client JavaFX TCP Chat with Java 21 Virtual Threads

*A source-accurate walkthrough of a two-layer TCP chat application with explicit framing, anonymous read-only access, JavaFX thread isolation, and executable Windows JARs.*

Repository: https://github.com/BadrTechie/java-tcp-chat

## What the application does

This project contains two Maven application modules:

- `tcp-server` runs a JavaFX server console and a multi-client TCP server.
- `tcp-client` runs a JavaFX chat client.

The server accepts multiple simultaneous clients. Named clients can publish messages, while clients that leave the username blank receive a generated label such as `Anonymous-1 (read-only)` and can only read broadcasts or request the active-user list. The server adds the timestamp and sender name, then sends each accepted message once to every active connection, including the sender.

The application also handles duplicate named identities, private `allUsers` responses, clean command/button/window disconnection, abrupt socket loss, and server shutdown.

## One constraint shapes the structure: exactly two layers

Each module uses only two architectural layers.

The **business layer** owns configuration, validation, protocol framing, sockets, connected-user state, commands, message processing, concurrency, and lifecycle. Its classes live under:

- `io.github.badrtechie.tcpchat.server.business`
- `io.github.badrtechie.tcpchat.client.business`

The **presentation layer** owns JavaFX controls, `GridPane` layout, CSS, UI event handling, status display, and the handoff to the JavaFX Application Thread:

- `io.github.badrtechie.tcpchat.server.presentation`
- `io.github.badrtechie.tcpchat.client.presentation`

There is no repository, persistence, service, controller, model, or network layer. Focused classes still have separate responsibilities, but all non-UI responsibilities remain inside `business`.

The default-package `TCPServer` and `TCPClient` classes are thin assignment-compatible entry points. `ServerBootstrap.start` launches `ServerApplication`, while `ClientBootstrap.start` passes startup arguments to `ClientApplication` before launching it. These adapters do not contain application behavior and are not a third layer.

The dependency direction stays simple:

```text
JavaFX presentation
        |
        | plain method calls and callback interfaces
        v
business logic and networking
```

`ServerApplication` implements `ServerEventListener`, and `ClientApplication` implements `ClientEventListener`. Those interfaces contain ordinary Java callbacks and no JavaFX types. As a result, `ChatServer` and `ChatClient` run in headless integration tests.

## External configuration and live user input

Deployment-specific values are not compiled into the applications.

`config/server.properties` contains:

```properties
server.host=0.0.0.0
server.port=3000
```

`config/client.properties` contains:

```properties
server.host=localhost
server.port=3000
```

`ServerSettings.load` reads the server defaults. `ClientSettings.resolve` reads the client defaults when no command-line arguments are present. Exactly two client arguments override both properties:

```powershell
java -jar .\tcp-client\target\TCPClient.jar localhost 3000
```

The selected host and port prefill editable connection controls. The username always comes from the client UI and remains optional. Production code does not seed users, conversations, or connection state.

The `Inputs` utility in each business package validates hosts, ports, usernames, and messages at their boundaries. Usernames reject control characters and are limited to 40 characters; messages must be nonblank and no longer than 4,000 characters.

## A framed UTF-8 protocol

TCP is a byte stream, not a message queue. One socket read is not guaranteed to correspond to one socket write, so the application defines explicit frame boundaries.

`WireProtocol.write` serializes every frame as:

1. A four-byte, big-endian payload length written by `DataOutputStream.writeInt`.
2. Exactly that many UTF-8 bytes.

The payload starts with a frame type followed by tab-separated fields. Each individual field is Base64-encoded UTF-8. The frame types are:

- `HELLO` and `WELCOME` for identity negotiation.
- `CHAT` and `MESSAGE` for client input and server-formatted broadcasts.
- `USERS_REQUEST` and `USERS` for the private active-user query.
- `ERROR` for validation or protocol errors.
- `DISCONNECT` and `SHUTDOWN` for lifecycle signaling.

Base64 is used only to keep arbitrary field content—such as tabs, newlines, and Unicode—from colliding with the field separator. **It is not encryption and provides no confidentiality or authentication.** The TCP connection is unencrypted.

`WireProtocol.read` rejects zero, negative, incomplete, malformed, or oversized frames. The maximum encoded frame size is 1,048,576 bytes. This bounded framing also prevents a peer from requesting an unbounded allocation through the length prefix.

The client recognizes complete commands before producing a `CHAT` frame. Exact `allUsers` becomes `USERS_REQUEST`, while case-insensitive `end`, `bye`, or `leave` calls `disconnect`. The server never searches message substrings for commands, so ordinary message content stays intact.

## Virtual threads and protected shared state

`ChatServer.start` creates and binds a `ServerSocket`, creates an `Executors.newVirtualThreadPerTaskExecutor()`, and starts the accept loop on a virtual thread. Every accepted socket is submitted as a separate virtual-thread task that runs `handle(Socket)`.

This maps the blocking socket code directly to a client session without putting network work on the JavaFX thread. The same approach is used by the client presentation: `ClientApplication` starts virtual threads around `ChatClient.connect`, `sendMessage`, `requestActiveUsers`, and `disconnect`.

Virtual threads do not remove the need for synchronization. The server stores active sessions and case-normalized named identities in `ConcurrentHashMap` instances. `putIfAbsent` performs the duplicate-name claim atomically. An `AtomicInteger` supplies unique anonymous labels, and atomic booleans protect server/session lifecycle transitions.

Multiple virtual threads may broadcast to the same client concurrently. Each `ClientSession` therefore has a `writeLock`, and its `send` method serializes the complete frame write and flush. `ChatClient` uses the same pattern for its outgoing frames. Without per-connection serialization, bytes from two valid frames could interleave on one TCP stream.

Lifecycle operations are idempotent. `ChatServer.remove` uses `compareAndSet` before removing maps, closing the socket, and emitting callbacks. `ChatClient.closeTransport` clears transport references, closes the socket, and emits `onDisconnected` once. Both client and server bound the initial handshake with socket timeouts.

## Identity and read-only enforcement

The first client frame must be `HELLO(username)`. `ChatServer.handle` validates the field and chooses one of two paths:

- A nonblank name is claimed case-insensitively in `namedUsers`. An existing claim produces a clear `ERROR` frame and closes the rejected connection.
- A blank name receives `Anonymous-N (read-only)` and a `WELCOME` frame whose second field is `true`.

Read-only behavior is intentionally enforced twice. `ClientApplication.onConnected` disables the composer and SEND button, and `ChatClient.sendMessage` rejects attempts from a read-only client. The server remains authoritative: `ChatServer.acceptChat` checks `sender.readOnly` and returns an `ERROR` even if another client implementation bypasses the official UI and sends `CHAT` directly.

Anonymous clients can still call `requestActiveUsers` through the Active Users button because that operation does not publish a chat message.

## Authoritative broadcasts and private replies

When a named client sends text, `ChatClient.sendMessage` validates it and writes `CHAT(text)`. `ChatServer.acceptChat` creates:

```text
[HH:mm:ss] DisplayName: original message
```

`broadcast` sends one `MESSAGE` frame to a snapshot of all active sessions, including the sender. The client does not display a local optimistic echo. The sender therefore sees the same single, server-authoritative copy as every other client.

An active-user request follows a different path. `ChatServer.sendUsers` sorts the current display names and calls `requester.send(...)` only. It does not call `broadcast`, so no other connection receives the `USERS` frame.

Clean disconnection can begin with `end`, `bye`, `leave`, the LEAVE button, or window close. The client writes `DISCONNECT` where possible and closes its transport. EOF and abrupt socket failures reach the same server cleanup path. When the operator stops the server, `ChatServer.stop` sends `SHUTDOWN("Server stopped.")` before closing sessions; connected clients update to Offline through `onDisconnected`.

## Keeping JavaFX on its own thread

JavaFX controls must be accessed on the JavaFX Application Thread. The business layer cannot import JavaFX, so it reports events through `ClientEventListener` and `ServerEventListener`.

Network callbacks occur on virtual threads. `ClientApplication` and `ServerApplication` wrap control changes in `Platform.runLater`. Text histories, status labels, indicators, buttons, the server activity log, and the connected-user `ListView` are therefore updated at the presentation boundary.

Both interfaces build their layouts with `GridPane` and load external CSS resources (`client.css` and `server.css`). The server presentation assigns a color when a connected user is added and keeps that mapping until the user disconnects.

## Testing the business layer over real sockets

The Maven test suite uses JUnit 5. `ServerSettingsTest` and `ClientSettingsTest` cover properties, CLI precedence, missing values, and invalid hosts/ports.

`ChatIntegrationTest` starts `ChatServer` on loopback with port `0`, allowing the operating system to select an ephemeral port. Tests use bounded polling rather than unbounded sleeps. The suite covers:

- multiple simultaneous clients, including an anonymous client;
- sender-inclusive delivery exactly once;
- multiline message content through the framing protocol;
- private active-user responses;
- client-side and server-side anonymous send rejection;
- case-insensitive duplicate identities;
- `allUsers`, `end`, and `bye`;
- normal and abrupt disconnect cleanup; and
- server shutdown notification.

The verified release command is:

```powershell
.\mvnw.cmd clean verify
```

It passed 10 tests on Temurin JDK 22.0.2 while compiling with `--release 21`.

## Packaging and platform limits

Each module uses Maven Shade with a thin default-package launcher as `Main-Class`. The build creates:

- `tcp-server/target/TCPServer.jar`
- `tcp-client/target/TCPClient.jar`

The JARs include JavaFX 21.0.2 dependencies and Windows x64 native libraries. They run with `java -jar` without a separate JavaFX SDK on Windows. This convenience also creates two explicit limitations:

1. The packaged artifacts are Windows-specific; another operating system needs a rebuild with the matching OpenJFX native dependencies.
2. Shaded JavaFX classes run from the unnamed module and may log a non-fatal “unsupported JavaFX configuration” warning.

The protocol uses plain TCP, so it does not provide TLS encryption, server authentication, user passwords, private chats, or durable history. Those are possible future features, not claims about this implementation.

## Factual implementation sequence

The repository history records the work in bounded milestones: requirements and ambiguity decisions, the Maven foundation, the complete two-layer application with tests and packaging, then protocol/UML/demo/article documentation. This sequence kept the headless business behavior testable before relying on manual GUI checks.

## Run it

From the repository root:

```powershell
.\mvnw.cmd clean verify
java -jar .\tcp-server\target\TCPServer.jar
```

Click START in the server window, then launch clients:

```powershell
java -jar .\tcp-client\target\TCPClient.jar localhost 3000
```

For a one-PC demonstration, run three client processes: two with different names and one with a blank username. The deployment diagram explains how the same application runs across separate machines when clients use the server's reachable address instead of `localhost`.

## Closing

The central design is deliberately direct: a framed protocol makes the TCP byte stream reliable, virtual threads keep blocking socket code readable, concurrent collections and per-socket write locks protect shared state, and plain callbacks keep JavaFX out of the business layer. The result is a functional multi-client chat whose automated tests and diagrams describe the same implementation that ships in the executable JARs.
