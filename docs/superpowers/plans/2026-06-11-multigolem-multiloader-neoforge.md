# MultiGolem Multiloader / NeoForge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reshape MultiGolem into a Fabric-preserving multiloader repository, then add a separate NeoForge adapter/build without changing the existing Fabric gameplay contract.

**Architecture:** Use a shared common/common-client code boundary with loader-owned Fabric and NeoForge adapters. Keep loader APIs, permission-provider calls, metadata lookup, event registration, networking transport, ModMenu, mixin declarations, and publishing paths out of common code.

**Tech Stack:** Java 25, Gradle, Fabric Loom, Fabric API, Fabric Permissions API, ModMenu, embedded ModStatusKit, NeoForge for Minecraft `26.1.2`, JUnit 6, Mockito, PowerShell release scripts, GitHub Actions.

---

## Preflight

**Files:**

- Read: `docs/superpowers/specs/2026-06-11-multigolem-multiloader-neoforge-design.md`
- Read: `build.gradle`
- Read: `settings.gradle`
- Read: `gradle.properties`
- Read: `.github/workflows/release.yml`
- Read: `scripts/upload-modrinth.ps1`
- Read: `scripts/upload-curseforge.ps1`

- [ ] **Step 1: Verify the repository root**

Run:

```powershell
git rev-parse --show-toplevel
```

Expected: `C:/Users/tyler/AI Projects/MultiGolem`.

- [ ] **Step 2: Inspect branch and local changes**

Run:

```powershell
git status --short --branch
git branch --show-current
```

Expected: a known branch and no unrelated dirty files. Preserve Tyler-authored changes.

- [ ] **Step 3: Run the current Fabric baseline**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat check
```

Expected: all pass before the migration begins. If any fail, stop and document the failure before changing layout.

## File Map

Target ownership after Phase 1:

```text
src/common/java/dev/charles/multigolem/
  ability/                common ability behavior, no event registration
  attachment/             loader-neutral state facades and storage contracts
  attribute/              variant stats application
  catalog/                variant catalog
  config/                 config model and parser
  customizations/         payload and snapshot semantics
  golempedia/             shared Golempedia model/content
  identity/               identity model and migration
  internal/modstatus/     embedded ModStatusKit
  permissions/            node names, defaults, messages
  spawn/                  spawn and resolver behavior
  status/                 status state and payload semantics

src/commonClient/java/dev/charles/multigolem/client/
  customizations/         client state
  modmenu/                shared screens only if no ModMenu imports remain
  render/                 texture selection and render-state helpers
  status/                 status UI state helpers

src/fabric/java/dev/charles/multigolem/fabric/
  MultiGolemFabric.java
  attachment/
  event/
  network/
  permissions/
  status/

src/fabricClient/java/dev/charles/multigolem/fabric/client/
  MultiGolemFabricClient.java
  customizations/
  modmenu/
  network/
  status/

src/neoforge/java/dev/charles/multigolem/neoforge/
src/neoforgeClient/java/dev/charles/multigolem/neoforge/client/
```

## Task 1: Add Boundary Documentation And Static Discovery Gates

**Files:**

- Create: `docs/multiloader/fabric-touchpoints.md`
- Create: `docs/multiloader/boundary-rules.md`
- Create: `docs/multiloader/verification-matrix.md`
- Modify: `build.gradle`
- Modify: `INTERNAL_CHANGELOG.md`

- [ ] **Step 1: Create Fabric touchpoint inventory**

Write `docs/multiloader/fabric-touchpoints.md` with these sections and concrete class lists:

```markdown
# Fabric Touchpoints

## Entrypoints

- `dev.charles.multigolem.MultiGolem` implements Fabric `ModInitializer`.
- `dev.charles.multigolem.client.MultiGolemClient` implements Fabric `ClientModInitializer`.

## Loader Metadata And Config

- `MultiGolem` uses `FabricLoader.getConfigDir()`.
- `MultiGolemStatus` uses Fabric metadata for current version lookup.

## Attachments

- `GolemIdentityAttachment`
- `GolemVariantAttachment`
- `GolemSpawnOriginAttachment`
- `GolemAbilityStateAttachment`

## Events

