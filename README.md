# Java TCP Chat

A university multi-client TCP chat project with separate server and client applications. The project is being delivered incrementally; this revision provides the Maven foundation only.

## Current milestone

Implemented:

- Root Maven aggregator with `tcp-server` and `tcp-client` application modules.
- Official Maven Wrapper 3.3.2 pinned to Apache Maven 3.9.16.
- Java compiler release 21 with no preview features.
- Pinned JavaFX 21.0.2 and JUnit 5.10.2 dependencies.
- Thin `TCPServer` and `TCPClient` launchers that delegate to packaged bootstraps.
- External server/client properties templates and JUnit launcher smoke tests.

Not implemented yet:

- TCP sockets, protocol messages, client sessions, virtual-thread handling, chat behavior, or configuration loading.
- JavaFX `Application` classes, controllers, layouts, CSS, or GUI screens.
- Final executable `TCPServer.jar` and `TCPClient.jar` artifacts.

The normal module JARs produced now are build skeletons and must not be treated as the final executable deliverables.

## Requirements

- Windows PowerShell.
- JDK 21 or newer. Development verification uses Temurin JDK 22.0.2 while Maven compiles with `--release 21`.
- No global Maven installation is required.

Check Java:

```powershell
java -version
javac -version
```

## Build and test

Run all commands from the repository root:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

Build one module and its required parent project:

```powershell
.\mvnw.cmd -pl tcp-server -am package
.\mvnw.cmd -pl tcp-client -am package
```

The wrapper downloads its pinned Maven distribution into the user's Maven cache on first use.

## Run the milestone placeholders

After `.\mvnw.cmd clean verify`, run the non-GUI placeholder launchers from the root:

```powershell
java -cp .\tcp-server\target\tcp-server-0.1.0-SNAPSHOT.jar TCPServer
java -cp .\tcp-client\target\tcp-client-0.1.0-SNAPSHOT.jar TCPClient localhost 3000
```

Each launcher prints that networking and JavaFX are not implemented, then exits. Client arguments are intentionally ignored until the configuration/protocol milestone.

## IntelliJ IDEA import

1. Open IntelliJ IDEA and choose **Open**.
2. Select the repository root (or its root `pom.xml`) and trust the project.
3. When prompted, import it as a Maven project.
4. Set the Project SDK to the installed JDK 22.0.2, or another JDK 21+.
5. In **Settings > Build, Execution, Deployment > Build Tools > Maven**, select **Use Maven wrapper** for Maven home.
6. Set the Maven importer and runner JDK to the same JDK 21+ installation.
7. Reload all Maven projects and run the root `verify` lifecycle.

The POM controls Java language compatibility with compiler release 21 even when IntelliJ and Maven run on JDK 22.

## Repository structure

```text
.
|-- config/
|   |-- client.properties
|   `-- server.properties
|-- tcp-client/
|   `-- src/{main,test}/{java,resources}
|-- tcp-server/
|   `-- src/{main,test}/{java,resources}
|-- docs/
|-- .mvn/wrapper/
|-- mvnw
|-- mvnw.cmd
`-- pom.xml
```

## Runtime configuration decisions

- `config/server.properties` will provide the server bind host and port.
- `config/client.properties` will provide client defaults.
- Two valid client command-line arguments will override the client properties.
- Invalid or partial client arguments will fail with usage information rather than silently falling back.

Configuration loading is planned but not implemented in this milestone.

## Accepted behavior decisions

- Anonymous connections receive unique labels such as `Anonymous-1 (read-only)`, appear in active-user results, receive broadcasts, and cannot send chat messages.
- The server will broadcast each accepted message once to every active connection, including the sender; clients will not perform a local optimistic echo.
- Thin default-package `TCPServer` and `TCPClient` launchers remain separate from future JavaFX `Application` subclasses.

## Documentation

- [Requirements checklist](docs/requirements.md)
- [Implementation and delivery plan](docs/plan.md)

## Submission rule

The archive name is configurable as `<StudentNames>_project<Number>.zip` or `.rar`. The current individual submission must be named `Badreddine_Lechhab_project1.zip`. The final attachment must be non-empty, readable, and uncorrupted, and must be submitted through the required course/LMS channel rather than email.
