# Implementation and Delivery Plan

## Current status

Milestone 0 (requirements and planning) and Milestone 1 (Maven foundation) are complete. The repository now has a Java 21-compatible two-module Maven reactor, the official Maven Wrapper, pinned JavaFX/JUnit dependencies, external properties templates, thin launcher skeletons, and launcher smoke tests. No sockets, chat behavior, JavaFX screens, final executable JARs, commit, push, or submission archive have been created.

## Planned repository structure

```text
java-tcp-chat/
|-- pom.xml                         # Parent and two-module aggregator
|-- mvnw
|-- mvnw.cmd
|-- .mvn/wrapper/
|-- config/
|   |-- server.properties
|   `-- client.properties
|-- tcp-server/                     # Maven JavaFX application module
|   |-- pom.xml
|   `-- src/{main,test}/...
|-- tcp-client/                     # Maven JavaFX application module
|   |-- pom.xml
|   `-- src/{main,test}/...
|-- docs/
|   |-- requirements.md
|   |-- plan.md
|   |-- uml/                        # Editable sources and exported diagrams
|   |-- demo/                       # Three-minute plan and final script
|   `-- blog/                       # Technical post draft
|-- dist/                           # Final staged JARs/config only when requested
|-- AGENTS.md
`-- README.md
```

The root project uses Maven `pom` packaging. `tcp-server` and `tcp-client` are the only application modules. Each owns its model, networking, services, and JavaFX UI so neither application depends on JavaFX for headless behavior. The text protocol is documented and implemented compatibly on both sides without introducing a third application module.

## Architecture plan

### Server application

- `TCPServer`: exact, thin default-package launcher and executable-JAR main class.
- Packaged JavaFX launcher: creates the server UI and composition root.
- Configuration loader: reads external server properties and validates bind address/port.
- Chat server service: owns lifecycle, listening socket, user registry, message routing, and server events without JavaFX imports.
- Client session handler: one virtual thread per accepted connection, UTF-8 protocol parsing, clean shutdown, and disconnect notification.
- User registry: thread-safe identities, anonymous labels, stable list colors, and active-user snapshots.
- JavaFX controller/adapters: subscribe to service events and marshal UI changes onto the JavaFX application thread.

### Client application

- `TCPClient`: exact, thin default-package launcher and executable-JAR main class.
- Packaged JavaFX launcher: parses startup inputs, creates the client UI, and composes dependencies.
- Configuration loader: reads external client properties; valid CLI host/port values take precedence.
- Chat client service: connect, receive, send allowed commands/messages, expose state/events, and disconnect without JavaFX imports.
- Client state model: named versus anonymous/read-only identity, online state, and connection lifecycle.
- JavaFX controller/adapters: enable/disable send controls, display messages/status, and marshal service events to the JavaFX thread.

### Protocol plan

- Use UTF-8, line-delimited frames with explicit message types rather than interpreting arbitrary display text.
- Define connect/identity, accepted/rejected, chat, active-users request/response, leave, presence, status, and error messages.
- Escape or encode payloads so usernames and message text cannot break frame boundaries.
- Format authoritative chat timestamps on the server.
- Treat `allUsers`, `end`, and `bye` as exact user commands before creating a chat frame.
- Ensure malformed frames fail safely without terminating the server accept loop.

## Resolved design decisions

1. **Configuration and CLI precedence:** external `.properties` files are runtime defaults; two client CLI arguments override client host/port. Partial or invalid arguments fail with usage information. The server has no required CLI arguments and reads server properties.
2. **Anonymous clients:** blank usernames become unique server display identities such as `Anonymous-1 (read-only)`. They count as active connections and receive broadcasts, but cannot send messages. The typed `allUsers` command is reserved for named clients.
3. **Broadcast semantics:** the server broadcasts an accepted, formatted chat message to every active connection, including the sender. Clients do not locally echo outgoing messages.
4. **Launch compatibility:** thin `TCPServer` and `TCPClient` wrappers preserve the exact documented names. JAR manifests use those wrappers so classpath and `java -jar` launches are both supported.
5. **Submission archive:** construct the archive name from configurable student names and the project number using `<StudentNames>_project<Number>.zip` or `.rar`. The current individual filename is exactly `Badreddine_Lechhab_project1.zip`. Validate that the attachment is present, non-empty, readable, and uncorrupted, and submit through the required course/LMS channel rather than email.

## Maven and build plan

- Configure Java compiler release 21 and UTF-8 centrally in the parent POM. JDK 22.0.2 may run Maven, but generated bytecode must target Java 21 without preview features.
- Pin Maven plugin, JUnit 5, and JavaFX dependency versions in dependency/plugin management.
- Keep the generated official Maven Wrapper files in version control when commits are requested so Windows users run `mvnw.cmd` without a global Maven installation.
- Configure each module as a JavaFX application with a normal main launcher that does not extend `Application` directly.
- Use a packaging plugin such as Maven Shade to produce self-contained, Windows-compatible executable JARs with service resources merged and the correct manifest main class.
- Rename/copy release artifacts to `dist/TCPServer.jar` and `dist/TCPClient.jar` only during the release milestone.
- Keep external `config/server.properties` and `config/client.properties` beside the distribution and document lookup behavior.

Planned build and verification commands:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -pl tcp-server package
.\mvnw.cmd -pl tcp-client package
java -jar dist\TCPServer.jar
java -jar dist\TCPClient.jar localhost 3000
java -cp dist\TCPServer.jar TCPServer
java -cp dist\TCPClient.jar TCPClient localhost 3000
```

