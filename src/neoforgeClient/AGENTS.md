# Agent Notes for NeoForge Client Source

This source root owns NeoForge client adapter code.

- NeoForge client lifecycle hooks, payload receivers, client-only event wiring, and NeoForge client resources belong here.
- Shared UI state and screen code may move to common-client only when it does not depend on loader APIs.
- During the skeleton phase, keep this root limited to empty client entrypoints.