- `ServerEntityEvents`
- `CreativeModeTabEvents`
- `LootTableEvents`
- `ServerTickEvents`
- `ServerLivingEntityEvents`

## Networking

- `MultiGolemStatusNetworking`
- `ServerCustomizationsNetworking`
- `MultiGolemStatusClient`
- `ServerCustomizationsClient`

## Permissions

- `MultiGolemPermissions` currently imports `me.lucko.fabric.api.permissions.v0.Permissions`.

## ModMenu

- `MultiGolemModMenu`
- shared ModMenu screens under `src/client/java/dev/charles/multigolem/client/modmenu`

## Mixins And Resources

- `fabric.mod.json`
- `multigolem.mixins.json`
- `multigolem.client.mixins.json`
```

- [ ] **Step 2: Create boundary rules**

Write `docs/multiloader/boundary-rules.md`:

```markdown
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
```

- [ ] **Step 3: Create verification matrix**

Write `docs/multiloader/verification-matrix.md` with Fabric and NeoForge sections covering:

```text
repository root
unit tests
full build
common import scan
jar metadata inspection
permissions default/provider override checks
ModStatus payload checks
server customizations payload checks
vanilla-client compatibility
manual playtest checklist
release-source checks
```

- [ ] **Step 4: Add static common import scan**

Add a root Gradle task:

```gradle
tasks.register("checkCommonSourceSetsLoaderNeutral") {
    group = "verification"
    description = "Prevents loader APIs from leaking into common multiloader source roots."

    doLast {
        def roots = ["src/common", "src/commonClient"].collect { file(it) }.findAll { it.exists() }
        if (roots.isEmpty()) {
            logger.lifecycle("No multiloader common roots yet; loader-neutral scan skipped.")
            return
        }
        def forbidden = ~/(?m)^\s*import\s+(net\.fabricmc|net\.neoforged|com\.terraformersmc\.modmenu|me\.lucko\.fabric)\./
        def offenders = []
        roots.each { root ->
            fileTree(root).matching { include "**/*.java" }.files.each { source ->
                def text = source.getText("UTF-8")
                if ((text =~ forbidden).find()) {
                    offenders.add(projectDir.toPath().relativize(source.toPath()).toString().replace(File.separatorChar, '/' as char))
                }
            }
        }
        if (!offenders.isEmpty()) {
            throw new GradleException("Common source roots contain loader-specific imports:\n${offenders.join("\n")}")
        }
        logger.lifecycle("Common source-set loader-neutral scan passed.")
    }
}
```

- [ ] **Step 5: Leave the scan available but not yet wired into root `check`**

Do not add `checkCommonSourceSetsLoaderNeutral` to root `check` in this task. The migration intentionally creates the `src/common` roots before all Fabric imports have been extracted, so wiring the gate too early would make intermediate tasks fail for known transitional state.

Keep the task runnable directly:

```powershell
.\gradlew.bat checkCommonSourceSetsLoaderNeutral
```

Expected before Task 7 is complete: either skip because common roots are absent, or fail with known Fabric imports that are being removed by Tasks 3 through 7.

The root `check` wiring happens after common and common-client are loader-neutral in Task 7.

- [ ] **Step 6: Record internal changelog**

Add under `INTERNAL_CHANGELOG.md` `## Unreleased`:

```markdown
- Added multiloader planning documents and a loader-neutral common source scan for the upcoming Fabric-preserving NeoForge port.
```

- [ ] **Step 7: Verify**

Run:

```powershell
.\gradlew.bat checkCommonSourceSetsLoaderNeutral
.\gradlew.bat check
git diff --check
```

Expected: `check` and whitespace pass. The direct common scan may skip or fail with known transitional imports until Task 7; record that output instead of treating it as a final gate.

## Task 2: Reshape Source Roots Without Behavior Changes

**Files:**

