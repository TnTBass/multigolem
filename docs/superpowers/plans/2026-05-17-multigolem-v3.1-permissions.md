# MultiGolem V3.1 Permissions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add LuckPerms-compatible, permissive-by-default permissions for player-built MultiGolem creation and ingot-based golem healing without changing village spawns, commands, spawn eggs, mob spawners, existing golems, drops, stats, abilities, targeting, or anger behavior.

**Architecture:** V3.1 adds one small permissions package that owns node names, permissive tier checks through `fabric-permissions-api`, bypass semantics, and denial messages. Healing is gated directly in the existing `IronGolemMixin`; player-built T-pattern creation is gated by a narrow block-placement context bridge because `CarvedPumpkinBlock.trySpawnGolem(Level, BlockPos)` does not expose the placing player.

**Tech Stack:** Java 25 · Fabric Loader 0.19.2 · Fabric API 0.148.0+26.1.2 · Fabric Loom · Minecraft 26.1.2 with Mojang official mappings · JUnit 5 · Mixin · `me.lucko:fabric-permissions-api:0.7.0`

**Reference spec:** `docs/superpowers/specs/2026-05-17-multigolem-permissions-design.md`
**Plan style reference:** `docs/superpowers/plans/2026-05-17-multigolem-v3.md`
**API source-of-truth doc:** `docs/26.1.2-mojang-targets.md`
**Lessons learned:** `docs/LESSONS-LEARNED.md`

---

## Pre-Plan Spike Findings

These findings were checked inline before this plan was written. Task 1 still records them into the project API notes before implementation.

- `fabric-permissions-api` is not bundled by the current `net.fabricmc.fabric-api:fabric-api:0.148.0+26.1.2` dependency. Use the cached/current `me.lucko:fabric-permissions-api:0.7.0` artifact and nest it with Loom so servers without LuckPerms or a separate permissions API jar still load MultiGolem.
- Permission API package: `me.lucko.fabric.api.permissions.v0`.
- Runtime check method: `Permissions.check(Entity entity, String permission, boolean defaultValue)`.
- Lower-level value method: `Permissions.getPermissionValue(Entity entity, String permission)` returns Fabric `TriState`.
- Current `CarvedPumpkinBlockMixin` injects into `trySpawnGolem(Level, BlockPos)`, and the vanilla source confirms that method has no player parameter.
- `BlockItem.place(BlockPlaceContext)` has `Player player = updatedPlaceContext.getPlayer()` and calls `this.placeBlock(updatedPlaceContext, placementState)`. That `setBlock` path synchronously invokes `CarvedPumpkinBlock.onPlace(...)` and then `trySpawnGolem(...)`, so a narrow thread-local placement bridge can identify a `ServerPlayer` only for the currently placed carved pumpkin or jack-o-lantern at the exact position.
- `Player` exposes `sendSystemMessage(Component)` and `sendOverlayMessage(Component)`. Verified `javap` output includes `public void sendOverlayMessage(net.minecraft.network.chat.Component);` on `net.minecraft.world.entity.player.Player` itself. Use `sendOverlayMessage(Component.literal(...))` for short denial messages because it is available and keeps repeated interaction denial out of chat scrollback.
- `InteractionResult.FAIL` is the clean deny result for healing. It consumes no item action, prevents vanilla repair feedback and item consumption, and does not fall through to vanilla `IronGolem#mobInteract`.

---

## File Structure

**New Java files:**

```
src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java
src/main/java/dev/charles/multigolem/permissions/PumpkinPlacementTracker.java
src/main/java/dev/charles/multigolem/mixin/BlockItemMixin.java
```

**Modified Java/resources/build files:**

```
build.gradle
src/main/java/dev/charles/multigolem/GolemVariant.java
src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java
src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java
src/main/resources/multigolem.mixins.json
```

**New tests:**

```
src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java
src/test/java/dev/charles/multigolem/permissions/PumpkinPlacementTrackerTest.java
```

**Docs:**

```
docs/26.1.2-mojang-targets.md
README.md
docs/modrinth-listing.md
docs/curseforge-listing.md
docs/playtest-checklist.md
docs/playtest.html
CHANGELOG.md
```

---

## Conventions

- Do not implement code before Tyler approves this plan.
- Run source/API spikes inline, not via subagents.
- Do not parallelize Gradle test tasks in one worktree.
- Use path-specific staging in every commit.
- Permission checks are permissive by default. Missing provider, missing node, or absent LuckPerms must allow the action.
- Gate only player actions: player-built MultiGolem T-pattern creation and ingot-based healing.
- Do not gate village natural spawns, commands, spawn eggs, mob spawners, existing golems, drops, stats, abilities, targeting, or anger behavior.
- Keep permission node strings in `MultiGolemPermissions` only.
- Treat config/docs order and permission node names as admin UX.
- Keep a Claude/reviewer feedback loop for mixins, dependency edges, and permission semantics before final publish.
- V3 in-game smoke remains a release gate before final publish; V3.1 adds permission playtest rows on top.

