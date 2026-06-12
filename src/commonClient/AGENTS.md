# Agent Notes for Common Client Source

This source root owns loader-neutral client behavior.

- Do not import Fabric, NeoForge, ModMenu, Fabric Permissions API, or LuckPerms provider APIs here.
- Keep client lifecycle hooks, payload receivers, ModMenu entrypoints, and loader render hook wiring in loader client adapters.
- Common client may own shared screen models, client state, and texture selection helpers when they do not depend on loader APIs.
