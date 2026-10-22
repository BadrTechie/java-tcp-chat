# Implementation and Delivery Plan

## Current status

Milestones 0 (requirements/planning), 1 (Maven foundation), and 2 (working application/release JARs) are complete. The repository contains a Java 21 two-module Maven build, JavaFX server and client applications, headless business layers, automated loopback tests, external properties, and executable Windows JARs.

The remaining milestone is documentation and submission collateral: UML, manual GUI acceptance, demo recording, technical post, remote GitHub verification, and the final inspected submission archive. These remain in scope but were intentionally not created during the application-recovery milestone.

## Authoritative two-layer architecture

Each application has only:

1. **Business layer** (`business` package): configuration, validation, protocol framing, sockets, connected-user management, commands, message routing, thread safety, and lifecycle.
2. **Presentation layer** (`presentation` package): JavaFX `GridPane` screens, CSS, UI events, and display of business callbacks.

Thin `TCPServer`/`TCPClient` entry points and their packaged bootstraps only launch the presentation. They do not form an architectural layer.

Dependency direction is presentation to business. The business layer never imports JavaFX. Plain callback interfaces (`ServerEventListener` and `ClientEventListener`) carry events to presentation code, where `Platform.runLater` performs the only JavaFX-thread handoff.

There are no persistence, repository, service, controller, model, networking, configuration, or protocol layers/packages beyond focused classes contained in the required two layers.

## Implemented design decisions

1. **Runtime data provenance:** properties and editable fields provide endpoints; the client UI provides the optional username. Production code has no fake users, seeded messages, or machine-specific addresses.
2. **Configuration precedence:** `config/server.properties` and `config/client.properties` are editable defaults. Exactly two client CLI arguments override both client values and prefill the UI. Partial/invalid arguments do not fall back.
3. **Anonymous identity:** a blank username receives a unique `Anonymous-N (read-only)` label. Anonymous clients receive broadcasts and request active users but cannot publish; both client and server enforce the restriction.
4. **Broadcasts:** the server timestamps and sends one authoritative message to every active connection, including the sender. Clients do not locally echo.
5. **Active users:** `allUsers` and the Active Users button produce a private snapshot only for the requester.
6. **Identity uniqueness:** named identities are unique case-insensitively and duplicates receive a clear rejection.
7. **Protocol:** frames use a four-byte length plus UTF-8 payload. Base64-encoded fields keep tabs, Unicode, and multiline message content safe. Per-connection write locks prevent concurrent frame interleaving.
8. **Concurrency/lifecycle:** the accept loop and each client session use virtual threads. Concurrent maps protect registries. Client/server close paths are idempotent and handle orderly disconnect, abrupt loss, and shutdown.
9. **Packaging:** Maven Shade creates `TCPServer.jar` and `TCPClient.jar` with JavaFX dependencies and Windows x64 native libraries. Thin launchers remain separate from JavaFX `Application` classes.

## Verification completed

- Configuration loading, CLI precedence, missing/invalid inputs.
- Ephemeral loopback server ports and bounded waits.
- Three simultaneous clients including anonymous read-only.
- Multiline/special-content framing and sender-inclusive delivery exactly once.
- Private active-user results and anonymous Active Users support.
- Client-side and server-side anonymous send rejection.
- Duplicate named identity rejection.
- `allUsers`, `end`, and `bye` command handling.
- Normal and abrupt disconnect cleanup.
- Server shutdown notification and client cleanup.
- Business source scan confirmed no JavaFX imports.
- `.\mvnw.cmd clean verify` passed 10 tests on Temurin JDK 22.0.2 with release 21.
- Both executable GUI JARs remained alive with no startup exception during a bounded smoke launch.

## Remaining manual acceptance

Run one server, two named clients, and one anonymous client. Confirm:

- GridPane layout and CSS render correctly at normal Windows scaling.
- Server Start/Stop, activity log, and live uniquely colored user rows update visibly.
- Named Send and Enter work; Shift+Enter creates a newline.
- Each client shows one copy of each broadcast with server time and sender.
- Anonymous mode disables message entry/Send while receiving broadcasts and using Active Users.
- Duplicate-name and failed-connection errors are understandable.
- `allUsers` is private; Leave, `end`, `bye`, window close, abrupt termination, and server Stop update all statuses and lists.

## Next milestone: deliverables and submission

1. Create editable PlantUML plus exported class, deployment, use-case, connection/broadcast sequence, and active-users/disconnect sequence diagrams under `docs/uml/`.
2. Execute the manual GUI checklist and capture final screenshots.
3. Finalize and record the rehearsed three-minute demo with code walkthrough and live server/two named/one anonymous client behavior.
4. Draft, review, and publish the technical article; add its link and the demo link to the README.
5. After explicit approval, push the focused commit history and verify `https://github.com/BadrTechie/java-tcp-chat`.
6. From a clean verified revision, create `Badreddine_Lechhab_project1.zip` excluding `.git`, IDE output, `target` directories, logs, and temporary files while including/linking every required artifact.
7. Extract-test the archive, verify it is non-empty/readable and every link works, then submit through the course/LMS channel only after explicit approval.

Do not push, publish, record, or create the final archive without the user's explicit request.