- Create: `src/common/AGENTS.md`
- Create: `src/commonClient/AGENTS.md`
- Create: `src/fabric/AGENTS.md`
- Create: `src/fabricClient/AGENTS.md`
- Move: `src/main/java/dev/charles/multigolem/**` to `src/common/java/dev/charles/multigolem/**`
- Move: `src/client/java/dev/charles/multigolem/client/**` to `src/commonClient/java/dev/charles/multigolem/client/**`
- Move: `src/main/resources/assets/multigolem/**` to `src/common/resources/assets/multigolem/**`
- Move: `src/main/resources/fabric.mod.json` to `src/fabric/resources/fabric.mod.json`
- Move: `src/main/resources/multigolem.mixins.json` to `src/fabric/resources/multigolem.mixins.json`
- Move: `src/client/resources/multigolem.client.mixins.json` to `src/fabricClient/resources/multigolem.client.mixins.json`
- Modify: `build.gradle`

- [ ] **Step 1: Verify root before moves**

Run:

```powershell
git rev-parse --show-toplevel
```

Expected: `C:/Users/tyler/AI Projects/MultiGolem`.

- [ ] **Step 2: Add scoped ownership docs**

Create `src/common/AGENTS.md`:

```markdown
# Agent Notes for Common Source

This source root owns loader-neutral MultiGolem behavior.

- Do not import Fabric, NeoForge, ModMenu, Fabric Permissions API, or LuckPerms provider APIs here.
- Keep permission provider calls, loader event registration, networking transport, config paths, metadata lookup, and mixin declarations in loader adapters.
- Common may own permission node names and default policy.
```

Create equivalent concise files for `commonClient`, `fabric`, and `fabricClient` that state their ownership.

- [ ] **Step 3: Move files with `git mv`**

Run move commands from the repo root, preserving package names for this task:

```powershell
New-Item -ItemType Directory -Force -Path src\common\java,src\commonClient\java,src\common\resources,src\fabric\resources,src\fabricClient\resources | Out-Null
git mv src\main\java\dev src\common\java\dev
git mv src\client\java\dev src\commonClient\java\dev
git mv src\main\resources\assets src\common\resources\assets
git mv src\main\resources\fabric.mod.json src\fabric\resources\fabric.mod.json
git mv src\main\resources\multigolem.mixins.json src\fabric\resources\multigolem.mixins.json
git mv src\client\resources\multigolem.client.mixins.json src\fabricClient\resources\multigolem.client.mixins.json
```

- [ ] **Step 4: Point Loom at the new roots**

Configure current root source sets while still building one Fabric jar:

```gradle
sourceSets {
    main {
        java.srcDirs = ["src/common/java", "src/fabric/java"]
        resources.srcDirs = ["src/common/resources", "src/fabric/resources", generatedBuildInfoDir]
    }
    client {
        java.srcDirs = ["src/commonClient/java", "src/fabricClient/java"]
        resources.srcDirs = ["src/commonClient/resources", "src/fabricClient/resources"]
    }
}
```

Keep `loom { splitEnvironmentSourceSets() }` and the `mods { "multigolem" { ... } }` block.

- [ ] **Step 5: Update path-sensitive tests**

Run:

```powershell
rg -n "src/main/java|src/client/java|src/main/resources|src/client/resources" src\test scripts docs
```

For source-text tests, update paths to the moved source roots. For release docs scripts, keep Fabric metadata path changes deliberate and covered by Task 6.

- [ ] **Step 6: Verify Fabric still builds**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat checkCommonSourceSetsLoaderNeutral
```

Expected: the build may fail the common scan because Fabric imports still live in common. If so, record the scan failure as expected for this intermediate task and continue immediately to Task 3. `test` and `build` must pass.

## Task 3: Extract Fabric Main Adapter

**Files:**

- Create: `src/fabric/java/dev/charles/multigolem/fabric/MultiGolemFabric.java`
- Create: `src/fabric/java/dev/charles/multigolem/fabric/event/FabricMultiGolemEvents.java`
- Create: `src/fabric/java/dev/charles/multigolem/fabric/attachment/FabricGolemAttachments.java`
- Modify: `src/common/java/dev/charles/multigolem/MultiGolem.java`
- Modify: attachment facades under `src/common/java/dev/charles/multigolem/attachment/`
- Modify: `src/fabric/resources/fabric.mod.json`

- [ ] **Step 1: Make `MultiGolem` loader-neutral**

Remove `implements ModInitializer`, Fabric imports, and direct `FabricLoader` path lookup from `MultiGolem`. Add:

```java
public static void initialize(Path configFile) {
    CONFIG = MultiGolemConfig.loadOrCreate(configFile);
    LOG.info("MultiGolem starting up - config loaded from {}", configFile);
}
```

Keep constants, config access, loot-drop helper behavior, and creative spawn egg variant helper in common.

- [ ] **Step 2: Create Fabric entrypoint**

Create `MultiGolemFabric`:

```java
package dev.charles.multigolem.fabric;