---

## Task 1: Blocking V3.1 Source/API Spike Notes

No mod code is written in this task. Record the already-verified source/API findings in the canonical API doc. If any verification disagrees with the pre-plan findings above, stop and update this plan before implementation.

**Files:**
- Modify: `docs/26.1.2-mojang-targets.md`

- [ ] **Step 1.1: Re-confirm permissions API artifact and signatures**

Run:

```powershell
jar tf 'C:\Users\tyler\.gradle\caches\modules-2\files-2.1\me.lucko\fabric-permissions-api\0.7.0\8c536967c417676af59b4574fc079444a19c87fd\fabric-permissions-api-0.7.0.jar'
javap -classpath 'C:\Users\tyler\.gradle\caches\modules-2\files-2.1\me.lucko\fabric-permissions-api\0.7.0\8c536967c417676af59b4574fc079444a19c87fd\fabric-permissions-api-0.7.0.jar' me.lucko.fabric.api.permissions.v0.Permissions
```

Expected: output includes `me/lucko/fabric/api/permissions/v0/Permissions.class`, `Permissions.check(net.minecraft.world.entity.Entity, java.lang.String, boolean)`, and `Permissions.getPermissionValue(net.minecraft.world.entity.Entity, java.lang.String)`.

- [ ] **Step 1.2: Re-confirm current Fabric API does not include permissions**

Run:

```powershell
rg -n "fabric-permissions|me/lucko/fabric/api/permissions|permissions/v0" "C:\Users\tyler\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api"
```

Expected: no matches. This confirms `fabric-permissions-api` needs its own dependency.

- [ ] **Step 1.3: Re-confirm player attribution for creation**

Run:

```powershell
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/level/block/CarvedPumpkinBlock.java'
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/item/BlockItem.java'
```

Expected:
- `CarvedPumpkinBlock.trySpawnGolem(final Level level, final BlockPos topPos)` has no player parameter.
- `BlockItem.place(BlockPlaceContext)` has access to `updatedPlaceContext.getPlayer()` and `updatedPlaceContext.getClickedPos()`.
- `BlockItem.place(...)` calls `this.placeBlock(updatedPlaceContext, placementState)` before item consumption, and that placement path triggers `CarvedPumpkinBlock.onPlace(...)` synchronously.

- [ ] **Step 1.4: Re-confirm denial message and healing deny APIs**

Run:

```powershell
javap -classpath '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2.jar' net.minecraft.world.entity.player.Player
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/InteractionResult.java'
```

Expected:
- `Player` exposes `public void sendOverlayMessage(net.minecraft.network.chat.Component);` and `public void sendSystemMessage(net.minecraft.network.chat.Component);` on `net.minecraft.world.entity.player.Player` itself.
- `InteractionResult.FAIL` has `consumesAction() == false`.

- [ ] **Step 1.5: Append V3.1 findings**

Append this section to `docs/26.1.2-mojang-targets.md`:

```markdown
## V3.1 Spike Findings (2026-05-17)

Output of V3.1 permissions planning. Used by player-built creation and healing permission gates.

### Fabric permissions API

- **Artifact:** `me.lucko:fabric-permissions-api:0.7.0`.
- **Bundled by Fabric API 0.148.0+26.1.2:** no.
- **Package:** `me.lucko.fabric.api.permissions.v0`.
- **Primary method:** `Permissions.check(Entity entity, String permission, boolean defaultValue)`.
- **Default behavior:** pass `true` as the default so missing providers or missing nodes allow the action.
- **Dependency strategy:** nest the API with Loom via `modImplementation include(...)` so MultiGolem still loads when a server has no separate permissions provider installed.

### Player-built T-pattern creation attribution

- **Current pumpkin hook:** `CarvedPumpkinBlock.trySpawnGolem(Level, BlockPos)` has no player parameter.
- **Chosen strategy:** add a narrow `BlockItem.place(BlockPlaceContext)` redirect around `placeBlock(updatedPlaceContext, placementState)` for carved pumpkin and jack-o-lantern placements. Store the current `ServerPlayer`, `ServerLevel`, and exact pumpkin position in a thread-local only while the vanilla placement call is executing.
- **Why chosen:** placement and `CarvedPumpkinBlock.onPlace(...)` are synchronous, so the existing `CarvedPumpkinBlockMixin` can read the responsible player for the exact top position without a broad global fallback.
- **Rejected strategy:** do not scan nearby players or use a broad entity/event fallback; that would misattribute non-player or unrelated golem creation.

### Denial messages and healing denial

- **Message API:** use `Player.sendOverlayMessage(Component.literal(message))` for short denial messages. `sendOverlayMessage` is declared on `net.minecraft.world.entity.player.Player`, so `sendHealDenied(Player, ...)` is valid.
- **Healing denial result:** return `InteractionResult.FAIL` before healing, sound, or item consumption.
```

- [ ] **Step 1.6: Commit spike notes**

Run:

```powershell
git add docs/26.1.2-mojang-targets.md
git commit -m "docs: record V3.1 permissions API spike"
```

