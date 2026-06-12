# Agent Notes for Fabric Source

This source root owns Fabric server/common adapter code.

- Fabric loader APIs, event registration, attachment registration, config path lookup, metadata lookup, payload transport, permission API checks, and Fabric resources belong here.
- Delegate loader-neutral behavior to common code instead of duplicating it.
