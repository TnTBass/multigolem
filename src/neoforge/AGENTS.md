# Agent Notes for NeoForge Source

This source root owns NeoForge server/common adapter code.

- NeoForge loader APIs, metadata lookup, config path lookup, event registration, storage, networking, permission checks, and NeoForge resources belong here.
- Delegate loader-neutral behavior to common code instead of duplicating it.
- During the skeleton phase, keep this root limited to metadata and empty entrypoints.
