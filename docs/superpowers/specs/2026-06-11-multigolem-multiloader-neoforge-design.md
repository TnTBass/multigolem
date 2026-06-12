# MultiGolem Multiloader / NeoForge Design Spec

## Goal

Begin the MultiGolem multiloader phase with a Fabric-preserving migration that reshapes the repository for separate Fabric and NeoForge loader jars before any NeoForge gameplay adapter code is added.

The first implementation phase must keep the current Fabric jar behavior stable while separating common domain code from Fabric-owned bootstrap, attachments, events, networking, permissions, ModMenu, metadata, resources, and publishing paths. NeoForge support comes after that physical shape exists and passes Fabric regression checks.

## Current MultiGolem Shape

MultiGolem is currently one Fabric Loom project:

- Root Gradle build applies `net.fabricmc.fabric-loom`, uses `splitEnvironmentSourceSets()`, and packages one jar named from `archives_base_name=multigolem`.
- Main Fabric metadata is `src/main/resources/fabric.mod.json`.
- Common-looking code lives in `src/main/java/dev/charles/multigolem`, but many classes import Fabric APIs directly.
- Client code lives in `src/client/java/dev/charles/multigolem/client` and includes Fabric client lifecycle/networking, ModMenu integration, and client mixins.
- Server config is `config/multigolem.json`, loaded through `FabricLoader`.
- Build metadata is generated into `multigolem-build.properties`.
- ModStatusKit is embedded under `dev.charles.multigolem.internal.modstatus`.
- Release automation assumes one Fabric jar and one sources jar.

There are no repo `AGENTS.md` files at the time of this planning pass.

## Reference Lessons Applied

The CarryBabyAnimals pilot is useful as a migration pattern, not as architecture to copy. The lessons that apply directly here are:

- Preserve Fabric first and treat the current Fabric player/server behavior as the regression baseline.
- Move into a physical multiloader-shaped source layout before NeoForge implementation.
- Keep loader-specific APIs out of common/common-client code with static scans.
- Produce loader-specific jars, not a universal jar.
- Suffix jar and sources artifact names by loader before CI, GitHub Release, Modrinth, or CurseForge publish paths depend on them.
- Modernize permissions as a cross-loader slice.
- Send the written spec and implementation plan through Revue before implementation starts.

MultiGolem-specific differences are significant: Fabric attachment APIs currently back identity, legacy variant, spawn-origin, and ability-state persistence; abilities directly register Fabric events; permissions compile directly against `me.lucko.fabric`; ModStatus version lookup directly calls `FabricLoader`; and release scripts have single-loader assumptions throughout.

## Recommended Approach

Use a staged, physical multiloader layout in this repository:

```text
src/
  common/
    java/dev/charles/multigolem/...
    resources/assets/multigolem/...
  commonClient/
    java/dev/charles/multigolem/client/...
  fabric/
    java/dev/charles/multigolem/fabric/...
    resources/fabric.mod.json
    resources/multigolem.mixins.json
  fabricClient/
    java/dev/charles/multigolem/fabric/client/...
    resources/multigolem.client.mixins.json
  neoforge/
    java/dev/charles/multigolem/neoforge/...
    resources/META-INF/neoforge.mods.toml
  neoforgeClient/
    java/dev/charles/multigolem/neoforge/client/...
```

Phase 1 creates and verifies `common`, `commonClient`, `fabric`, and `fabricClient`. It may also create empty NeoForge source roots and scoped ownership docs, but it must not add NeoForge gameplay code until Fabric behavior is stable in the new shape.

Use separate Gradle loader projects or equivalent loader-specific build scripts so Fabric Loom and NeoForge tooling do not contend in one project. The preferred target is:

```text
:fabric    -> packages common + commonClient + fabric + fabricClient
:neoforge  -> packages common + commonClient + neoforge + neoforgeClient
```

Root Gradle remains the orchestration surface for shared versioning, changelog gates, static scans, release-source checks, and CI aggregation.

## Boundaries

### Common Main

Common main owns loader-neutral behavior:

- Mod constants and logger identity.
- Variant catalog, stats, identity model, identity migration rules, golem family/surface/weathering models.
- Config schema, parsing, canonicalization, and default values.
- Spawn/creation rules and resolver logic.
- Ability behavior decisions and effect math, excluding loader event registration.
- Loot-condition decision logic, excluding loader registry wiring.
- Permission node names and default policy.
- Payload IDs, payload field contracts, size limits, codec semantics, and summaries.
- Embedded ModStatusKit helpers and MultiGolem status state machine.

Common main must not import Fabric, ModMenu, LuckPerms/Fabric Permissions API, or NeoForge classes.