## Test strategy

### Unit tests

- Configuration file loading, missing keys, invalid ports, and CLI-over-properties precedence.
- Username validation and anonymous identity assignment.
- Protocol serialization, parsing, escaping, and malformed input.
- Exact handling of `allUsers`, `end`, and `bye`.
- Thread-safe user registry snapshots, joins, removals, and stable color assignment.
- Server-side timestamp and display formatting.
- Client lifecycle state and idempotent disconnect behavior.

### Integration tests

- Start the headless server on a loopback address and ephemeral port.
- Connect multiple headless clients concurrently and verify virtual-thread sessions.
- Verify sender-inclusive broadcast exactly once per client.
- Verify `allUsers` returns only to the requester and includes anonymous display identities.
- Verify anonymous clients receive messages but cannot publish chat messages.
- Verify `end`, `bye`, `LEAVE`, orderly EOF, and abrupt socket loss remove the user and release resources.
- Verify one malformed or failed client does not stop other sessions or the accept loop.

### UI and release checks

- Keep controller behavior testable through service fakes where practical.
- Manually verify JavaFX `GridPane` layout, CSS, send-by-button, send-by-Enter, status label/circle, server logs, live `ListView`, and readable random user colors.
- Run `mvnw.cmd clean verify` from a clean checkout-equivalent state.
- Smoke-test all four launch forms listed above on the installed JDK 22.0.2 for Windows while enforcing compiler release 21.
- Run at least three clients, including one anonymous client, during final acceptance testing.

## Milestone sequence

### Milestone 1 - Maven foundation

Status: completed on 2026-10-22.

- Added the root aggregator, two application modules, official Maven Wrapper, Java release 21/JavaFX/JUnit configuration, `.gitignore`, and external property templates.
- Added exact launcher skeletons and package boundaries without implementing chat behavior or JavaFX `Application` classes.
- Verified `.\mvnw.cmd -version`, `.\mvnw.cmd clean verify`, Java 21 class-file output, both launcher tests, and both placeholder launch commands on Windows.
- Kept the generated module JARs as non-executable skeleton artifacts; final executable-JAR delivery remains open.

### Milestone 2 - Protocol and configuration

- Implement and test configuration loading, CLI precedence, validation, protocol frames, command parsing, identity rules, and lifecycle models.
- Document the wire protocol and runtime configuration.

### Milestone 3 - Headless server core

- Implement listening lifecycle, virtual-thread session handling, user registry, anonymous identities, authoritative timestamps, broadcasts, `allUsers`, disconnect handling, and server events.
- Complete loopback integration tests without JavaFX.

### Milestone 4 - Headless client core

- Implement connect/send/receive/disconnect behavior, read-only enforcement, state events, and clean resource shutdown.
- Complete multi-client integration tests against the headless server.