Expected: commit succeeds.

---

## Task 2: Add Fabric Permissions Dependency

**Files:**
- Modify: `build.gradle`

- [ ] **Step 2.1: Add repository and nested dependency**

Modify `build.gradle`:

```groovy
repositories {
	mavenCentral()
}

dependencies {
	// Existing dependencies remain unchanged.
	modImplementation include("me.lucko:fabric-permissions-api:0.7.0")
}
```

Keep the existing `implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"` line. Add the permissions dependency near the Fabric API dependency so dependency intent is obvious.

- [ ] **Step 2.2: Verify dependency resolves**

Run:

```powershell
.\gradlew.bat --quiet compileJava
```

Expected: compile succeeds and Gradle resolves `me.lucko:fabric-permissions-api:0.7.0`.

- [ ] **Step 2.3: Commit dependency**

Run:

```powershell
git add build.gradle
git commit -m "build: add Fabric permissions API"
```

Expected: commit succeeds.

---

## Task 3: Add `MultiGolemPermissions` Pure Helper (TDD)

**Files:**
- Create: `src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java`
- Create: `src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java`
- Modify: `src/main/java/dev/charles/multigolem/GolemVariant.java`

- [ ] **Step 3.1: Write failing tests**

Create `MultiGolemPermissionsTest`:

```java
package dev.charles.multigolem.permissions;

import dev.charles.multigolem.GolemVariant;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemPermissionsTest {
    private record Call(String node, boolean defaultValue) {}

    @Test
    void createNodes_matchSpecExactly() {
        assertEquals("multigolem.create.copper", MultiGolemPermissions.createNode(GolemVariant.COPPER));
        assertEquals("multigolem.create.gold", MultiGolemPermissions.createNode(GolemVariant.GOLD));
        assertEquals("multigolem.create.emerald", MultiGolemPermissions.createNode(GolemVariant.EMERALD));
        assertEquals("multigolem.create.diamond", MultiGolemPermissions.createNode(GolemVariant.DIAMOND));
        assertEquals("multigolem.create.netherite", MultiGolemPermissions.createNode(GolemVariant.NETHERITE));
    }

    @Test
    void healNodes_matchSpecExactlyIncludingIron() {
        assertEquals("multigolem.heal.copper", MultiGolemPermissions.healNode(GolemVariant.COPPER));
        assertEquals("multigolem.heal.iron", MultiGolemPermissions.healNode(GolemVariant.IRON));
        assertEquals("multigolem.heal.gold", MultiGolemPermissions.healNode(GolemVariant.GOLD));
        assertEquals("multigolem.heal.emerald", MultiGolemPermissions.healNode(GolemVariant.EMERALD));
        assertEquals("multigolem.heal.diamond", MultiGolemPermissions.healNode(GolemVariant.DIAMOND));
        assertEquals("multigolem.heal.netherite", MultiGolemPermissions.healNode(GolemVariant.NETHERITE));
    }

    @Test
    void adminBypass_isCheckedBeforeTierNode() {
        List<Call> calls = new ArrayList<>();
        MultiGolemPermissions.PermissionLookup lookup = (node, defaultValue) -> {
            calls.add(new Call(node, defaultValue));
            return node.equals(MultiGolemPermissions.ADMIN_BYPASS_NODE);
        };

        assertTrue(MultiGolemPermissions.canCreate(GolemVariant.DIAMOND, lookup));
        assertEquals(List.of(new Call("multigolem.admin.bypass", false)), calls);
    }

    @Test
    void missingProviderOrMissingNode_defaultsToAllow() {
        List<Call> calls = new ArrayList<>();
        MultiGolemPermissions.PermissionLookup lookup = (node, defaultValue) -> {
            calls.add(new Call(node, defaultValue));
            return defaultValue;
        };

        assertTrue(MultiGolemPermissions.canCreate(GolemVariant.NETHERITE, lookup));
        assertTrue(MultiGolemPermissions.canHeal(GolemVariant.IRON, lookup));
        assertEquals(false, calls.get(0).defaultValue());
        assertEquals(true, calls.get(1).defaultValue());
        assertEquals(false, calls.get(2).defaultValue());
        assertEquals(true, calls.get(3).defaultValue());
    }

    @Test
    void explicitTierDenial_returnsFalseWhenBypassAbsent() {
        MultiGolemPermissions.PermissionLookup lookup = (node, defaultValue) ->
            node.equals(MultiGolemPermissions.ADMIN_BYPASS_NODE) ? false : !node.equals("multigolem.create.diamond");

        assertFalse(MultiGolemPermissions.canCreate(GolemVariant.DIAMOND, lookup));
    }

    @Test
    void denialMessages_useStableDisplayNames() {
        assertEquals("Iron", GolemVariant.IRON.displayName());
        assertEquals("Emerald", GolemVariant.EMERALD.displayName());
        assertEquals("Diamond", GolemVariant.DIAMOND.displayName());
        assertEquals("Netherite", GolemVariant.NETHERITE.displayName());
        assertEquals("You do not have permission to heal an Iron golem.",
            MultiGolemPermissions.healDeniedMessage(GolemVariant.IRON));
        assertEquals("You do not have permission to heal an Emerald golem.",
            MultiGolemPermissions.healDeniedMessage(GolemVariant.EMERALD));
        assertEquals("You do not have permission to create a Diamond golem.",
            MultiGolemPermissions.createDeniedMessage(GolemVariant.DIAMOND));
        assertEquals("You do not have permission to heal a Netherite golem.",
            MultiGolemPermissions.healDeniedMessage(GolemVariant.NETHERITE));
    }
}
```