### Common Client

Common client owns loader-neutral client behavior:

- Texture selection and render-state data model.
- ModStatus display state.
- Server customizations client state.
- Golempedia content and screen model where practical.

Common client must not register Fabric or NeoForge client events directly and must not import ModMenu. If existing UI screen classes depend heavily on Minecraft client classes but not loader APIs, they can remain common-client; ModMenu entrypoint wiring remains Fabric-client.

### Fabric Main Adapter

Fabric main owns:

- `ModInitializer`.
- Fabric event registration.
- Fabric attachment type registration and entity storage bridge.
- Fabric networking registration, send/can-send checks, and join sends.
- Fabric config path lookup.
- Fabric metadata/version lookup.
- Fabric Permissions API integration.
- Fabric loot table and creative tab event wiring.
- Fabric mixin JSON and Fabric-owned mixin classes.
- Fabric resources and generated Fabric build metadata packaging.

### Fabric Client Adapter

Fabric client owns:

- `ClientModInitializer`.
- Fabric client lifecycle, tick, join, disconnect, and payload receivers.
- ModMenu entrypoint and ModMenu-only integration.
- Fabric client mixin JSON and Fabric-owned client mixins.
- Any Fabric-only render hook wiring.

### NeoForge Main Adapter

NeoForge main will own:

- NeoForge mod entrypoint and event-bus registration.
- NeoForge attachment/data persistence and sync bridge.
- NeoForge networking registration, send/can-send checks, and join sends.
- NeoForge config path lookup.
- NeoForge metadata/version lookup.
- NeoForge built-in permission node registration and permission checks.
- NeoForge loot, creative tab, lifecycle, tick, entity load, damage, and interaction event wiring.
- NeoForge metadata and mixin declarations.

### NeoForge Client Adapter

NeoForge client will own:

- NeoForge client setup events.
- NeoForge client payload receivers.
- Client lifecycle/tick/join/disconnect wiring.
- NeoForge render-state/mixin/event wiring.
- No NeoForge in-game config UI in the first NeoForge phase unless Tyler explicitly adds that scope later.

## Fabric Touchpoint Inventory

Targeted scans found these current Fabric or Fabric-adjacent clusters:

- Entrypoints: `MultiGolem` imports `ModInitializer`; `MultiGolemClient` imports `ClientModInitializer`.
- Config/version metadata: `MultiGolem` and `MultiGolemStatus` call `FabricLoader`.
- Attachments: `GolemIdentityAttachment`, `GolemVariantAttachment`, `GolemSpawnOriginAttachment`, and `GolemAbilityStateAttachment` use Fabric attachment APIs.
- Server events: `MultiGolem` uses `ServerEntityEvents`, `CreativeModeTabEvents`, and `LootTableEvents`; ability classes use `ServerTickEvents` and `ServerLivingEntityEvents`.
- Networking: `MultiGolemStatusNetworking` and `ServerCustomizationsNetworking` use Fabric server payload APIs; `MultiGolemStatusClient` and `ServerCustomizationsClient` use Fabric client networking/lifecycle APIs.
- Permissions: `MultiGolemPermissions` uses `me.lucko.fabric.api.permissions.v0.Permissions`.
- ModMenu: `MultiGolemModMenu` uses Terraformers ModMenu APIs and Fabric environment annotations.
- Resources: `fabric.mod.json`, `multigolem.mixins.json`, and `multigolem.client.mixins.json` are Fabric-side declarations today.
- Client rendering: client mixins modify iron golem render state and texture selection.

## Attachment And State Strategy

Attachments are the highest-risk boundary because they are both storage and sync. Do not try to hide Fabric attachments by moving the current classes unchanged into common.

Create loader-neutral storage contracts in common:

- `GolemIdentityStore` for raw identity read/write/clear plus old variant migration fallback.
- `GolemVariantStore` for legacy variant read/write/clear during migration.
- `GolemSpawnOriginStore` for spawn-origin state.
- `GolemAbilityStateStore` for ability cooldown/state.

Common behavior should call a single `MultiGolemPlatform` or narrower state services that delegate to the active loader adapter. The active loader must register a `GolemStorageAdapter` before common initialization and event wiring, and common facades should throw a clear `IllegalStateException` if used before registration. Fabric implements the adapter with Fabric attachments. NeoForge implements it with NeoForge attachment/data APIs available for Minecraft `26.1.2`.

The current public helper names may remain as facades if that minimizes churn, but the facades must not import loader APIs after Phase 1.

## Event And Ability Strategy

The current ability classes mix behavior and Fabric event registration. Split them as follows:

- Common ability classes keep `onTick`, `allowDamage`, target selection, effect application, and cooldown math.
- Loader adapters own event registration and call common hooks.
- `AbilityRegistry` becomes either a loader-neutral list of common hook objects or a Fabric adapter registry that wires Fabric events to common hook methods.

The Fabric-preserving phase should avoid changing ability semantics. It should move registration ownership first, then prove the existing tests and playtest checklist still apply.

## Networking Strategy

Keep payload contracts shared:

- `multigolem:mod_status/server_version`
- `multigolem:server_customizations`

Common owns payload records, ID/path constants, codecs/field order, limits, and ModStatus/customization state transitions. Loader adapters own transport registration and send/can-send checks.

Fabric transport remains capability-gated with `ServerPlayNetworking.canSend` and Fabric client global receivers. NeoForge transport must use NeoForge payload registration and equivalent supported-payload checks so vanilla-compatible clients are not required to receive custom payloads.

Static and unit checks should prove channel names, payload field limits, ModStatus structured/legacy decoding, and capability-gated send behavior remain stable.

## Permissions Strategy

Permissions are a cross-loader modernization slice.

Current state:

- Permission nodes are permissive-by-default.
- `multigolem.admin.bypass` bypasses tier-specific create/heal denials.
- Tier nodes include `multigolem.create.<variant>` and `multigolem.heal.<variant>`.
- Current implementation compiles directly against `me.lucko.fabric.api.permissions.v0.Permissions`.

Target state:

- Common owns node names and default policy only.
- Fabric adapter checks Fabric API's current permission API and keeps LuckPerms as an external provider behind that API.
- NeoForge adapter registers permission nodes through NeoForge's built-in permission API and checks those registered nodes through the loader API.
- Common and common-client code must not compile against Fabric Permissions API or LuckPerms APIs.
- The Fabric adapter may compile against Fabric Permissions API as a loader-adapter-only dependency.
- No source root should compile directly against LuckPerms itself; LuckPerms remains a runtime server provider behind loader permission APIs.
- OP/game-master fallback must be explicit per loader.

Fallback policy:

- Tier create/heal nodes default to allowed when no provider denies them.
- Admin bypass defaults to false.
- Fabric fallback should match current gameplay: without a permissions provider, creation and healing continue to work.
- NeoForge fallback should match Fabric unless a reviewed NeoForge API limitation requires a documented difference.
- OP status must not silently bypass tier denials. Only `multigolem.admin.bypass` should bypass tier nodes unless Tyler explicitly chooses an OP bypass later.

## ModStatusKit Strategy

Keep embedded ModStatusKit classes in common. Move loader-specific version/build lookup out of `MultiGolemStatus`:

- Common keeps status labels, severity, timeout, state machine, payload parsing, and display values.
- Fabric adapter supplies version from Fabric metadata.
- NeoForge adapter supplies version from NeoForge metadata.
- Build metadata continues to come from generated `multigolem-build.properties` or system properties.

Both loader jars must include the build metadata resource and both status transports must preserve the current payload semantics.

## ModMenu And Config UI Strategy

ModMenu remains Fabric-client-only:

- `MultiGolemModMenu` and Terraformers imports move to `fabricClient`.
- Shared screen/state classes can stay in `commonClient` if they do not import Fabric/ModMenu APIs.
- NeoForge phase does not add an in-game config UI by default.

The server config path remains `config/multigolem.json` on both loaders. Client ModMenu screens continue to show status, server customizations, and Golempedia on Fabric.

## Mixin And Render Strategy

MultiGolem changes vanilla behavior primarily through mixins and event hooks. Treat mixins as loader-adapter owned unless the same mixin class is proven compatible and intentionally shared.

Fabric owns current mixin JSON declarations. Common may own helper methods used by mixins, but common must not own loader-specific mixin metadata. NeoForge should start with separate mixin JSON names if mixins are required, then reuse common helpers for identity, texture selection, spawn/conversion, and ability rules.

## Gradle And Artifact Strategy

Preferred build shape:

- Root project holds shared version, group, repositories that are safe globally, static checks, texture generation, changelog gates, release-source checks, and aggregation.
- `:fabric` applies Fabric Loom and packages Fabric loader roots.
- `:neoforge` applies the NeoForge Gradle plugin and packages NeoForge loader roots once implementation begins.

Artifact names must be loader-specific:

- `multigolem-${version}-fabric.jar`
- `multigolem-${version}-fabric-sources.jar`
- `multigolem-${version}-neoforge.jar`
- `multigolem-${version}-neoforge-sources.jar`

Do not produce a universal jar.

The texture generator must continue to write canonical asset outputs, and the plan must decide whether generated textures live in common resources or are copied into loader-specific jars by Gradle.