import dev.charles.multigolem.MultiGolem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MultiGolemFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricGolemAttachments.register();
        MultiGolem.initialize(FabricLoader.getInstance().getConfigDir().resolve(MultiGolem.MOD_ID + ".json"));
        FabricMultiGolemEvents.register();
    }
}
```

- [ ] **Step 3: Move Fabric event registration**

Move `ServerEntityEvents`, `CreativeModeTabEvents`, `LootTableEvents`, and registry wiring into `FabricMultiGolemEvents`. Delegate common decisions back to common helpers.

- [ ] **Step 4: Define and register the common storage adapter**

Create a common `GolemStorageAdapter` interface that covers identity, legacy variant, spawn-origin, and ability-state storage operations. Add a single common registry/facade such as:

```java
public final class GolemStorage {
    private static volatile GolemStorageAdapter adapter;

    private GolemStorage() {}

    public static void register(GolemStorageAdapter storageAdapter) {
        adapter = Objects.requireNonNull(storageAdapter, "storageAdapter");
    }

    static GolemStorageAdapter adapter() {
        GolemStorageAdapter current = adapter;
        if (current == null) {
            throw new IllegalStateException("MultiGolem storage adapter has not been registered by the active loader");
        }
        return current;
    }
}
```

Fabric must call `GolemStorage.register(new FabricGolemStorageAdapter())` from `MultiGolemFabric.onInitialize()` before `MultiGolem.initialize(...)`, `FabricGolemAttachments.register()`, and `FabricMultiGolemEvents.register()`. Tests should assert that calling a storage facade before registration fails with the explicit `IllegalStateException`.

- [ ] **Step 5: Move attachment registration behind loader-owned bridge**

Fabric attachment classes may still use Fabric APIs, but they must live under `src/fabric/java`. Common facades should depend on a registered storage accessor instead of importing `AttachmentRegistry`.

- [ ] **Step 6: Update Fabric metadata**

Set Fabric main entrypoint:

```json
"main": [
  "dev.charles.multigolem.fabric.MultiGolemFabric"
]
```

- [ ] **Step 7: Verify**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
rg -n "^\s*import\s+net\.fabricmc|^\s*import\s+me\.lucko\.fabric|^\s*import\s+com\.terraformersmc" src\common
```

Expected: tests and build pass; remaining common Fabric imports are limited to slices not yet migrated and listed for the next tasks.

## Task 4: Extract Fabric Ability/Event Hooks

**Files:**

- Create: `src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java`
- Modify: `src/common/java/dev/charles/multigolem/ability/AbilityRegistry.java`
- Modify: `src/common/java/dev/charles/multigolem/ability/CopperAbility.java`
- Modify: `src/common/java/dev/charles/multigolem/ability/DiamondAbility.java`
- Modify: `src/common/java/dev/charles/multigolem/ability/EmeraldAbility.java`
- Modify: `src/common/java/dev/charles/multigolem/ability/GoldAbility.java`
- Modify: `src/common/java/dev/charles/multigolem/ability/NetheriteAbility.java`

- [ ] **Step 1: Remove Fabric event registration from common ability classes**

Keep common methods such as:

```java
public static void onTick(ServerLevel level) { ... }
public static boolean allowDamage(Entity entity, DamageSource source, float amount) { ... }
```

Remove `ServerTickEvents` and `ServerLivingEntityEvents` imports from common ability classes.

- [ ] **Step 2: Register Fabric hooks in adapter**

In `FabricAbilityEvents.register()`, wire:

```java
ServerTickEvents.START_LEVEL_TICK.register(GoldAbility::onTick);
ServerTickEvents.START_LEVEL_TICK.register(EmeraldAbility::onTick);
ServerTickEvents.START_LEVEL_TICK.register(DiamondAbility::onTick);
ServerLivingEntityEvents.ALLOW_DAMAGE.register(CopperAbility::allowDamage);
ServerLivingEntityEvents.ALLOW_DAMAGE.register(NetheriteAbility::allowDamage);
ServerLivingEntityEvents.ALLOW_DAMAGE.register(DiamondAbility::allowDamage);
```

