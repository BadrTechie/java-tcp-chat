# Implementation and Delivery Plan

## Current status

Milestones 0 (requirements/planning), 1 (Maven foundation), 2 (working application/release JARs), and 3 (documentation) are complete. The repository contains a Java 21 two-module Maven build, JavaFX server and client applications, headless business layers, automated loopback tests, external properties, executable Windows JARs, rendered UML, a timed demo script, and a publish-ready technical article.

Remaining release work is deliberately external or destructive: record/share the demo, publish/link the article, push and verify GitHub access, complete any desired additional manual checks/screenshots, then create and extract-test the final submission archive.

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

## Manual verification status

The user verified only these GUI results: each connected client receives each message once; `allUsers` is private to its requester; anonymous clients receive but cannot send; `bye` disconnects; and server STOP changes remaining clients to Offline.

No other manual GUI check is claimed as passed. `docs/manual-verification.md` lists examples that remain unclaimed, even where automated business-layer coverage exists.

## Milestone 3 - Documentation

Status: completed on 2026-10-22.

- Added implementation-derived class, deployment, use-case, connection/broadcast sequence, and active-users/disconnect sequence diagrams.
- Stored editable PlantUML and rendered SVG/PNG under `docs/uml/`; PlantUML 1.2024.8 parsed and rendered all five sources without syntax errors.
- Documented the multi-machine deployment and the separate one-PC `localhost:3000` demo arrangement.
- Added `docs/demo-script.md` with an exact 180-second source and live-demo walkthrough.
- Added `docs/technical-post.md` with source-accurate architecture, protocol, concurrency, JavaFX, testing, and packaging explanations.
- Updated requirements and README links/status without marking recording, publication, push/access verification, or archive work complete.

## Next milestone: publication and submission

1. Rehearse and record the three-minute demo; upload it and verify the recording link is accessible.
2. Publish `docs/technical-post.md` and add the published URL.
3. Complete any desired remaining manual GUI checks and capture final screenshots without changing the already recorded verification scope.
4. After explicit approval, push the focused commit history and verify `https://github.com/BadrTechie/java-tcp-chat`.
5. From a clean verified revision, create `<StudentNames>_project1.zip` (currently `Badreddine_Lechhab_project1.zip`), excluding `.git`, IDE output, `target` directories, logs, and temporary files while including/linking every required artifact.
6. Confirm every attachment is present, nonempty, readable, and valid; extract-test the ZIP and verify every external link.
7. Submit through the course/LMS channel only. Email submissions are not accepted.

Do not push, publish, record, or create the final archive without the user's explicit request.
