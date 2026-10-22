# Java TCP Chat

Repository URL: https://github.com/BadrTechie/java-tcp-chat

Remote push/access verification: **pending**

A Java 21 multi-client group chat with separate JavaFX server and client applications. The server uses TCP sockets and one virtual thread per client session. Named users can chat, anonymous users connect in read-only mode, and the server operator can monitor activity and connected users.

## Features

- Server UI with editable bind address/port, Start/Stop controls, activity log, and a live colored user list.
- Client UI with editable connection details and optional username, chat history, composer, Send, Enter-to-send, Leave, Active Users, and online/offline status.
- Unique server-generated anonymous labels and read-only enforcement in both client and server business code.
- Server-authoritative timestamps and sender-inclusive broadcasts with no optimistic client echo.
- Private `allUsers` replies, duplicate-name rejection, clean disconnect aliases, abrupt-loss cleanup, and server-shutdown notification.
- Length-prefixed UTF-8 protocol with Base64-encoded fields, bounded frames, and safe multiline/delimiter content.
- Headless networking code covered by loopback integration tests.

## Requirements

Runtime:

- Windows x64.
- Java 21 or newer. Packaged startup was verified with Temurin JDK 22.0.2.
- The supplied JARs include JavaFX 21.0.2 and its Windows native libraries; no separate JavaFX SDK is required.

Build:

- A JDK 21+ (`javac` is required).
- PowerShell.
- Internet access on the first build so the Maven Wrapper can populate the local Maven cache.
- No global Maven installation is needed.

The compiler always uses Java release 21 with no preview features, even when Maven runs on JDK 22.

## Architecture

Each application has exactly two architectural layers:

- `business`: configuration, validation, protocol framing, sockets, concurrent connection state, commands, routing, and lifecycle. This layer has no JavaFX imports and works in headless tests.
- `presentation`: JavaFX `GridPane` screens, CSS, UI event handlers, and rendering of events received through plain Java callback interfaces.

Thin default-package launchers `TCPServer` and `TCPClient` delegate to packaged bootstraps and remain separate from the JavaFX `Application` subclasses.

```text
JavaFX presentation
        |
        | plain method calls and callback interfaces
        v
business (configuration, protocol, sockets, state)
```

There is no persistence, repository, service, or controller layer.

## Configuration

Configuration is external and resolved relative to the process working directory:

- `config/server.properties`
  - `server.host`: bind address, such as `0.0.0.0`
  - `server.port`: bind port; `0` requests an OS-assigned port and is mainly useful for tests
- `config/client.properties`
  - `server.host`: default server hostname/address
  - `server.port`: default server port

Start the applications from the repository root so these paths resolve. The UI fields are populated from the files and remain editable before connection/start.

For `TCPClient`, either supply no arguments or exactly two:

```text
java -jar TCPClient.jar [serverHost serverPort]
```

Two valid arguments override both client properties and prefill the UI. Partial or invalid arguments display an actionable configuration error and never silently fall back. Usernames are always entered live in the client UI and may be left blank for read-only mode. No production users, conversations, or machine-specific addresses are seeded in code.

## Build and test

From the repository root:

```powershell
.\mvnw.cmd clean verify
```

The build produces:

- `tcp-server\target\TCPServer.jar`
- `tcp-client\target\TCPClient.jar`

The verification suite uses ephemeral loopback ports and bounded waits. It covers configuration/CLI precedence, invalid input, multiple clients, sender-inclusive broadcasts without duplication, safe message framing, anonymous rejection in both client and server, private active-user replies, duplicate names, `allUsers`, `end`, `bye`, orderly/abrupt cleanup, and server shutdown.

Verified result: `.\mvnw.cmd clean verify` passed 10 tests on Temurin JDK 22.0.2 while compiling with Java release 21.

## Run

Run from the repository root. Launch the server first, then click **START** in its UI:

```powershell
java -jar .\tcp-server\target\TCPServer.jar
```

Launch clients using configuration defaults:

```powershell
java -jar .\tcp-client\target\TCPClient.jar
```

Or override and prefill the server endpoint:

```powershell
java -jar .\tcp-client\target\TCPClient.jar localhost 3000
```

One server plus three client processes:

```powershell
Start-Process java -ArgumentList '-jar', '.\tcp-server\target\TCPServer.jar'
# Click START in the server window before connecting clients.
Start-Process java -ArgumentList '-jar', '.\tcp-client\target\TCPClient.jar', 'localhost', '3000'
Start-Process java -ArgumentList '-jar', '.\tcp-client\target\TCPClient.jar', 'localhost', '3000'
Start-Process java -ArgumentList '-jar', '.\tcp-client\target\TCPClient.jar', 'localhost', '3000'
```

