# Agent Notes for Common Source

This source root owns loader-neutral MultiGolem behavior.

- Do not import Fabric, NeoForge, ModMenu, Fabric Permissions API, or LuckPerms provider APIs here.
- Keep permission provider calls, loader event registration, networking transport, config paths, metadata lookup, and mixin declarations in loader adapters.
- Common may own permission node names and default policy.