### Milestone 5 - Server JavaFX UI

- Build the `GridPane` server view, live activity log, active-user `ListView`, stable readable random colors, and start/stop/status controls as appropriate.
- Bind it to the headless server through event adapters only.

### Milestone 6 - Client JavaFX UI

- Build authentication/identity flow, full chat view, read-only presentation, text area, `SEND`, Enter handling, `LEAVE`, online label/circle, message display, and errors.
- Bind it to the headless client through event adapters only.

### Milestone 7 - Packaging and acceptance

- Produce `TCPServer.jar` and `TCPClient.jar`, stage editable properties, and verify classpath and executable-JAR launches.
- Perform the complete automated and manual acceptance matrix on JDK 22.0.2/Windows with Java 21-compatible bytecode.

### Milestone 8 - UML and README

- Create editable and exported class, deployment, use case, and sequence UML diagrams.
- Expand the README with project overview, requirements, architecture, setup, configuration, commands, testing, diagrams, demo link, blog link, troubleshooting, and limitations.

### Milestone 9 - Demo and technical post

- Record/edit the three-minute code walkthrough and live demonstration using the script below.
- Draft, review, and publish the technical blog post; add its link to the README.

### Milestone 10 - Submission

- Confirm naming rules and student identity, run a clean release build, inspect every artifact/link, create the correctly named ZIP, extract-test it, and submit/share only after explicit user approval.

## UML deliverables

- **Class diagram:** launchers, JavaFX controllers, services, configuration, protocol types, server/session/user registry, client connection/state, interfaces, and key relationships. Keep UI dependencies pointing toward interfaces, not networking implementations.
- **Deployment diagram:** one server JVM/node, multiple client JVM/nodes, configurable IP/port, TCP links, external properties, and executable JARs.
- **Use case diagram:** named user, anonymous read-only user, and server operator; connect, authenticate/identify, receive, send, query active users, leave, monitor users, and inspect logs.
- **Sequence diagram 1:** connect, identity negotiation, welcome/presence update, send, server formatting, and sender-inclusive broadcast.
- **Sequence diagram 2:** `allUsers` private response plus `end`/`bye`/`LEAVE` disconnect and active-list update.
- Store diagram source (preferably PlantUML) and exported PNG or SVG files so both maintainability and submission readability are covered.

## README plan

The final README will include:

- Clear project description, feature list, screenshots, and assignment context.
- Java 21 or newer and Windows prerequisites, IntelliJ import instructions, and Maven Wrapper use.
- Module/package architecture and separation-of-concerns explanation.
- Properties keys, lookup locations, defaults, and CLI precedence.
- Build, test, classpath, and executable-JAR commands.
- Protocol and command summary, including anonymous behavior and broadcast semantics.
- Links/previews for all UML diagrams, the demo video, and the technical post.
- Troubleshooting for port conflicts, firewall prompts, JavaFX startup, invalid config, and connection failures.
- Known limitations and possible future improvements.

## Three-minute demo plan and script

Target length: 2:50 to 3:00, rehearsed before recording.

- **0:00-0:15 - Introduction:** "This is a Java 21-compatible multi-client TCP chat application with separate JavaFX server and client Maven modules. It uses virtual threads and keeps networking independent of the UI."
- **0:15-0:40 - Repository/build:** Show the two modules, Maven Wrapper, properties files, tests, README, and executable JARs. Briefly run or show the successful `mvnw.cmd clean verify` result.
- **0:40-1:05 - Architecture/code:** Show the class and deployment diagrams, then point to the headless server/client services, protocol types, and thin JavaFX adapters. Mention the exact `TCPServer`/`TCPClient` launchers.
- **1:05-1:25 - Start applications:** Launch `TCPServer.jar` and show `Server Started`/`Waiting for Client`. Launch two named clients with host/port arguments and one anonymous client from properties.
- **1:25-1:55 - Messaging:** Send by button and Enter from different named clients. Show username/time formatting, messages delivered once to every client including the sender, server welcome/log entries, and live colored user list.
- **1:55-2:15 - Read-only mode:** Show the anonymous display identity, disabled sending, ability to receive broadcasts, and its representation in the active-user list.
- **2:15-2:35 - Commands:** Run `allUsers` and show the private response. Disconnect one client with `bye` or `end`, then another with `LEAVE`; show server/UI updates.
- **2:35-2:50 - Configuration/testing:** Show editable properties and explain that valid client arguments override client defaults. Briefly show passing unit/integration tests.
- **2:50-3:00 - Close:** "The repository includes source, executable JARs, all UML diagrams, setup documentation, the demo link, and the technical article."