The assignment-compatible class names can also be invoked from the shaded JARs:

```powershell
java -cp .\tcp-server\target\TCPServer.jar TCPServer
java -cp .\tcp-client\target\TCPClient.jar TCPClient localhost 3000
```

## Commands and behavior

- `allUsers`: requests a private active-user list. Anonymous users use the **ACTIVE USERS** button.
- `end`, `bye`, or `leave`: cleanly disconnects.
- **LEAVE** and window close also disconnect cleanly.
- Enter sends for named users; Shift+Enter inserts a newline.
- Every accepted message is formatted once by the server as `[HH:mm:ss] Username: message` and broadcast once to every active client, including its sender.

See [docs/protocol.md](docs/protocol.md) for the wire format.

## User-verified GUI results

The following results, and only these results, were reported as manually verified:

- Messages reach each connected client once.
- `allUsers` responds only to its requester.
- Anonymous clients receive messages but cannot send.
- `bye` disconnects correctly.
- Server **STOP** changes remaining clients to Offline.

See [the manual verification record](docs/manual-verification.md) for the explicitly unclaimed checks.

## UML diagrams

Each diagram has editable PlantUML plus SVG and PNG exports:

| Diagram | SVG | PNG | Source |
| --- | --- | --- | --- |
| Class | [View](docs/uml/class-diagram.svg) | [View](docs/uml/class-diagram.png) | [PlantUML](docs/uml/class-diagram.puml) |
| Deployment | [View](docs/uml/deployment-diagram.svg) | [View](docs/uml/deployment-diagram.png) | [PlantUML](docs/uml/deployment-diagram.puml) |
| Use case | [View](docs/uml/use-case-diagram.svg) | [View](docs/uml/use-case-diagram.png) | [PlantUML](docs/uml/use-case-diagram.puml) |
| Connection and broadcast sequence | [View](docs/uml/sequence-connection-broadcast.svg) | [View](docs/uml/sequence-connection-broadcast.png) | [PlantUML](docs/uml/sequence-connection-broadcast.puml) |
| Active users and disconnect sequence | [View](docs/uml/sequence-users-disconnect.svg) | [View](docs/uml/sequence-users-disconnect.png) | [PlantUML](docs/uml/sequence-users-disconnect.puml) |

[Deployment notes and render instructions](docs/uml/README.md) explain the production/multi-machine view separately from the one-PC `localhost` demo.

## IntelliJ IDEA

Open the root `pom.xml`, select JDK 21+ (JDK 22.0.2 is supported), choose **Use Maven wrapper**, reload Maven projects, and run the root `verify` lifecycle. Run packaged JARs from the repository root so external configuration is found.

## Known limitations and remaining deliverables

- The packaged JARs contain Windows JavaFX natives and are Windows-specific. Rebuild with the appropriate OpenJFX platform dependencies for another operating system.
- Shaded JavaFX starts from the unnamed module and may log a non-fatal "unsupported JavaFX configuration" warning.
- The protocol is plain TCP without TLS, authentication, private messages, or persistent history.
- Manual GUI checks not listed in the user-verified section remain unclaimed.

## Submission checklist

- [x] Working source, Maven Wrapper, tests, executable JAR build, protocol documentation, and descriptive README.
- [x] Editable and rendered class, deployment, use-case, and sequence diagrams.
- [x] Timed [three-minute demo script](docs/demo-script.md).
- [x] Publish-ready [technical article draft](docs/technical-post.md).
- [ ] Demo recording and accessible recording link.
- [ ] Technical article publication and published link.
- [ ] GitHub push and public/access verification.
- [ ] Final archive named `<StudentNames>_project1.zip`. For the current individual submission, the planned concrete name is `Badreddine_Lechhab_project1.zip`.
- [ ] Verify every submitted attachment is present, nonempty, readable, and valid; extract-test the final ZIP.
- [ ] Submit through the required course/LMS channel. Email submissions are not accepted.

## Project documents

- [Requirements checklist](docs/requirements.md)
- [Implementation and delivery plan](docs/plan.md)
- [Wire protocol](docs/protocol.md)
- [UML index](docs/uml/README.md)
- [Three-minute demo script](docs/demo-script.md)
- [Technical article draft](docs/technical-post.md)
- [Manual GUI verification](docs/manual-verification.md)