- [ ] **Step 3.2: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
```

Expected: FAIL because `MultiGolemPermissions` and `GolemVariant.displayName()` do not exist.

- [ ] **Step 3.3: Implement display names**

Modify `GolemVariant` with a display name field:

```java
COPPER   ("copper",    "Copper",    Blocks.COPPER_BLOCK,    Items.COPPER_INGOT,    Items.COPPER_INGOT),
IRON     ("iron",      "Iron",      Blocks.IRON_BLOCK,      Items.IRON_INGOT,      Items.IRON_INGOT),
GOLD     ("gold",      "Gold",      Blocks.GOLD_BLOCK,      Items.GOLD_INGOT,      Items.GOLD_INGOT),
EMERALD  ("emerald",   "Emerald",   Blocks.EMERALD_BLOCK,   Items.EMERALD,         Items.EMERALD),
DIAMOND  ("diamond",   "Diamond",   Blocks.DIAMOND_BLOCK,   Items.DIAMOND,         Items.DIAMOND),
NETHERITE("netherite", "Netherite", Blocks.NETHERITE_BLOCK, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP);

private final String displayName;

GolemVariant(String id, String displayName, Block bodyBlock, Item healIngot, Item dropItem) {
    this.id = id;
    this.displayName = displayName;
    this.bodyBlock = bodyBlock;
    this.healIngot = healIngot;
    this.dropItem = dropItem;
}

public String displayName() { return displayName; }
```

- [ ] **Step 3.4: Implement permission helper**

Create `MultiGolemPermissions`:

```java
package dev.charles.multigolem.permissions;

import dev.charles.multigolem.GolemVariant;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class MultiGolemPermissions {
    public static final String ADMIN_BYPASS_NODE = "multigolem.admin.bypass";

    @FunctionalInterface
    interface PermissionLookup {
        boolean check(String node, boolean defaultValue);
    }

    private MultiGolemPermissions() {}

    public static boolean canCreate(ServerPlayer player, GolemVariant variant) {
        return canCreate(variant, (node, defaultValue) -> Permissions.check(player, node, defaultValue));
    }

    public static boolean canHeal(Player player, GolemVariant variant) {
        return canHeal(variant, (node, defaultValue) -> Permissions.check(player, node, defaultValue));
    }

    static boolean canCreate(GolemVariant variant, PermissionLookup lookup) {
        return checkWithBypass(createNode(variant), lookup);
    }

    static boolean canHeal(GolemVariant variant, PermissionLookup lookup) {
        return checkWithBypass(healNode(variant), lookup);
    }

    private static boolean checkWithBypass(String node, PermissionLookup lookup) {
        if (lookup.check(ADMIN_BYPASS_NODE, false)) return true;
        return lookup.check(node, true);
    }

    static String createNode(GolemVariant variant) {
        return "multigolem.create." + variant.id();
    }

    static String healNode(GolemVariant variant) {
        return "multigolem.heal." + variant.id();
    }

    public static void sendCreateDenied(ServerPlayer player, GolemVariant variant) {
        player.sendOverlayMessage(Component.literal(createDeniedMessage(variant)));
    }

    public static void sendHealDenied(Player player, GolemVariant variant) {
        player.sendOverlayMessage(Component.literal(healDeniedMessage(variant)));
    }

    static String createDeniedMessage(GolemVariant variant) {
        return "You do not have permission to create " + article(variant.displayName()) + " " + variant.displayName() + " golem.";
    }

    static String healDeniedMessage(GolemVariant variant) {
        return "You do not have permission to heal " + article(variant.displayName()) + " " + variant.displayName() + " golem.";
    }

    private static String article(String displayName) {
        return "AEIOUaeiou".indexOf(displayName.charAt(0)) >= 0 ? "an" : "a";
    }
}
```

- [ ] **Step 3.5: Run tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
git add src/test/java/dev/charles/multigolem/permissions/MultiGolemPermissionsTest.java src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java src/main/java/dev/charles/multigolem/GolemVariant.java
git commit -m "feat: add MultiGolem permission helper"
```

Expected: tests pass and commit succeeds.

---

## Task 4: Add Pumpkin Placement Tracker (TDD)

**Files:**
- Create: `src/test/java/dev/charles/multigolem/permissions/PumpkinPlacementTrackerTest.java`
- Create: `src/main/java/dev/charles/multigolem/permissions/PumpkinPlacementTracker.java`

