# Project Requirements Checklist

Source: `project_1_requirements_v2.pdf`, "Mini Project 1: Multi-Client Chat Application Using Java Sockets," by El Habib Nfaoui. The source has two sections numbered 3.3; both are retained below by title.

Checkboxes represent implementation or delivery status, not whether a requirement was discovered.

## 1. Goal and architecture

- [ ] Build a group chat application using Java TCP sockets and JavaFX.
- [ ] Allow multiple clients to communicate in real time through one central server.
- [ ] Use a server-client architecture.
- [ ] Make the central server a distributor that receives a message from one client and sends it to connected clients.
- [ ] Let each client connect to the server IP address and port, send messages, and receive messages.
- [ ] Provide a shared chat environment for all connected clients.

## 2.1 Client features

- [ ] Require a username before granting access to the full chat interface.
- [ ] If no username is supplied, connect the user in `READ-ONLY MODE`.
- [ ] Prevent a read-only client from sending chat messages.
- [ ] Provide a text area for composing messages.
- [ ] Send a message through a `SEND` button.
- [ ] Send a message by pressing Enter.
- [ ] Recognize the exact command `allUsers`.
- [ ] Return the list of all currently active clients only to the client that requested `allUsers`.
- [ ] Recognize `end` as a disconnect command.
- [ ] Recognize `bye` as a disconnect command.
- [ ] Provide a `LEAVE` button that disconnects the client.
- [ ] Close the client connection when the user disconnects.
- [ ] Notify the server when the client disconnects normally.
- [ ] Display an `Online` status label in the client UI.
- [ ] Display a visual online indicator, such as a circle, in the client UI.

## 2.2 Server features

- [ ] Accept multiple concurrent client connections.
- [ ] Receive messages from connected clients.
- [ ] Format each chat message with the sender's username.
- [ ] Format each chat message with the time.
- [ ] Distribute each formatted message to active connections according to the resolved broadcast rule.
- [ ] Provide a server JavaFX UI.
- [ ] Maintain a live `ListView` of all connected user display names.
- [ ] Assign different users random background colors in the server client list.
- [ ] Keep each user's assigned color stable while that user remains connected.
- [ ] Ensure generated colors remain readable with the list text.
- [ ] Display a live activity log in the server UI.
- [ ] Include a `Server Started` activity.
- [ ] Include a `Waiting for Client` activity.
- [ ] Include a `Welcome [User]` activity.

## 3. Technical specifications

### 3.1 Technology stack

- [ ] Implement the applications in Java.
- [ ] Use Java sockets over TCP for network communication.
- [ ] Build both graphical interfaces with JavaFX.
- [ ] Use `GridPane` for JavaFX layout where required by the provided design.
- [ ] Use CSS to style the JavaFX interfaces.
- [ ] Keep the project usable from IntelliJ IDEA.

### 3.2 Server-side model

- [ ] Use multithreading to handle simultaneous client connections.
- [ ] Use Java virtual threads for concurrent client sessions.
- [ ] Allow clients to exchange messages in real time.

### 3.3 Network configuration parameters

- [ ] Make the server IP address configurable at runtime.
- [ ] Make the server port configurable at runtime.
- [ ] Load network settings from a standard runtime configuration file, such as `.properties` or `.xml`.
- [ ] Permit environment or deployment changes without recompiling application code.

### 3.3 Model-view decoupling

- [ ] Strictly separate business logic from presentation logic.
- [ ] Keep data processing independent of JavaFX views.
- [ ] Keep socket communication independent of JavaFX views.
- [ ] Ensure the model can function without the JavaFX interface.
- [ ] Ensure presentation changes do not require changes to underlying application logic.

## 4. Operational requirements

- [ ] Provide an entry point named `TCPClient`.
- [ ] Support the documented client launch form `java TCPClient <ServerIPAddress> <PortNumber>` when the compiled classes or executable JAR are on the classpath.
- [ ] Verify the example-equivalent launch using `localhost` and port `3000`.
- [ ] Provide an entry point named `TCPServer`.
- [ ] Support the documented server launch form `java TCPServer` when the compiled classes or executable JAR are on the classpath.
- [ ] Also support executable-JAR launches for both applications.

## 5. Mandatory deliverables