- [ ] **Step 3: Call `FabricAbilityEvents.register()` from `FabricMultiGolemEvents.register()`**

Keep registration idempotent if current common classes were idempotent.

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat test --tests dev.charles.multigolem.ability.*
.\gradlew.bat build
rg -n "ServerTickEvents|ServerLivingEntityEvents|net\.fabricmc" src\common\java\dev\charles\multigolem\ability
```

Expected: tests and build pass; no Fabric event imports remain in common ability code.

## Task 5: Modernize Permissions Behind Loader Adapters

**Files:**

- Modify: `src/common/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java`
- Create: `src/common/java/dev/charles/multigolem/permissions/MultiGolemPermissionNodes.java`
- Create: `src/fabric/java/dev/charles/multigolem/fabric/permissions/FabricMultiGolemPermissions.java`
- Modify tests under `src/test/java/dev/charles/multigolem/permissions/`
- Modify: `gradle.properties`
- Modify: `build.gradle`

- [ ] **Step 1: Move node names/defaults to common**

Create `MultiGolemPermissionNodes` with:

```java
public static final String ADMIN_BYPASS = "multigolem.admin.bypass";
public static String create(GolemVariant variant) { ... }
public static String heal(GolemVariant variant) { ... }
public static boolean defaultAllowed(String node) {
    return !ADMIN_BYPASS.equals(node);
}
```

- [ ] **Step 2: Make common permission checks injectable**

`MultiGolemPermissions` should expose behavior in terms of:

```java
@FunctionalInterface
public interface PermissionLookup {
    boolean check(ServerPlayer player, String node, boolean defaultValue);
}
```

Common retains denial messages and client-side prediction behavior.

- [ ] **Step 3: Implement Fabric permission adapter**

Use Fabric API's current permission API in `FabricMultiGolemPermissions`. Do not compile against LuckPerms directly.

Expected dependency direction: Fabric Permissions API or Fabric API permission integration is a Fabric adapter dependency only. LuckPerms remains an external provider installed on the server.

- [ ] **Step 4: Document fallback**

Add docs in `docs/multiloader/boundary-rules.md`:

```markdown
Fabric fallback: tier create/heal nodes default to allowed, admin bypass defaults to false, and OP is not an implicit provider-granted bypass.
NeoForge fallback: match Fabric defaults through registered NeoForge permission nodes unless implementation-time API evidence requires a documented exception.
```

- [ ] **Step 5: Verify**

Run:

```powershell
.\gradlew.bat test --tests dev.charles.multigolem.permissions.*
.\gradlew.bat build
rg -n "me\.lucko|Permissions\.check" src\common src\commonClient
```

Expected: tests and build pass; no LuckPerms/Fabric Permissions API imports remain in common.

## Task 6: Extract Networking And ModStatus Loader Boundaries

**Files:**

- Move/modify: `MultiGolemStatusNetworking` into `src/fabric/java/dev/charles/multigolem/fabric/status/`
- Move/modify: `ServerCustomizationsNetworking` into `src/fabric/java/dev/charles/multigolem/fabric/customizations/`
- Move/modify: `MultiGolemStatusClient` into `src/fabricClient/java/dev/charles/multigolem/fabric/client/status/`
- Move/modify: `ServerCustomizationsClient` into `src/fabricClient/java/dev/charles/multigolem/fabric/client/customizations/`
- Modify: `src/common/java/dev/charles/multigolem/status/MultiGolemStatus.java`
- Modify: `src/common/java/dev/charles/multigolem/status/MultiGolemStatusPayload.java`
- Modify: `src/common/java/dev/charles/multigolem/customizations/ServerCustomizationsPayload.java`

- [ ] **Step 1: Keep payload records in common**

Payload records stay common if they only import Minecraft protocol classes and not loader transport APIs.

- [ ] **Step 2: Move Fabric transport classes**

Move transport registration and send/can-send checks into Fabric adapter packages.

- [ ] **Step 3: Inject version lookup**

Replace `FabricLoader` use in `MultiGolemStatus` with common initialization:

```java
public static void initializeVersion(String version, String build) { ... }
```

Fabric adapter supplies version from Fabric metadata.

- [ ] **Step 4: Update Fabric entrypoints**

`MultiGolemFabric` registers server status/customizations transports. `MultiGolemFabricClient` registers client receivers and lifecycle events.

- [ ] **Step 5: Verify**

Run:

```powershell
.\gradlew.bat test --tests dev.charles.multigolem.status.* --tests dev.charles.multigolem.customizations.*
.\gradlew.bat build
rg -n "FabricLoader|ServerPlayNetworking|ClientPlayNetworking|PayloadTypeRegistry|ClientPlayConnectionEvents|ServerPlayConnectionEvents|net\.fabricmc" src\common src\commonClient
```

Expected: tests and build pass; no Fabric transport or metadata imports remain in common roots.

## Task 7: Extract Fabric Client, ModMenu, And Resource Ownership

**Files:**

- Create: `src/fabricClient/java/dev/charles/multigolem/fabric/client/MultiGolemFabricClient.java`
- Move: `MultiGolemModMenu` to `src/fabricClient/java/dev/charles/multigolem/fabric/client/modmenu/`
- Modify: shared screen classes only if package/import updates are required
- Modify: `src/fabric/resources/fabric.mod.json`
- Modify: `scripts/check-release-docs.py`
- Modify tests under `src/test/java/dev/charles/multigolem/status/`

- [ ] **Step 1: Create Fabric client entrypoint**

Create:

```java
package dev.charles.multigolem.fabric.client;

