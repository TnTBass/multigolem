# Multiloader Boundary Rules

- Common code owns behavior, data models, payload semantics, permission node names, and default policy.
- Loader adapters own loader API calls, lifecycle/event registration, config paths, metadata lookup, permission API checks, payload transport, mixin declarations, and loader resources.
- Common and common-client code must not import `net.fabricmc`, `net.neoforged`, `com.terraformersmc.modmenu`, or `me.lucko.fabric`.
- Common and common-client source roots must not import `me.lucko.fabric`. The Fabric adapter may compile against the Fabric Permissions API (`me.lucko.fabric.api.permissions`) as a loader-adapter-only dependency.
- LuckPerms remains a runtime server provider behind loader permission APIs; no source root compiles against LuckPerms itself.
- Fabric permissions use Fabric API's current permission API.
- NeoForge permissions use NeoForge's built-in permission API and registered nodes.
- OP/game-master fallback is not an implicit provider-granted bypass.
- Build outputs are loader-specific jars, never a universal jar.
