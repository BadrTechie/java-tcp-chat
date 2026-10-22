# Repository Working Agreement

## Purpose and scope

This repository contains a university mini-project: a multi-client TCP chat system with separate JavaFX server and client applications. Work must proceed one clearly bounded milestone at a time.

The assignment PDF is the primary source of truth. `docs/requirements.md` is the traceable checklist, and `docs/plan.md` records implementation decisions where the PDF is ambiguous. If these documents conflict with the PDF, stop and resolve the conflict before implementing more code.

## Incremental workflow

For every milestone:

1. Read the applicable requirements and the current plan before changing files.
2. State the milestone scope and avoid implementing later milestones early.
3. Inspect the working tree and preserve unrelated or user-owned changes.
4. Make the smallest coherent change that completes the milestone.
5. Add or update focused tests when production behavior changes.
6. Run the narrowest relevant checks first, then the full Maven verification when practical.
7. Update requirements, plan, README, diagrams, or other documentation affected by the change.
8. Explain the completed milestone in the final handoff: what changed, why, validation performed, known limitations, and the next milestone.

Do not commit, push, tag, publish, upload, or create the final submission archive unless the user explicitly requests that action.

## Architecture boundaries

- Keep networking and business logic independent of JavaFX.
- Code in model, protocol, configuration, and networking packages must not import `javafx.*`.
- JavaFX controllers and views may call application services through small interfaces; they must not own sockets, blocking I/O loops, connection registries, or protocol parsing.
- Keep dependency direction inward: JavaFX view/controller -> application service -> model/network abstractions.
- Isolate JavaFX thread handoff at the UI boundary. Use `Platform.runLater` only in JavaFX adapters/controllers, never in the networking model.
- Make server and client networking components usable in headless tests.
- Use explicit lifecycle methods and make disconnect/close operations idempotent.
- Keep shared protocol rules documented and compatible across the two applications.
- Use virtual threads for concurrent server client sessions as required by the assignment.

Recommended package boundaries are `io.github.badrtechie.tcpchat.server` and `io.github.badrtechie.tcpchat.client`, each split into `config`, `model`, `network`, `service`, and `ui` packages as needed. Thin default-package launchers named `TCPServer` and `TCPClient` are the only planned exception; they preserve the assignment's exact entry-point names and delegate immediately to packaged code.

## Build and module conventions

- Compile with Java release 21 and no preview features. Maven may run on the installed JDK 22.0.2.
- Use a root Maven aggregator with exactly two application modules: `tcp-server` and `tcp-client`.
- Use the committed Maven Wrapper (`mvnw`, `mvnw.cmd`, and `.mvn/wrapper/*`) because `mvn` is not available on PATH.
- Run Windows commands with `mvnw.cmd`, not a globally installed Maven executable.
- Keep runtime configuration external and editable. Do not hard-code host names or ports.
- Produce executable application artifacts named `TCPServer.jar` and `TCPClient.jar` that run with `java -jar`.
- Do not check in Maven `target/` directories, IDE build output, logs, temporary files, or generated demo data.

## Coding conventions

- Prefer small classes with one responsibility and descriptive names.
- Use UTF-8 for source, configuration, protocol text, and documentation.
- Validate host, port, username, and protocol input at boundaries and show actionable errors.
- Do not silently swallow exceptions. Convert low-level failures into meaningful status or log events.
- Make protocol message types and command parsing explicit; avoid scattered string checks.
- Keep timestamps and formatting deterministic enough to test.
- Document public behavior where names and types alone are insufficient; avoid comments that merely restate code.

## Testing conventions

- Use JUnit 5 through Maven Surefire.
- Unit-test configuration precedence, validation, protocol parsing, user registry behavior, command handling, formatting, and lifecycle state.
- Integration-test the headless server and clients over loopback sockets, including multiple concurrent clients.
- Test anonymous read-only behavior, `allUsers`, broadcasts, disconnect aliases, abrupt disconnects, and resource cleanup.
- Keep UI logic thin so most behavior is testable without launching JavaFX.
- Treat a successful `mvnw.cmd clean verify` and executable-JAR smoke test as release gates.

## Documentation and deliverables

- Keep `docs/requirements.md` traceable to the PDF and update checkbox status only when evidence exists.
- Keep `docs/plan.md` current when an ambiguity is resolved or sequencing changes.
- Maintain a descriptive root `README.md` with prerequisites, architecture, configuration, build, run, test, screenshots/demo links, diagrams, and known limitations.
- Store editable UML sources and exported images under `docs/uml/` when that milestone begins.
- Plan and preserve all mandatory and optional deliverables, including the demo script, technical post, executable JARs, and submission ZIP.
- After every completed milestone, append or update the milestone record below and explain the same result in the user handoff.

## Version-control conventions

- Create focused commits only when the user asks for commits.
- Use imperative, meaningful messages such as `build: add Maven wrapper and modules` or `feat(server): broadcast messages with virtual threads`.
- Do not combine unrelated formatting, IDE metadata, generated artifacts, and feature changes in one commit.
- Never rewrite published history or force-push without explicit approval.

## Milestone record

### Milestone 0 - Requirements and planning

Status: completed on 2026-10-22.

The assignment PDF and initial repository were reviewed. The complete requirement checklist, architecture and delivery plan, ambiguity resolutions, validation strategy, documentation plan, demo script outline, blog outline, and submission packaging strategy were recorded. No application code, build files, commits, pushes, or release archives were created.

### Milestone 1 - Maven foundation

Status: completed on 2026-10-22.

The root aggregator, `tcp-server` and `tcp-client` modules, official Maven Wrapper, Java release 21 build, pinned JavaFX/JUnit dependencies, external properties templates, thin launchers, packaged bootstraps, resource/test directories, and launcher tests were added. Maven 3.9.16 ran on Temurin JDK 22.0.2, `.\mvnw.cmd clean verify` passed, and emitted classes target Java 21. The module JARs are skeleton build outputs, not final executable deliverables.

### Next milestone - Protocol and configuration

Implement and test external properties loading, client CLI precedence, validation, protocol frames, command parsing, identity rules, and lifecycle models. Keep this milestone headless: do not add server sockets, chat routing, JavaFX screens, or final executable-JAR packaging yet.