- [ ] **Step 4.1: Write failing tests**

Create `PumpkinPlacementTrackerTest`:

```java
package dev.charles.multigolem.permissions;

import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PumpkinPlacementTrackerTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void noPlacement_returnsEmpty() {
        assertTrue(PumpkinPlacementTracker.currentPlayerFor(BlockPos.ZERO).isEmpty());
    }

    @Test
    void activePlacement_matchesOnlyExactPumpkinPosition() {
        Object player = new Object();
        BlockPos pos = new BlockPos(10, 64, -3);

        PumpkinPlacementTracker.withCurrentPlacement(player, pos, () -> {
            assertEquals(player, PumpkinPlacementTracker.currentPlayerFor(pos).orElseThrow());
            assertTrue(PumpkinPlacementTracker.currentPlayerFor(pos.above()).isEmpty());
        });
    }

    @Test
    void placementClearsAfterCallback() {
        Object player = new Object();
        BlockPos pos = new BlockPos(1, 2, 3);

        PumpkinPlacementTracker.withCurrentPlacement(player, pos, () -> {});

        assertTrue(PumpkinPlacementTracker.currentPlayerFor(pos).isEmpty());
    }

    @Test
    void placementClearsAfterException() {
        Object player = new Object();
        BlockPos pos = new BlockPos(4, 5, 6);

        assertThrows(IllegalStateException.class, () ->
            PumpkinPlacementTracker.withCurrentPlacement(player, pos, () -> {
                throw new IllegalStateException("boom");
            }));

        assertTrue(PumpkinPlacementTracker.currentPlayerFor(pos).isEmpty());
    }
}
```

- [ ] **Step 4.2: Run tests and verify failure**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.permissions.PumpkinPlacementTrackerTest
```

Expected: FAIL because `PumpkinPlacementTracker` does not exist.

- [ ] **Step 4.3: Implement tracker**

Create `PumpkinPlacementTracker`:

```java
package dev.charles.multigolem.permissions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class PumpkinPlacementTracker {
    private static final ThreadLocal<Placement> CURRENT = new ThreadLocal<>();

    private PumpkinPlacementTracker() {}

    public static void withCurrentPlacement(ServerPlayer player, BlockPos pos, Runnable action) {
        withCurrentPlacement((Object) player, pos, action);
    }

    static void withCurrentPlacement(Object player, BlockPos pos, Runnable action) {
        CURRENT.set(new Placement(player, pos.immutable()));
        try {
            action.run();
        } finally {
            CURRENT.remove();
        }
    }

    public static Optional<ServerPlayer> currentServerPlayerFor(BlockPos pos) {
        return currentPlayerFor(pos)
            .filter(ServerPlayer.class::isInstance)
            .map(ServerPlayer.class::cast);
    }

    static Optional<Object> currentPlayerFor(BlockPos pos) {
        Placement placement = CURRENT.get();
        if (placement == null || !placement.pos.equals(pos)) return Optional.empty();
        return Optional.of(placement.player);
    }

    private record Placement(Object player, BlockPos pos) {}
}
```

- [ ] **Step 4.4: Run tests and commit**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.permissions.PumpkinPlacementTrackerTest
git add src/test/java/dev/charles/multigolem/permissions/PumpkinPlacementTrackerTest.java src/main/java/dev/charles/multigolem/permissions/PumpkinPlacementTracker.java
git commit -m "feat: track active pumpkin placements"
```

Expected: tests pass and commit succeeds.

---

## Task 5: Bridge Block Placement Context Into Creation Path

**Files:**
- Create: `src/main/java/dev/charles/multigolem/mixin/BlockItemMixin.java`
- Modify: `src/main/resources/multigolem.mixins.json`

- [ ] **Step 5.1: Implement narrow `BlockItem.placeBlock` redirect**

Create `BlockItemMixin`:

```java
package dev.charles.multigolem.mixin;

import dev.charles.multigolem.permissions.PumpkinPlacementTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Shadow
    protected abstract boolean placeBlock(BlockPlaceContext context, BlockState placementState);

    @Redirect(
        method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean multigolem$trackPumpkinPlacement(BlockItem instance, BlockPlaceContext context, BlockState placementState) {
        boolean isPumpkinHead = placementState.is(Blocks.CARVED_PUMPKIN) || placementState.is(Blocks.JACK_O_LANTERN);
        if (isPumpkinHead && context.getLevel() instanceof ServerLevel && context.getPlayer() instanceof ServerPlayer player) {
            final boolean[] placed = new boolean[1];
            PumpkinPlacementTracker.withCurrentPlacement(player, context.getClickedPos(), () ->
                placed[0] = this.placeBlock(context, placementState));
            return placed[0];
        }
        return this.placeBlock(context, placementState);
    }
}
```

If Mixin rejects shadow dispatch for this redirect at build or runtime, stop for review before switching to Mixin Extras. Do not replace this with a broad event fallback.

- [ ] **Step 5.2: Register mixin**