## Static Scans

Add or extend Gradle checks for:

```text
src/common and src/commonClient must not import:
net.fabricmc
net.neoforged
com.terraformersmc.modmenu
me.lucko.fabric
```

Add release-source scans for:

- Loader-suffixed primary jar paths.
- Loader-suffixed sources jar paths.
- GitHub Release asset names that cannot collide.
- Modrinth dependency strategy documented in scripts/tests.
- CurseForge relation metadata shape, including no empty `relations.projects`.
- Marketplace description source stays separate from internal changelog source.

## Publishing Strategy

No publish, tag, upload, or release action is part of this planning phase.

When implementation reaches release prep, update these current single-loader assumptions:

- `.github/workflows/build.yml` artifact capture.
- `.github/workflows/release.yml` release artifact verification and `gh release create` assets.
- `scripts/upload-modrinth.ps1` single jar/sources path, `loaders = @("fabric")`, and required Fabric API dependency.
- `scripts/upload-curseforge.ps1` single file upload, Fabric game version, and required Fabric API relation.
- Marketplace docs if NeoForge support changes public install guidance.

Modrinth decision:

- Recommended default: separate Modrinth versions per loader, because dependencies are version-wide and Fabric API should not be globally required for a NeoForge file.
- Acceptable alternative: one combined Modrinth version with both loader files only if Fabric-only dependencies are optional or otherwise documented so NeoForge installs are not polluted by Fabric API requirements.

CurseForge decision:

- Upload per loader.
- Fabric upload includes Fabric loader metadata and Fabric API dependency relation.
- NeoForge upload includes NeoForge loader metadata and omits Fabric API relation.
- Omit `relations` entirely when a loader has no dependency relation instead of sending an empty `relations.projects` list.
- Retry/reporting must be per loader so a retried NeoForge upload does not claim a Fabric file ID or vice versa.

Changelog decision:

- `INTERNAL_CHANGELOG.md` records planning, structural baselines, and implementation milestones.
- `CHANGELOG.md` only records verified player/server-admin behavior once Tyler approves release prep wording.
- Marketplace descriptions remain in `docs/modrinth-listing.md` and `docs/curseforge-listing.md`; changelog sections remain release notes, not long descriptions.

## Verification Matrix

Planning verification:

- `git rev-parse --show-toplevel`
- `git status --short --branch`
- `rg` inventories for Fabric imports, permissions, networking, ModStatus, ModMenu, mixins, Gradle, and release scripts.
- Revue design/spec and implementation-plan review with explicit files.

Fabric-preserving implementation verification:

- `.\gradlew.bat test`
- `.\gradlew.bat build`
- `.\gradlew.bat checkChangelog`
- `.\gradlew.bat checkReleaseDocs`
- `.\gradlew.bat checkCommonSourceSetsLoaderNeutral`
- Fabric jar inspection for `fabric.mod.json`, mixin JSON, icon, generated build metadata, entrypoint classes, and ModMenu entrypoint.
- Common import scan for Fabric, NeoForge, ModMenu, and LuckPerms/Fabric Permissions API leaks.

NeoForge implementation verification:

- `.\gradlew.bat :neoforge:test`
- `.\gradlew.bat :neoforge:build`
- NeoForge jar inspection for `META-INF/neoforge.mods.toml`, mixin declarations if present, icon, generated build metadata, and NeoForge entrypoint classes.
- Permission default and provider override tests where loader APIs permit automation.

Manual verification:

- Existing Fabric playtest checklist remains the baseline.
- Add NeoForge rows before release prep for creation, healing, village spawning, marked eggs, zombie golem behavior, permissions, status payloads, customizations payloads, client textures, vanilla-client fallback, and save/load behavior.

## Scope Exclusions

This phase does not:

- Move production code yet.
- Add NeoForge gameplay adapter code yet.
- Publish to Modrinth or CurseForge.
- Create tags, GitHub Releases, or deployments.
- Touch Minecraft-Docker.
- Add broad workflow rules to `AGENTS.md`.
- Extract a reusable library.
- Build a universal jar.

## Open Risks

- NeoForge API exact names and Gradle plugin version should be refreshed at implementation time against the target Minecraft `26.1.2` ecosystem.
- Fabric attachment replacement is the largest code-movement risk and should be planned before event wiring.
- Current tests include several source-text safety checks tied to `src/main/java`; they will need path-aware updates during physical layout migration.
- Release automation has no current multi-file retry model.
- Cross-loader custom payload compatibility is not a release requirement unless Tyler explicitly adds it. Same-loader custom behavior plus vanilla-compatible fallback is the target.