- [ ] Provide a fully functional Maven project for `TCPServer` source code.
- [ ] Provide a fully functional Maven project for `TCPClient` source code.
- [ ] Produce an executable `TCPServer.jar`.
- [ ] Produce an executable `TCPClient.jar`.
- [ ] Create a class diagram showing software structure and relationships between classes.
- [ ] Create a deployment diagram showing server and client nodes and their TCP/IP links.
- [ ] Record a three-minute demo video.
- [ ] Include a walkthrough of the source code in the demo video.
- [ ] Include a live demonstration of application features in the demo video.
- [ ] Share the GitHub repository link: `https://github.com/BadrTechie/java-tcp-chat`.
- [ ] Keep the complete source code in the GitHub repository.
- [ ] Build a meaningful, incremental Git commit history.
- [ ] Replace the minimal README with a descriptive `README.md`.

## 5. Optional deliverables included in project scope

The PDF marks use case/sequence diagrams and the technical post as optional. This project plans to deliver all of them.

- [ ] Create a use case diagram covering named users, anonymous read-only users, and server/operator interactions.
- [ ] Create sequence diagrams that clarify connection, authentication, broadcast, `allUsers`, and disconnect flows.
- [ ] Publish a technical blog post on Medium, Dev.to, or a personal site/forum.
- [ ] Explain the development process in the technical post.
- [ ] Explain important technical challenges and decisions in the technical post.
- [ ] Describe the final implementation in the technical post.
- [ ] Add the published technical post link to the README and submission materials.

## 6. Submission packaging

The following additional submission guidelines are authoritative for final delivery.

- [ ] Name the archive `<StudentNames>_project<Number>.zip` or `<StudentNames>_project<Number>.rar`.
- [ ] For this individual submission, use the exact filename `Badreddine_Lechhab_project1.zip`.
- [ ] Keep the student-name portion configurable so the correct names can be substituted if the submission becomes a group submission.
- [ ] Include or link every required source, JAR, UML export/source, README, demo video, and blog post from the submission package.
- [ ] Build the ZIP from a clean verified revision, not from IDE output or an arbitrary working directory.
- [ ] Verify that the submitted attachment is present, non-empty, readable, and not corrupted.
- [ ] Extract and inspect the final archive before submission.
- [ ] Submit through the required course/LMS channel; do not submit by email because email submissions are discarded.

## 7. Expected learning outcomes

These are stated outcomes rather than additional features, but the implementation and documentation should provide evidence for them.

- [ ] Demonstrate practical TCP communication. UDP is mentioned as a learning outcome but is not required by the implementation specification.
- [ ] Demonstrate Java I/O stream usage.
- [ ] Demonstrate multithreading with virtual threads.
- [ ] Demonstrate server-client architecture and network programming.
- [ ] Demonstrate JavaFX graphical user interface development.

## Accepted requirement interpretations

### Runtime properties versus client arguments

The PDF requires runtime configuration files for server IP/port and separately specifies client host/port arguments.

Accepted decision: ship editable `config/server.properties` and `config/client.properties`. `TCPServer` loads its bind address/port from server properties. `TCPClient` loads client properties as defaults, while two valid command-line arguments override those values. Therefore both `java TCPClient localhost 3000` and a no-argument executable-JAR launch using the runtime properties remain supported. Invalid or partial arguments show usage and do not silently fall back.

### Anonymous clients and active-user lists

The PDF says a blank username creates a read-only client, while the server list is described as a list of connected usernames.

Accepted decision: every anonymous connection receives a server-generated display identity such as `Anonymous-1 (read-only)`. It appears in the server `ListView` and in `allUsers` results because it is an active connection, but it is not treated as an authenticated username. Anonymous clients receive broadcasts and status updates but cannot send chat messages. The `allUsers` text command remains available to named users; anonymous users stay strictly read-only.

### Broadcast recipients

The architecture section says messages go to all other clients, while the server feature says all active connections.

Accepted decision: make the server authoritative and broadcast the formatted message to every active connection, including the sender. The sender does not perform an optimistic local echo, so each message appears exactly once and only after the server accepts it. This favors the more specific server requirement and gives the sender delivery confirmation.

### Named entry points versus executable JARs

The PDF shows `java TCPServer` and `java TCPClient ...`, while also requiring executable JARs.

Accepted decision: provide thin default-package classes named exactly `TCPServer` and `TCPClient`; each delegates immediately to packaged application code. Set those classes as the respective JAR `Main-Class` values during the packaging milestone. Document and test both classpath launches and `java -jar TCPServer.jar` / `java -jar TCPClient.jar ...` launches.

### Submission archive identity

Accepted rule: construct the filename from configurable student names and the project number using `<StudentNames>_project<Number>.zip` or `.rar`. The current individual filename is `Badreddine_Lechhab_project1.zip`. Final verification must reject a missing, empty, unreadable, or corrupted archive, and the archive must be submitted through the required course/LMS channel rather than email.