Add `"BlockItemMixin"` to `src/main/resources/multigolem.mixins.json` near `CarvedPumpkinBlockMixin`.

- [ ] **Step 5.3: Build and commit**

Run:

```powershell
.\gradlew.bat --quiet build
git add src/main/java/dev/charles/multigolem/mixin/BlockItemMixin.java src/main/resources/multigolem.mixins.json
git commit -m "feat: capture pumpkin placement player context"
```

Expected: build passes. If it fails because the redirect target changed, return to Task 1 source notes and fix the narrow redirect target only.

- [ ] **Step 5.4: Immediate server startup smoke for required mixin**

Before continuing to Task 6, start a local Fabric 26.1.2 server with the built jar.

Expected:
- server reaches startup without a Mixin apply error for `BlockItemMixin`.
- no `InvalidMixinException`, redirect target mismatch, or `placeBlock` shadow failure appears in the log.

If startup fails here, stop for review. Do not continue to the creation gate and do not switch to Mixin Extras without an explicit plan update.

---

## Task 6: Gate Player-Built MultiGolem Creation

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java`

- [ ] **Step 6.1: Add permission check before block clearing**

In `GolemCreationHandler.trySpawnVariant(Level level, BlockPos topPos)`, after a non-iron variant pattern matches and before `EntityType.IRON_GOLEM.create(...)` or `CarvedPumpkinBlock.clearPatternBlocks(...)`, add:

```java
Optional<ServerPlayer> responsiblePlayer = PumpkinPlacementTracker.currentServerPlayerFor(topPos);
if (responsiblePlayer.isPresent()) {
    ServerPlayer player = responsiblePlayer.get();
    if (!MultiGolemPermissions.canCreate(player, variant)) {
        MultiGolemPermissions.sendCreateDenied(player, variant);
        return true;
    }
}
```

Add imports:

```java
import dev.charles.multigolem.permissions.MultiGolemPermissions;
import dev.charles.multigolem.permissions.PumpkinPlacementTracker;
import java.util.Optional;
```

Required behavior:
- Denied player-built MultiGolem creation returns `true` so `CarvedPumpkinBlockMixin` cancels vanilla golem logic for that matched MultiGolem T-pattern.
- Denial happens before clearing blocks, spawning a golem, triggering criteria, or updating neighbors.
- No responsible player means current behavior proceeds. This preserves non-player or unsupported contexts instead of guessing.
- Vanilla iron T-pattern creation remains vanilla-owned and has no `multigolem.create.iron` node.

- [ ] **Step 6.2: Build and commit**

Run:

```powershell
.\gradlew.bat --quiet build
git add src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java
git commit -m "feat: gate player-built variant creation"
```

Expected: build passes.

---

## Task 7: Gate Ingot-Based Healing

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java`

- [ ] **Step 7.1: Replace method body with permission-aware healing flow**

Replace the full body of `IronGolemMixin#multigolem$healWithMatchingIngot` with this flow:

```java
if (!MultiGolem.config().allowGolemHealing()) {
    // Healing disabled globally — cancel ALL heal interactions (including vanilla iron→iron)
    // so the iron-ingot heal doesn't sneak through.
    ItemStack stack = player.getItemInHand(hand);
    if (GolemVariant.fromIngot(stack.getItem()).isPresent()) {
        cir.setReturnValue(InteractionResult.FAIL);
    }
    return;
}

IronGolem self = (IronGolem) (Object) this;
GolemVariant variant = GolemVariantAttachment.get(self);
ItemStack stack = player.getItemInHand(hand);
if (stack.isEmpty()) return;

Optional<GolemVariant> held = GolemVariant.fromIngot(stack.getItem());
if (held.isEmpty() || held.get() != variant) return;

if (!MultiGolemPermissions.canHeal(player, variant)) {
    MultiGolemPermissions.sendHealDenied(player, variant);
    cir.setReturnValue(InteractionResult.FAIL);
    return;
}

if (variant == GolemVariant.IRON) {
    // Permission allowed; vanilla handles iron→iron heal.
    return;
}

float before = self.getHealth();
self.heal(25.0F);
if (self.getHealth() == before) {
    // already full HP — do nothing (mimics vanilla PASS behavior)
    return;
}
float pitch = 1.0F + (self.getRandom().nextFloat() - self.getRandom().nextFloat()) * 0.2F;
self.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
stack.consume(1, player);
cir.setReturnValue(InteractionResult.SUCCESS);
```

Required ordering:
- The existing `allow_golem_healing` global disablement remains first and unchanged.
- Permission checks happen only after confirming the held item matches the golem variant.
- For Iron, denial happens before falling through to vanilla. If allowed, return and let vanilla handle iron healing.
- For non-Iron, denial happens before `self.heal(25.0F)`, repair sound, or `stack.consume(1, player)`.

Add import:

```java
import dev.charles.multigolem.permissions.MultiGolemPermissions;
```

- [ ] **Step 7.2: Build and commit**

Run:

