# Multiloader Verification Matrix

## Fabric

- Repository root: `git rev-parse --show-toplevel`
- Unit tests: `.\gradlew.bat test`
- Full build: `.\gradlew.bat build`
- Common import scan: `.\gradlew.bat checkCommonSourceSetsLoaderNeutral`
- Jar metadata inspection: verify Fabric jar includes `fabric.mod.json`, Fabric mixin JSON files, icon assets, generated build metadata, Fabric entrypoints, and ModMenu entrypoint.
- Permissions default/provider override checks: verify tier create/heal nodes default to allowed, `multigolem.admin.bypass` defaults to false, provider denials are respected, and OP is not an implicit bypass.
- ModStatus payload checks: verify version/build payload fields, legacy/structured decode, timeout/state transitions, and mismatch display state.
- Server customizations payload checks: verify payload field order, defaults, limits, and client snapshot update/reset behavior.
- Vanilla-client compatibility: verify the server only sends custom payloads to clients that advertise support.
- Manual playtest checklist: run existing Fabric client/server checklist before release prep.
- Release-source checks: verify Fabric jar and sources jar names are loader-suffixed before publishing paths depend on them.

## NeoForge

- Repository root: `git rev-parse --show-toplevel`
- Unit tests: `.\gradlew.bat :neoforge:test`
- Full build: `.\gradlew.bat :neoforge:build`
- Common import scan: `.\gradlew.bat checkCommonSourceSetsLoaderNeutral`
- Jar metadata inspection: verify NeoForge jar includes `META-INF/neoforge.mods.toml`, NeoForge mixin declarations if present, icon assets, generated build metadata, and NeoForge entrypoints.
- Permissions default/provider override checks: verify registered NeoForge permission nodes match Fabric defaults unless an implementation-time API limitation is reviewed and documented.
- ModStatus payload checks: verify NeoForge version/build lookup feeds the same common payload contract.
- Server customizations payload checks: verify NeoForge transport preserves the common payload field contract and limits.
- Vanilla-client compatibility: verify NeoForge sends custom payloads only when the client supports them.
- Manual playtest checklist: add and run NeoForge singleplayer, dedicated server, vanilla-client fallback, permissions, ModStatus, customizations, save/load, marked eggs, and zombie golem rows before release prep.
- Release-source checks: verify NeoForge jar and sources jar names are loader-suffixed before publishing paths depend on them.