Recording checklist:

- Use readable font scaling and crop notifications or unrelated windows.
- Prebuild artifacts and prepopulate safe demo configuration.
- Keep server, three clients, terminal, diagrams, and code tabs ready before recording.
- Avoid dead time, credential exposure, personal notifications, and unverified claims.
- Add captions or a text description and verify the final video duration and sharing permissions.

## Technical blog post plan

Working title: "Building a Multi-Client JavaFX TCP Chat with Java 21 Virtual Threads."

Outline:

1. Project goal and requirements.
2. Why a central TCP server and line-framed protocol were chosen.
3. Two-module architecture and keeping JavaFX out of networking/business logic.
4. Virtual-thread client sessions and thread-safe active-user management.
5. Runtime properties plus command-line precedence.
6. Anonymous read-only clients, `allUsers`, and authoritative broadcasts.
7. JavaFX thread-boundary and UI update strategy.
8. Unit/integration testing with loopback sockets.
9. Executable-JAR packaging and exact launcher compatibility.
10. Challenges, tradeoffs, lessons learned, final screenshots, repository/demo links, and future improvements.

Do not publish until the implementation, diagrams, commands, screenshots, and links are final and technically verified.

## Commit-history plan

Commits will be created only when the user requests them. Suggested focused sequence:

1. `docs: capture project requirements and delivery plan`
2. `build: add Maven wrapper and application modules`
3. `feat(protocol): add configuration and wire messages`
4. `feat(server): support concurrent chat sessions`
5. `feat(client): add headless chat connection service`
6. `feat(server-ui): add monitoring interface`
7. `feat(client-ui): add chat interface and read-only mode`
8. `test: cover multi-client TCP workflows`
9. `build: package executable client and server jars`
10. `docs: add UML diagrams and complete README`
11. `docs: add demo and technical article links`

## Submission package plan

Release staging must include or clearly link:

- Complete `TCPServer` and `TCPClient` Maven source projects plus root Maven Wrapper.
- Verified `TCPServer.jar` and `TCPClient.jar` with external runtime properties.
- Class, deployment, use case, and both sequence diagrams in editable and viewable formats.
- Descriptive README and GitHub repository link with meaningful commit history.
- Three-minute demo video link with correct viewing permission.
- Published technical blog post link.
- Any instructor cover sheet or metadata required outside the supplied PDF.

Before packaging:

1. Read configurable student names and the project number, then derive `<StudentNames>_project<Number>.zip` or `.rar`; for the current individual submission use `Badreddine_Lechhab_project1.zip`.
2. Tag/check out or otherwise stage the exact verified revision without changing history unexpectedly.
3. Run `mvnw.cmd clean verify` and all executable-JAR smoke tests.
4. Build a clean staging directory that excludes `.git`, `.idea`, `target`, logs, secrets, and temporary files.
5. Reject an absent or zero-byte attachment and create the correctly named archive.
6. Extract the archive into a fresh directory, inspect for corruption and completeness, rebuild/test if expected, and verify every external link.
7. Submit through the required course/LMS channel only after explicit user approval; never use email because emailed submissions are discarded.

## Release gates

The project is not ready for submission until every mandatory checkbox in `docs/requirements.md` is complete, every planned optional deliverable is present, all ambiguity decisions are reflected consistently in code/docs/demo, automated verification passes, JAR launch tests pass, links are accessible, and the ZIP name/content have been independently checked.

## Next milestone

Milestone 2 is protocol and configuration only: implement and test external properties loading, client CLI-over-properties precedence, validation, explicit protocol frames, command parsing, identity rules, and lifecycle models. Keep the work headless and independent of JavaFX; server sockets, multi-client sessions, chat routing, and GUI screens remain later milestones.