import dev.charles.multigolem.MultiGolem;
import net.fabricmc.api.ClientModInitializer;

public final class MultiGolemFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MultiGolem.LOG.info("MultiGolem Fabric client initializing");
        FabricMultiGolemStatusClient.register();
        FabricServerCustomizationsClient.register();
    }
}
```

- [ ] **Step 2: Keep ModMenu Fabric-only**

Move the ModMenu API implementation into `fabricClient`. Shared screens may stay common-client.

- [ ] **Step 3: Update metadata paths in release docs gate**

`scripts/check-release-docs.py` should read:

```text
src/fabric/resources/fabric.mod.json
src/common/resources/assets/multigolem/icon.png
src/common/resources/assets/multigolem/lang/en_us.json
```

- [ ] **Step 4: Verify**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat checkReleaseDocs
rg -n "com\.terraformersmc|ModMenuApi|net\.fabricmc" src\commonClient
```

Expected: tests/build/docs gate pass; no Fabric or ModMenu imports remain in common-client.

- [ ] **Step 5: Wire the common import scan into root `check`**

After Tasks 3 through 7 remove Fabric, ModMenu, Fabric Permissions API, and NeoForge imports from `src/common` and `src/commonClient`, update root `check`:

```gradle
tasks.named("check") {
    dependsOn(
        "checkChangelog",
        "checkWrapperChecksum",
        "checkReleaseNotesStyle",
        "checkV4PlanningHandoff",
        "checkReleaseDocs",
        "checkUnitTestCompanions",
        "checkCommonSourceSetsLoaderNeutral"
    )
}
```

Run:

```powershell
.\gradlew.bat checkCommonSourceSetsLoaderNeutral
.\gradlew.bat check
```

Expected: both pass. From this point on, a common-source loader import is a real failure, not transitional state.

## Task 8: Add Loader-Specific Gradle Projects And Artifact Names

**Files:**

- Modify: `settings.gradle`
- Modify: `build.gradle`
- Create: `fabric/build.gradle`
- Create: `neoforge/build.gradle`
- Modify: `.github/workflows/build.yml`

- [ ] **Step 1: Include loader projects**

Update `settings.gradle`:

```gradle
pluginManagement {
    repositories {
        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
        maven { name = 'NeoForge'; url = 'https://maven.neoforged.net/releases' }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = 'multigolem'
include("fabric")
include("neoforge")
```

- [ ] **Step 2: Move Fabric Loom build ownership to `:fabric`**

Apply Fabric Loom in `fabric/build.gradle`, using source dirs:

