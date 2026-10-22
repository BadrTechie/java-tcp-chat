# UML diagrams

Each diagram is stored as editable PlantUML plus rendered SVG and PNG. The diagrams are derived from the implementation in `tcp-server/src/main/java` and `tcp-client/src/main/java`.

## Files

- `class-diagram`: launchers, presentation classes, business classes, APIs, workflow methods, nested protocol types, and relationships.
- `deployment-diagram`: server and multiple client machines/JVMs, executable JARs, external properties, and TCP/IP connections.
- `use-case-diagram`: named user, anonymous read-only user, and server operator behavior.
- `sequence-connection-broadcast`: connection/identity negotiation and one sender-inclusive broadcast.
- `sequence-users-disconnect`: private active-user response, clean client disconnection, and server STOP.

## Two-layer rule

The server and client each contain exactly two architectural layers:

1. `business`: configuration, validation, framing, networking, users, commands, messages, concurrency, and lifecycle.
2. `presentation`: JavaFX controls, UI events, callback display, and `Platform.runLater` handoff.

`TCPServer`, `TCPClient`, `ServerBootstrap`, and `ClientBootstrap` are thin entry-point adapters, not additional architectural layers.

## Deployment interpretation

The deployment diagram first shows the logical/production arrangement: one Windows x64 server machine runs `TCPServer.jar`, and multiple Windows x64 client machines each run `TCPClient.jar`. Every process requires Java 21 or newer. The server reads `config/server.properties`; clients read `config/client.properties` unless two CLI arguments override and prefill host/port.

For the localhost demo, all four processes run on one PC: one server JVM and three separate client JVMs. The clients use `localhost:3000`, but they still communicate through real TCP sockets and retain separate JavaFX windows and process state.

## Re-render

These exports were rendered with PlantUML 1.2024.8:

```powershell
$plantUmlJar = Join-Path $env:USERPROFILE '.m2\repository\net\sourceforge\plantuml\plantuml\1.2024.8\plantuml-1.2024.8.jar'
java -DPLANTUML_LIMIT_SIZE=8192 -jar $plantUmlJar -tsvg .\docs\uml\*.puml
java -DPLANTUML_LIMIT_SIZE=8192 -jar $plantUmlJar -tpng .\docs\uml\*.puml
```