```powershell
.\gradlew.bat --quiet build
git add src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java
git commit -m "feat: gate golem healing permissions"
```

Expected: build passes. Review the diff to confirm `InteractionResult.FAIL` is returned only for explicit permission denial or global healing disablement, not for wrong items or full-health golems.

---

## Task 8: Update Permission Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/modrinth-listing.md`
- Modify: `docs/curseforge-listing.md`

- [ ] **Step 8.1: Update README admin section**

Add a permissions subsection that lists exactly:

```markdown
### Permissions

MultiGolem supports LuckPerms-compatible permissions through Fabric Permissions API. Permissions are permissive by default: existing servers keep working unless a permissions provider explicitly denies a node.

Creation permissions:

- `multigolem.create.copper`
- `multigolem.create.gold`
- `multigolem.create.emerald`
- `multigolem.create.diamond`
- `multigolem.create.netherite`

Healing permissions:

- `multigolem.heal.copper`
- `multigolem.heal.iron`
- `multigolem.heal.gold`
- `multigolem.heal.emerald`
- `multigolem.heal.diamond`
- `multigolem.heal.netherite`

Bypass:

- `multigolem.admin.bypass`

These nodes only affect player-built MultiGolem T-pattern creation and ingot-based golem healing. Village spawns, commands, spawn eggs, mob spawners, existing golems, drops, stats, abilities, targeting, and anger behavior are unchanged.
```

- [ ] **Step 8.2: Update Modrinth and CurseForge listings**

Add a short server-admin note to both marketplace listing docs:

```markdown
Server owners can optionally control who may create or heal each MultiGolem tier with LuckPerms-compatible permission nodes. Permissions are permissive by default, so existing servers keep their current behavior unless a permissions plugin denies a node.
```

Keep marketplace wording player/admin-facing. Do not mention mixins, thread-locals, or `fabric-permissions-api` internals.

- [ ] **Step 8.3: Commit docs**

Run:

```powershell
git add README.md docs/modrinth-listing.md docs/curseforge-listing.md
git commit -m "docs: document MultiGolem permissions"
```

Expected: commit succeeds.

---

## Task 9: Update Playtest Checklists

**Files:**
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`

- [ ] **Step 9.1: Add Markdown V3.1 rows**

Add a `# V3.1 - permissions` section to `docs/playtest-checklist.md`:

```markdown
# V3.1 - permissions

## Creation permissions

- [ ] With no LuckPerms or permissions provider installed, player-built Diamond and Netherite MultiGolem T-patterns still spawn normally.
- [ ] Denying `multigolem.create.diamond` prevents a player-built Diamond MultiGolem T-pattern from spawning.
- [ ] Denied Diamond creation leaves all T-pattern blocks intact.
- [ ] Denied Diamond creation shows `You do not have permission to create a Diamond golem.`
- [ ] Granting `multigolem.create.diamond` allows Diamond T-pattern creation.
- [ ] `multigolem.admin.bypass` allows Diamond creation even when `multigolem.create.diamond` is denied.
- [ ] Vanilla Iron golem T-pattern creation is unchanged and has no MultiGolem creation permission node.

## Healing permissions

- [ ] With no LuckPerms or permissions provider installed, ingot-based healing still works normally.
- [ ] Denying `multigolem.heal.netherite` prevents Netherite golem healing.
- [ ] Denied Netherite healing does not consume the netherite ingot.
- [ ] Denied Netherite healing does not play vanilla repair feedback.
- [ ] Denied Netherite healing shows `You do not have permission to heal a Netherite golem.`
- [ ] Granting `multigolem.heal.netherite` allows Netherite healing.
- [ ] Denying `multigolem.heal.iron` prevents vanilla Iron golem iron-ingot healing.
- [ ] `multigolem.admin.bypass` allows healing even when the tier-specific heal node is denied.

## Scope negatives

- [ ] Village natural spawns are not permission-gated.
- [ ] Command-spawned golems are not permission-gated.
- [ ] Spawn egg golems are not permission-gated.
- [ ] Mob spawner golems are not permission-gated.
- [ ] Existing golems, drops, stats, abilities, targeting, and anger behavior are unchanged.
```

- [ ] **Step 9.2: Add matching HTML rows**

Add the same V3.1 checklist labels to `docs/playtest.html`. Keep the existing progress script and storage keys working.

- [ ] **Step 9.3: Commit playtest docs**

Run:

```powershell
git add docs/playtest-checklist.md docs/playtest.html
git commit -m "docs: add V3.1 permission playtest rows"
```

Expected: commit succeeds.

---

## Task 10: Update CHANGELOG

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 10.1: Add Unreleased notes**

Under `## Unreleased`, add:

```markdown
- Server owners can now control who may create or heal each MultiGolem tier using LuckPerms-compatible permission nodes.
- Permissions are permissive by default, so existing servers keep their current creation and healing behavior unless a permissions plugin denies a node.
- Permission checks only apply to player-built MultiGolem creation and ingot-based healing; village spawns, commands, spawn eggs, mob spawners, and existing golems are unchanged.
```