```gradle
sourceSets {
    main {
        java.srcDirs = ["../src/common/java", "../src/fabric/java"]
        resources.srcDirs = ["../src/common/resources", "../src/fabric/resources"]
    }
    client {
        java.srcDirs = ["../src/commonClient/java", "../src/fabricClient/java"]
        resources.srcDirs = ["../src/commonClient/resources", "../src/fabricClient/resources"]
    }
    test {
        java.srcDirs = ["../src/test/java"]
    }
}
```

- [ ] **Step 3: Configure Fabric archive names**

Set Fabric outputs:

```gradle
base.archivesName = "multigolem-${project.version}-fabric"
```

Ensure sources output becomes `multigolem-${version}-fabric-sources.jar`.

- [ ] **Step 4: Add empty NeoForge project shell**

Create `neoforge/build.gradle` with Java toolchain and source dirs only. Do not add gameplay code yet.

- [ ] **Step 5: Keep root orchestration**

Root `check` should depend on `:fabric:test`, `:fabric:build`, changelog gates, release docs gates, and static scans. Do not require `:neoforge:build` until NeoForge dependencies and empty entrypoints exist.

- [ ] **Step 6: Verify Fabric project**

Run:

```powershell
.\gradlew.bat :fabric:test
.\gradlew.bat :fabric:build
.\gradlew.bat check
```

Expected: pass and produce loader-suffixed Fabric jar/sources artifacts.

## Task 9: Add Release-Source Checks For Loader Artifacts

**Files:**

- Create: `scripts/check-multiloader-release-sources.py`
- Create: `scripts/test-check-multiloader-release-sources.py`
- Modify: `build.gradle`
- Modify: `.github/workflows/release.yml`
- Modify: `scripts/upload-modrinth.ps1`
- Modify: `scripts/upload-curseforge.ps1`

- [ ] **Step 1: Add release-source checker**

The checker must assert:

```text
release workflow verifies fabric and neoforge jar names
release workflow verifies fabric and neoforge sources jar names
GitHub Release upload lists unique loader-suffixed sources artifacts
Modrinth script accepts loader arguments or explicit file maps
CurseForge script accepts loader arguments or explicit file maps
CurseForge script omits relations when no relation projects exist
```

- [ ] **Step 2: Add tests for the checker**

Tests must include:

```text
fails on unsuffixed multigolem-$version.jar-only workflow
fails on duplicate sources jar names
fails on empty CurseForge relations.projects
passes on separate fabric/neoforge upload metadata
```

- [ ] **Step 3: Wire Gradle gate**

Add:

```gradle
tasks.register("checkMultiloaderReleaseSources") {
    group = "verification"
    description = "Checks release scripts and workflows for loader-specific artifact handling."
    doLast {
        def python = providers.environmentVariable("PYTHON").getOrElse("python")
        exec { commandLine(python, "scripts/check-multiloader-release-sources.py") }
    }
}
```

Add it to root `check`.

- [ ] **Step 4: Verify**

Run:

```powershell
python -m unittest scripts/test-check-multiloader-release-sources.py
.\gradlew.bat checkMultiloaderReleaseSources
.\gradlew.bat check
```

Expected: all pass.

## Task 10: Add NeoForge Skeleton

**Files:**

- Modify: `gradle.properties`
- Modify: `neoforge/build.gradle`
- Create: `src/neoforge/AGENTS.md`
- Create: `src/neoforgeClient/AGENTS.md`
- Create: `src/neoforge/resources/META-INF/neoforge.mods.toml`
- Create: `src/neoforge/java/dev/charles/multigolem/neoforge/MultiGolemNeoForge.java`
- Create: `src/neoforgeClient/java/dev/charles/multigolem/neoforge/client/MultiGolemNeoForgeClient.java`

- [ ] **Step 1: Refresh NeoForge versions**

Before editing, verify current NeoForge Gradle/plugin coordinates for Minecraft `26.1.2`. Record the chosen version in `gradle.properties`.

- [ ] **Step 2: Add NeoForge metadata and empty entrypoints**

Create loader metadata, entrypoint classes, and no gameplay registrations except basic startup/version initialization.

- [ ] **Step 3: Configure NeoForge artifact names**

NeoForge outputs must be:

```text
multigolem-${version}-neoforge.jar
multigolem-${version}-neoforge-sources.jar
```

