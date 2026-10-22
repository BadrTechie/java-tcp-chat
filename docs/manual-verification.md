# Manual GUI verification

Recorded from the user's manual test on 2026-10-22.

## User-verified results

- Messages reach each connected client once.
- `allUsers` responds only to its requester.
- Anonymous clients receive messages but cannot send.
- `bye` disconnects correctly.
- Server **STOP** changes remaining clients to Offline.

## Not claimed as manually verified

No other GUI behavior is marked as manually passed by this record. In particular, this record does not claim completion of visual/CSS inspection, stable color inspection, Send-versus-Enter comparison, Shift+Enter behavior, duplicate-name UI messaging, failed-connection UI messaging, `end`, **LEAVE**, window-close behavior, or abrupt process termination. Some of those behaviors have automated business-layer coverage, but that is separate from manual GUI verification.