Do not mention implementation details.

- [ ] **Step 10.2: Run style gate and commit**

Run:

```powershell
python scripts\check-changelog-style.py --section Unreleased
git add CHANGELOG.md
git commit -m "docs: add V3.1 changelog entry"
```

Expected: style gate passes and commit succeeds.

---

## Task 11: Review Loop And Verification

**Files:**
- No expected source edits unless review or verification finds a focused issue.

- [ ] **Step 11.1: Run focused unit tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.permissions.MultiGolemPermissionsTest
.\gradlew.bat --quiet test --tests dev.charles.multigolem.permissions.PumpkinPlacementTrackerTest
```

Expected: both test classes pass.

- [ ] **Step 11.2: Run full test suite**

Run:

```powershell
.\gradlew.bat --quiet test
```

Expected: all unit tests pass.

- [ ] **Step 11.3: Run full build**

Run:

```powershell
.\gradlew.bat --quiet build
```

Expected: build passes, including `checkChangelog`, `checkReleaseNotesStyle`, `checkUnitTestCompanions`, and texture generation.

- [ ] **Step 11.4: Claude/reviewer prompt for mixin/config/dependency edges**

Send this focused review prompt before final publish:

```text
Review MultiGolem V3.1 permissions in C:\Users\tyler\AI Projects\MultiGolem.

Focus only on correctness risks in:
- build.gradle dependency/repository handling for nested fabric-permissions-api
- src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java
- src/main/java/dev/charles/multigolem/permissions/PumpkinPlacementTracker.java
- src/main/java/dev/charles/multigolem/mixin/BlockItemMixin.java
- src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java
- src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java
- src/main/resources/multigolem.mixins.json

Check for:
1. wrong permission default semantics
2. node name mistakes
3. bypass ordering/default mistakes
4. mixin target/descriptor fragility
5. player attribution mistakes in pumpkin placement
6. denied creation accidentally clearing blocks or spawning golems
7. denied healing accidentally playing repair feedback or consuming items
8. accidental gating of village spawns, commands, spawn eggs, mob spawners, existing golems, drops, stats, abilities, targeting, or anger behavior

Return numbered findings with file paths and exact risk. Do not suggest broad refactors.

Also include a follow-up prompt for Codex/Tyler to paste back into Codex, inside a fenced code block for easy copy/paste.
```

Expected: review returns no blocking findings, or focused findings are fixed in follow-up commits.

- [ ] **Step 11.5: Server startup smoke without LuckPerms**

Start a Fabric 26.1.2 server with the built MultiGolem jar and Fabric API, but without LuckPerms.

Expected:
- server starts without `NoClassDefFoundError` for `me.lucko.fabric.api.permissions.v0.Permissions`.
- player-built non-iron MultiGolem creation still works.
- ingot healing still works.

- [ ] **Step 11.6: Server smoke with LuckPerms**

Start with LuckPerms installed. Verify:

```text
deny multigolem.create.diamond
deny multigolem.heal.netherite
grant multigolem.admin.bypass
```

Expected:
- denied Diamond creation is blocked and leaves blocks intact.
- denied Netherite healing is blocked and consumes no item.
- bypass allows both actions.

- [ ] **Step 11.7: V3 release gate remains open until smoke finishes**

Confirm Tyler's V3 village natural-spawn smoke has no outstanding blocker before final V3.1 publish prep.

Expected: V3 smoke remains acceptable, and V3.1 permission smoke passes.

- [ ] **Step 11.8: Commit any final fixes**

If verification or review required changes, commit each focused fix with path-specific staging:

```powershell
git add <exact-files>
git commit -m "fix: <focused V3.1 permission issue>"
```

Expected: final `.\gradlew.bat --quiet build` passes after the fix.

---

## Self-Review Checklist

- Plan includes the first blocking source/API spike task and records exact findings to `docs/26.1.2-mojang-targets.md`.
- `fabric-permissions-api` is used directly; no LuckPerms API calls are introduced.
- Permissions default to allow through `Permissions.check(..., true)`.
- `multigolem.admin.bypass` is checked before tier-specific denial with default `false`; tier nodes are checked with default `true`.
- Permission node strings match the spec exactly.
- Only player-built MultiGolem T-pattern creation and ingot-based healing are gated.
- Creation denial happens before clearing blocks or spawning a golem.
- Healing denial happens before heal, sound, or item consumption and returns `InteractionResult.FAIL`.
- Vanilla Iron T-pattern creation remains vanilla-owned and has no create node.
- Iron healing is gated with `multigolem.heal.iron`.
- Village natural spawns, commands, spawn eggs, mob spawners, existing golems, drops, stats, abilities, targeting, and anger behavior are not gated.
- Docs include `README.md`, `docs/modrinth-listing.md`, `docs/curseforge-listing.md`, `docs/playtest-checklist.md`, `docs/playtest.html`, and `CHANGELOG.md`.
- No V4 spawn eggs or V5 copper golem variants are included.