- [ ] **Step 4: Verify empty NeoForge build**

Run:

```powershell
.\gradlew.bat :neoforge:build
.\gradlew.bat :fabric:build
```

Expected: both builds pass.

## Task 11: Implement NeoForge Storage, Events, Permissions, Networking, And Client Hooks

**Files:**

- Create/modify: `src/neoforge/java/dev/charles/multigolem/neoforge/attachment/**`
- Create/modify: `src/neoforge/java/dev/charles/multigolem/neoforge/event/**`
- Create/modify: `src/neoforge/java/dev/charles/multigolem/neoforge/permissions/**`
- Create/modify: `src/neoforge/java/dev/charles/multigolem/neoforge/network/**`
- Create/modify: `src/neoforgeClient/java/dev/charles/multigolem/neoforge/client/**`
- Create/modify: NeoForge mixin resources if required

- [ ] **Step 1: Implement NeoForge attachment/data storage**

Map identity, old variant, spawn origin, and ability state to NeoForge persistence/sync APIs.

- [ ] **Step 2: Implement NeoForge lifecycle and gameplay events**

Wire equivalent behavior for entity load, tick abilities, damage gates, loot, creative tab items, spawn/creation hooks, healing hooks, zombie conversion, and village maintenance.

- [ ] **Step 3: Implement NeoForge permissions**

Register nodes and check through NeoForge permission APIs:

```text
multigolem.admin.bypass -> false
multigolem.create.<variant> -> true
multigolem.heal.<variant> -> true
```

Do not treat OP as implicit bypass unless Tyler changes the design.

- [ ] **Step 4: Implement NeoForge networking**

Register and send:

```text
multigolem:mod_status/server_version
multigolem:server_customizations
```

Preserve capability-gated sends and vanilla-client compatibility.

- [ ] **Step 5: Implement NeoForge client hooks**

Wire status/customizations client receivers, tick/join/disconnect cleanup, render-state identity transfer, texture selection, and client mixins/events.

- [ ] **Step 6: Verify**

Run:

```powershell
.\gradlew.bat :neoforge:test
.\gradlew.bat :neoforge:build
.\gradlew.bat :fabric:test
.\gradlew.bat :fabric:build
.\gradlew.bat checkCommonSourceSetsLoaderNeutral
```

Expected: all pass.

## Task 12: Manual Verification And Closeout

**Files:**

- Modify: `docs/playtest-checklist.md`
- Modify: `INTERNAL_CHANGELOG.md`
- Modify: `CHANGELOG.md` only after Tyler approves public release-note wording

- [ ] **Step 1: Add NeoForge manual verification rows**

Extend the checklist with:

```text
NeoForge singleplayer
NeoForge dedicated server with NeoForge client
NeoForge server with vanilla client fallback
NeoForge permissions defaults and provider overrides
NeoForge ModStatus and server customizations payloads
NeoForge save/load and mod removal behavior
NeoForge marked spawn eggs and zombie golem behavior
```

- [ ] **Step 2: Run Fabric manual regression**

Record jar path, Minecraft version, loader version, Fabric API version, server/client shape, and pass/fail notes.

- [ ] **Step 3: Run NeoForge manual verification**

Record jar path, Minecraft version, NeoForge version, server/client shape, and pass/fail notes.

- [ ] **Step 4: Run final automated verification**

Run:

```powershell
git rev-parse --show-toplevel
.\gradlew.bat :fabric:test
.\gradlew.bat :fabric:build
.\gradlew.bat :neoforge:test
.\gradlew.bat :neoforge:build
.\gradlew.bat check
git diff --check
```

Expected: all pass.

- [ ] **Step 5: Request implementation review**

Use Revue `implementation-review` with explicit files and commands run. Resolve or disposition findings before calling implementation complete.

## Stop Rules

- Stop after the reviewed spec and plan until Tyler explicitly approves implementation.
- Stop if repo root is not `C:/Users/tyler/AI Projects/MultiGolem`.
- Stop if Fabric baseline tests fail before migration.
- Stop if common code still imports Fabric, NeoForge, ModMenu, or LuckPerms after the boundary tasks.
- Do not push, tag, release, publish, deploy, upload, or touch Minecraft-Docker without explicit Tyler approval.
