# TCP Chat Wire Protocol

## Transport and framing

The client and server communicate over a Java TCP `Socket`. Every frame is:

1. A four-byte signed big-endian length written by `DataOutputStream.writeInt`.
2. Exactly that many UTF-8 bytes.

The maximum encoded frame length is 1,048,576 bytes. Zero, negative, oversized, incomplete, unknown, or malformed frames terminate or reject that connection without stopping the server accept loop.

The UTF-8 payload contains an uppercase frame type followed by zero or more tab-separated fields:

```text
TYPE<TAB>BASE64_UTF8_FIELD_1<TAB>BASE64_UTF8_FIELD_2...
```

Each field is independently Base64 encoded. Consequently chat text may safely contain tabs, newlines, Unicode, or strings that look like commands. Commands are recognized from complete client input before the `CHAT` frame is created; the server never searches message substrings for commands.

Writes to one connection are serialized under a per-connection lock, so frames from concurrent broadcasts cannot interleave.

## Handshake

The first client frame must be:

- `HELLO(username)`

The username field may be empty. The server validates it and responds with one of:

- `WELCOME(displayName, readOnlyBoolean)`
- `ERROR(reason)`, followed by connection close

A blank username receives a unique server-generated display name such as `Anonymous-1 (read-only)`. Named identity comparison is case-insensitive while preserving the accepted display spelling. A duplicate named identity is rejected.

## Established-session frames

Client to server:

- `CHAT(text)`: publish one named-user message.
- `USERS_REQUEST()`: request the active-user snapshot.
- `DISCONNECT()`: orderly close.

Server to client:

- `MESSAGE(formattedText)`: one server-timestamped broadcast.
- `USERS(displayName...)`: private, sorted active-user snapshot sent only to the requester.
- `ERROR(reason)`: recoverable command or validation error.
- `SHUTDOWN(reason)`: server is stopping; the connection then closes.

Anonymous `CHAT` frames are rejected by the server with `ERROR` even if a nonstandard client bypasses the official client's read-only guard.

## Application commands

The official client maps user input before sending:

- Exact `allUsers` becomes `USERS_REQUEST`.
- Case-insensitive `end`, `bye`, and `leave` become `DISCONNECT`.
- Other nonblank input from a named client becomes `CHAT`.

The Active Users button sends `USERS_REQUEST` directly and remains available to anonymous clients.

## Broadcast rule

For each accepted `CHAT` frame, the server creates exactly one string:

```text
[HH:mm:ss] DisplayName: original message
```

It sends that `MESSAGE` frame once to every active connection, including the sender. Clients never perform optimistic local echo, so the sender also sees exactly one authoritative copy.
