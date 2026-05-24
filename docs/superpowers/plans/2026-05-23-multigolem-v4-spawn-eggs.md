# MultiGolem V4 Spawn Eggs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add five marked vanilla iron golem spawn egg stacks for Copper, Gold, Emerald, Diamond, and Netherite MultiGolem variants without adding new item IDs, entity types, data components, creative tabs, recipes, or separate permission nodes.

**Architecture:** V4 treats variant spawn eggs as `minecraft:iron_golem_spawn_egg` stacks whose only authoritative marker is exact vanilla `minecraft:custom_data` containing `{multigolem:{variant:"<id>"}}`. A single stack factory writes that marker, a narrow `SpawnEggItem.spawnMob` redirect handles normal egg use, client resources select per-tier icons from exact `custom_data`, and spawner support is isolated as the final cuttable task.

**Tech Stack:** Java 25 · Fabric Loader 0.19.2 · Fabric API 0.148.0+26.1.2 · Fabric Loom · Minecraft 26.1.2 with Mojang official mappings · Mixin · JUnit 5 · Fabric Permissions API via existing V3.1 helper

**Reference docs:** `docs/LESSONS-LEARNED.md`, `docs/26.1.2-mojang-targets.md`, `docs/superpowers/specs/2026-05-15-multigolem-design.md`, `docs/superpowers/specs/2026-05-17-multigolem-permissions-design.md`, `docs/superpowers/plans/2026-05-17-multigolem-v3.1-permissions.md`

---

## File Structure

**New Java files:**

```text
src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java
src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java
src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java
src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java
src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java
src/main/java/dev/charles/multigolem/mixin/BaseSpawnerAccessor.java
```

**Modified Java/resources/build files:**

```text
src/main/java/dev/charles/multigolem/MultiGolem.java
src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java
src/main/resources/multigolem.mixins.json
src/client/java/dev/charles/multigolem/client/MultiGolemClient.java
src/main/resources/assets/minecraft/items/iron_golem_spawn_egg.json
src/main/resources/assets/multigolem/models/item/copper_golem_spawn_egg.json
src/main/resources/assets/multigolem/models/item/gold_golem_spawn_egg.json
src/main/resources/assets/multigolem/models/item/emerald_golem_spawn_egg.json
src/main/resources/assets/multigolem/models/item/diamond_golem_spawn_egg.json
src/main/resources/assets/multigolem/models/item/netherite_golem_spawn_egg.json
src/main/resources/assets/multigolem/textures/item/copper_golem_spawn_egg.png
src/main/resources/assets/multigolem/textures/item/gold_golem_spawn_egg.png
src/main/resources/assets/multigolem/textures/item/emerald_golem_spawn_egg.png
src/main/resources/assets/multigolem/textures/item/diamond_golem_spawn_egg.png
src/main/resources/assets/multigolem/textures/item/netherite_golem_spawn_egg.png
scripts/generate-textures.py
scripts/test-generate-textures.py
build.gradle
CHANGELOG.md
docs/playtest-checklist.md
docs/playtest.html
```

**New tests:**

```text
src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java
src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java
```

**Optional provenance file if a vanilla spawn egg texture is committed as a build input:**

```text
build-inputs/textures/spawn_egg/LICENSE-AND-PROVENANCE.md
```

Do not create new item registry entries, `DataComponentType` registrations, entity types, creative tabs, recipes, or spawn egg permission nodes.

---

## Conventions

- Do not implement code until Tyler starts an execution session from this plan.
- Run `git rev-parse --show-toplevel` before the first edit in the execution session and confirm it is `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/codex-v4-spawn-eggs`.
- Keep V4 focused on marked vanilla `minecraft:iron_golem_spawn_egg` stacks. V5 vanilla `CopperGolem` integration is out of scope.
- Use `GolemVariantAttachment.set(...)` and `VariantAttributes.apply(...)`; do not duplicate attachment or stat logic.
- Reuse `MultiGolemPermissions.canCreate(...)` and `sendCreateDenied(...)`; do not add separate spawn egg permission nodes.
- The stack factory is the sole writer to spawn egg `minecraft:custom_data`.
- `minecraft:custom_data` must be exactly `{multigolem:{variant:"<id>"}}` with no extra keys.
- Unmarked vanilla iron golem spawn eggs stay vanilla-owned.
- Marked eggs and spawner-spawned golems must not call `setPlayerCreated(true)`.
- Denied normal egg use may still arm-swing because `SpawnEggItem.spawnMob` returns `InteractionResult.SUCCESS`; this is accepted with overlay denial feedback.
- Spawner support is a final isolated task and may be cut to V4.1 if the no-dependency mixin path gets brittle.

---

## Task 1: Source/API Confirmation Spike

No mod behavior changes in this task. Confirm the current mappings and APIs match the V4 spike notes before implementing.

**Files:**
- Read: `docs/26.1.2-mojang-targets.md`
- Modify only if findings differ: `docs/26.1.2-mojang-targets.md`

- [ ] **Step 1.1: Verify worktree root**

Run:

```powershell
git rev-parse --show-toplevel
```

Expected: `C:/Users/tyler/AI Projects/MultiGolem/.worktrees/codex-v4-spawn-eggs`.

- [ ] **Step 1.2: Reconfirm `SpawnEggItem.spawnMob` redirect target**

Run:

```powershell
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/item/SpawnEggItem.java'
```

Expected:
- `spawnMob(ServerLevel, ItemStack, Player, BlockPos)` calls `EntityType.spawn(ServerLevel, ItemStack, LivingEntity, BlockPos, EntitySpawnReason, boolean, boolean)`.
- Item consumption and `GameEvent.ENTITY_PLACE` happen only when that spawn call returns non-null.
- Spawner use is handled in `useOn(...)` before `spawnMob(...)` and needs separate hooks.

- [ ] **Step 1.3: Reconfirm component and SNBT APIs**

Run:

```powershell
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/core/component/DataComponents.java'
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/item/component/CustomData.java'
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/nbt/CompoundTag.java'
```

Expected:
- `DataComponents.CUSTOM_DATA` is the vanilla component to set.
- `CustomData.of(CompoundTag)` or equivalent is available.
- `CompoundTag` can store nested compound key `multigolem` and string key `variant`.

- [ ] **Step 1.4: Reconfirm creative tab event side**

Run:

```powershell
rg -n "interface ItemGroupEvents|modifyEntriesEvent|CreativeModeTabs" "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\net.fabricmc.fabric-api"
```

Expected:
- `ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)` is available.
- If the event is safe and available from common/main initialization without synced registry additions, prefer common registration so modded dedicated-server tab population is covered.
- If source inspection or compile/runtime verification shows the event is client-only, use `MultiGolemClient.onInitializeClient()` and verify that modded clients connected to a modded dedicated server still see the entries through client-side registration.
- In either case, do not add synced registry entries, a new creative tab, or new item IDs.

- [ ] **Step 1.5: Reconfirm exact `custom_data` item model matching**

Run:

```powershell
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-clientOnly-a26c9a9f3c\26.1.2\minecraft-clientOnly-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/client/renderer/item/properties/select/ComponentContents.java'
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-clientOnly-a26c9a9f3c\26.1.2\minecraft-clientOnly-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/client/renderer/item/ItemModel.java'
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/item/component/CustomData.java'
```

Expected:
- `minecraft:component` selection for `minecraft:custom_data` uses `CustomData.matchedBy(...)`.
- Extra keys in the actual `CustomData` compound can still match a V4 case because 26.1.2 uses subset-style compound matching.
- Task 4.6 was updated for this behavior before implementation; the stack factory still writes exact marker compounds and remains the sole V4 writer.

- [ ] **Step 1.6: Reconfirm spawner targets**

Run:

```powershell
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/level/SpawnerBlockEntity.java'
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/level/BaseSpawner.java'
tar -xOf '.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-common-a26c9a9f3c\26.1.2\minecraft-common-a26c9a9f3c-26.1.2-sources.jar' 'net/minecraft/world/level/SpawnData.java'
```

Expected:
- `SpawnEggItem.useOn(...)` calls `Spawner.setEntityId(...)` before shrinking the egg.
- `SpawnerBlockEntity.getSpawner()` exposes the internal `BaseSpawner`.
- `BaseSpawner.serverTick(ServerLevel, BlockPos)` creates the entity before `ServerLevel.tryAddFreshEntityWithPassengers(entity)`.
- Vanilla `BaseSpawner.serverTick` performs at most one `tryAddFreshEntityWithPassengers(entity)` call per successful tick path. If this changes, the Task 6 thread-local plan must be revised before implementation.

- [ ] **Step 1.7: Update spike doc only if current APIs differ**

If any expected API differs, append a short `## V4 Implementation Recheck Findings` section to `docs/26.1.2-mojang-targets.md` and update later tasks before coding.

Expected: no doc change if the source still matches the existing V4 spike sections.

---

## Task 2: Add Exact Marked Spawn Egg Stack Factory

Create the single source of truth for marked `minecraft:iron_golem_spawn_egg` stacks and marker parsing.

**Files:**
- Create: `src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java`
- Create: `src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java`

- [ ] **Step 2.1: Write failing marker tests**

Create `SpawnEggStacksTest` with tests equivalent to:

```java
@Test
void markedStacksUseVanillaIronGolemSpawnEggAndExactCustomData() {
    for (GolemVariant variant : List.of(COPPER, GOLD, EMERALD, DIAMOND, NETHERITE)) {
        ItemStack stack = SpawnEggStacks.create(variant);
        assertTrue(stack.is(Items.IRON_GOLEM_SPAWN_EGG));
        assertEquals(variant, SpawnEggStacks.variantFrom(stack).orElseThrow());
        assertEquals("{multigolem:{variant:\"" + variant.id() + "\"}}", SpawnEggStacks.customDataSnbt(stack));
    }
}

@Test
void unmarkedIronGolemEggHasNoVariantMarker() {
    assertTrue(SpawnEggStacks.variantFrom(new ItemStack(Items.IRON_GOLEM_SPAWN_EGG)).isEmpty());
}

@Test
void factoryReturnsFreshCopies() {
    ItemStack first = SpawnEggStacks.create(GolemVariant.DIAMOND);
    ItemStack second = SpawnEggStacks.create(GolemVariant.DIAMOND);
    first.setCount(17);
    assertEquals(1, second.getCount());
    assertEquals(GolemVariant.DIAMOND, SpawnEggStacks.variantFrom(second).orElseThrow());
}

@Test
void manuallyMarkedIronVariantIsNotAV4Egg() {
    ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
    CompoundTag root = new CompoundTag();
    CompoundTag multigolem = new CompoundTag();
    multigolem.putString("variant", "iron");
    root.put("multigolem", multigolem);
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    assertTrue(SpawnEggStacks.variantFrom(stack).isEmpty());
}

@Test
void ironIsNotAMarkedV4EggVariant() {
    assertThrows(IllegalArgumentException.class, () -> SpawnEggStacks.create(GolemVariant.IRON));
}
```

Expected: fail because `SpawnEggStacks` does not exist.

- [ ] **Step 2.2: Run failing test**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest
```

Expected: compile failure for missing `SpawnEggStacks`.

- [ ] **Step 2.3: Implement `SpawnEggStacks`**

Create `SpawnEggStacks` with these responsibilities:

```java
public final class SpawnEggStacks {
    private static final String ROOT_KEY = MultiGolem.MOD_ID;
    private static final String VARIANT_KEY = "variant";

    private SpawnEggStacks() {}

    public static ItemStack create(GolemVariant variant) {
        if (variant == GolemVariant.IRON) {
            throw new IllegalArgumentException("Iron is vanilla-owned and has no marked V4 spawn egg");
        }
        ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(markerTag(variant)));
        return stack;
    }

    public static Optional<GolemVariant> variantFrom(ItemStack stack) {
        if (!stack.is(Items.IRON_GOLEM_SPAWN_EGG)) return Optional.empty();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return Optional.empty();
        CompoundTag root = data.copyTag();
        CompoundTag multigolem = root.getCompoundOrEmpty(ROOT_KEY);
        return GolemVariant.fromId(multigolem.getStringOrEmpty(VARIANT_KEY))
            .filter(variant -> variant != GolemVariant.IRON);
    }

    public static String customDataSnbt(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().toString();
    }

    private static CompoundTag markerTag(GolemVariant variant) {
        CompoundTag root = new CompoundTag();
        CompoundTag multigolem = new CompoundTag();
        multigolem.putString(VARIANT_KEY, variant.id());
        root.put(ROOT_KEY, multigolem);
        return root;
    }
}
```

Adjust only method names like `getCompoundOrEmpty` or `getStringOrEmpty` if Task 1 shows the 26.1.2 source uses different names.

- [ ] **Step 2.4: Run marker tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest
```

Expected: pass. Confirm test assertions prove the stack factory is the sole writer to `minecraft:custom_data` and that the whole component is exact.

- [ ] **Step 2.5: Guard against other V4 custom_data writers**

Run:

```powershell
rg -n "CUSTOM_DATA|custom_data|CustomData|putString\\(\"variant\"|multigolem" src
rg -n "IRON_GOLEM_SPAWN_EGG|DataComponents\\.CUSTOM_DATA|\\.set\\(DataComponents\\.CUSTOM_DATA" src/main/java src/client/java src/test/java
```

Expected: the only new writer for spawn egg item stack `minecraft:custom_data` is `SpawnEggStacks`. Other code may read the marker, but must not add extra keys to these stacks. No other site calls `.set(DataComponents.CUSTOM_DATA, ...)` or mutates `custom_data` on an `IRON_GOLEM_SPAWN_EGG` stack.

- [ ] **Step 2.6: Commit stack factory**

Run:

```powershell
git add src/test/java/dev/charles/multigolem/spawn/SpawnEggStacksTest.java src/main/java/dev/charles/multigolem/spawn/SpawnEggStacks.java
git commit -m "feat: add marked golem spawn egg stacks"
```

Expected: commit succeeds.

---

## Task 3: Normal Marked Egg Use

Hook normal egg use through `SpawnEggItem.spawnMob`, keep unmarked eggs vanilla, and reuse V3.1 creation permissions.

**Files:**
- Create: `src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java`
- Create: `src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java`
- Modify: `src/main/resources/multigolem.mixins.json`

- [ ] **Step 3.1: Implement normal spawn helper**

Create `SpawnEggVariantSpawner`:

```java
public final class SpawnEggVariantSpawner {
    private SpawnEggVariantSpawner() {}

    @Nullable
    public static Entity spawnMarkedOrVanilla(
        EntityType<?> type,
        ServerLevel level,
        ItemStack stack,
        @Nullable LivingEntity user,
        BlockPos pos,
        EntitySpawnReason reason,
        boolean alignPosition,
        boolean invertY
    ) {
        Optional<GolemVariant> variant = SpawnEggStacks.variantFrom(stack);
        if (variant.isEmpty()) {
            return type.spawn(level, stack, user, pos, reason, alignPosition, invertY);
        }

        if (type != EntityType.IRON_GOLEM) {
            return type.spawn(level, stack, user, pos, reason, alignPosition, invertY);
        }

        if (user instanceof ServerPlayer player && !MultiGolemPermissions.canCreate(player, variant.get())) {
            MultiGolemPermissions.sendCreateDenied(player, variant.get());
            return null;
        }

        Entity spawned = type.spawn(level, stack, user, pos, reason, alignPosition, invertY);
        if (spawned instanceof IronGolem golem) {
            GolemVariantAttachment.set(golem, variant.get());
            VariantAttributes.apply(golem);
        }
        return spawned;
    }
}
```

Do not call `setPlayerCreated(true)`. Egg-spawned variants are not player-created; only T-pattern creation sets that flag.

- [ ] **Step 3.2: Add `SpawnEggItem.spawnMob` redirect**

Create `SpawnEggItemMixin`:

```java
@Mixin(SpawnEggItem.class)
public abstract class SpawnEggItemMixin {
    @Redirect(
        method = "spawnMob(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"
        )
    )
    private Entity multigolem$spawnMarkedVariantOrVanilla(
        EntityType<?> type,
        ServerLevel level,
        ItemStack stack,
        LivingEntity user,
        BlockPos pos,
        EntitySpawnReason reason,
        boolean alignPosition,
        boolean invertY
    ) {
        return SpawnEggVariantSpawner.spawnMarkedOrVanilla(type, level, stack, user, pos, reason, alignPosition, invertY);
    }
}
```

If Task 1 shows `spawnMob` returns a different type or has a different descriptor, update the descriptor and leave the redirect scope narrow.

- [ ] **Step 3.3: Register mixin**

Add `"SpawnEggItemMixin"` to `src/main/resources/multigolem.mixins.json`.

- [ ] **Step 3.4: Build and run focused tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest
.\gradlew.bat --quiet compileJava
```

Expected: tests and compile pass.

- [ ] **Step 3.5: Manual normal egg verification**

In a modded integrated world or local dedicated server, verify:

```text
/give @p minecraft:iron_golem_spawn_egg
/give @p minecraft:iron_golem_spawn_egg[minecraft:custom_data={multigolem:{variant:"copper"}}]
/give @p minecraft:iron_golem_spawn_egg[minecraft:custom_data={multigolem:{variant:"gold"}}]
/give @p minecraft:iron_golem_spawn_egg[minecraft:custom_data={multigolem:{variant:"emerald"}}]
/give @p minecraft:iron_golem_spawn_egg[minecraft:custom_data={multigolem:{variant:"diamond"}}]
/give @p minecraft:iron_golem_spawn_egg[minecraft:custom_data={multigolem:{variant:"netherite"}}]
```

Expected:
- unmarked iron golem egg spawns an unmarked vanilla-owned iron golem;
- each marked egg spawns the correct variant attachment and stats;
- denied marked egg use with `multigolem.create.<variant>` denied sends overlay denial, spawns nothing, and does not consume the egg;
- denied marked egg use may still arm-swing, and this cosmetic artifact is accepted;
- egg-spawned variants are not player-created.

- [ ] **Step 3.6: Commit normal egg hook**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java src/main/resources/multigolem.mixins.json
git commit -m "feat: spawn marked golem eggs"
```

Expected: commit succeeds.

---

## Task 4: Spawn Egg Item Models And Textures

Add client visuals using exact `minecraft:custom_data` selection while preserving vanilla fallback.

**Files:**
- Modify: `scripts/generate-textures.py`
- Modify: `scripts/test-generate-textures.py`
- Create: `src/main/resources/assets/minecraft/items/iron_golem_spawn_egg.json`
- Create: `src/main/resources/assets/multigolem/models/item/copper_golem_spawn_egg.json`
- Create: `src/main/resources/assets/multigolem/models/item/gold_golem_spawn_egg.json`
- Create: `src/main/resources/assets/multigolem/models/item/emerald_golem_spawn_egg.json`
- Create: `src/main/resources/assets/multigolem/models/item/diamond_golem_spawn_egg.json`
- Create: `src/main/resources/assets/multigolem/models/item/netherite_golem_spawn_egg.json`
- Create/update generated textures under `src/main/resources/assets/multigolem/textures/item/`
- Create if using a committed vanilla item template: `build-inputs/textures/spawn_egg/LICENSE-AND-PROVENANCE.md`

- [ ] **Step 4.1: Extend texture generator for spawn egg icons**

Update `scripts/generate-textures.py` so `main()` writes existing entity textures and five item textures:

```python
SPAWN_EGG_OUT_DIR = REPO / "src" / "main" / "resources" / "assets" / "multigolem" / "textures" / "item"

def generate_spawn_egg_icon(tier: str, params: dict[str, float]) -> Image.Image:
    base = Image.open(SPAWN_EGG_TEMPLATE).convert("RGBA")
    img = shift_hue(base.copy(), params["hue_shift"], params["saturation"], params["lightness"])
    return apply_spawn_egg_material_details(tier, img)
```

If the implementation copies Mojang's vanilla `assets/minecraft/textures/item/iron_golem_spawn_egg.png` into `build-inputs`, add `build-inputs/textures/spawn_egg/LICENSE-AND-PROVENANCE.md` with:

```markdown
# Spawn Egg Texture Template Provenance

- Source jar: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-a26c9a9f3c/26.1.2/minecraft-clientOnly-a26c9a9f3c-26.1.2.jar`
- Source entry: `assets/minecraft/textures/item/iron_golem_spawn_egg.png`
- SHA-256: `6f269e010c8cf5d3657ede43a8c5d79df4df5d3b77bdd6e0a0bd079f4216f405`
- License note: this template is a Mojang vanilla game asset used only as a local build input under the Minecraft EULA; generated MultiGolem variant textures are the shipped mod assets.
```

Do not use the vanilla CopperGolem spawn egg art for the MultiGolem copper variant.

- [ ] **Step 4.2: Extend texture tests**

Update `scripts/test-generate-textures.py` so deterministic hash and material-identity checks include:

```python
GENERATED_SPAWN_EGG_TEXTURES = (
    "copper_golem_spawn_egg.png",
    "gold_golem_spawn_egg.png",
    "emerald_golem_spawn_egg.png",
    "diamond_golem_spawn_egg.png",
    "netherite_golem_spawn_egg.png",
)
```

Expected assertions:
- all five spawn egg textures are generated deterministically;
- copper has copper/orange pixels plus distinct patina pixels;
- gold is bright gold;
- emerald is green;
- diamond is blue-green and not olive/dirty;
- netherite is dark with visible warm crack pixels.

- [ ] **Step 4.3: Add generated item models**

Create one model per tier:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "multigolem:item/diamond_golem_spawn_egg"
  }
}
```

Use matching `layer0` paths for copper, gold, emerald, diamond, and netherite.

- [ ] **Step 4.4: Override vanilla iron golem egg item definition**

Create `src/main/resources/assets/minecraft/items/iron_golem_spawn_egg.json`:

```json
{
  "model": {
    "type": "minecraft:select",
    "property": "minecraft:component",
    "component": "minecraft:custom_data",
    "cases": [
      {
        "when": "{multigolem:{variant:\"copper\"}}",
        "model": { "type": "minecraft:model", "model": "multigolem:item/copper_golem_spawn_egg" }
      },
      {
        "when": "{multigolem:{variant:\"gold\"}}",
        "model": { "type": "minecraft:model", "model": "multigolem:item/gold_golem_spawn_egg" }
      },
      {
        "when": "{multigolem:{variant:\"emerald\"}}",
        "model": { "type": "minecraft:model", "model": "multigolem:item/emerald_golem_spawn_egg" }
      },
      {
        "when": "{multigolem:{variant:\"diamond\"}}",
        "model": { "type": "minecraft:model", "model": "multigolem:item/diamond_golem_spawn_egg" }
      },
      {
        "when": "{multigolem:{variant:\"netherite\"}}",
        "model": { "type": "minecraft:model", "model": "multigolem:item/netherite_golem_spawn_egg" }
      }
    ],
    "fallback": {
      "type": "minecraft:model",
      "model": "minecraft:item/iron_golem_spawn_egg"
    }
  }
}
```

The fallback must remain `minecraft:item/iron_golem_spawn_egg` so unmarked stacks and vanilla clients have acceptable behavior.

- [ ] **Step 4.5: Run texture and resource verification**

Run:

```powershell
python scripts\test-generate-textures.py
.\gradlew.bat --quiet processResources
.\gradlew.bat --quiet build
```

Expected: texture tests, resource processing, and build pass.

- [ ] **Step 4.6: Manual exact model selection verification**

In a modded client, verify:
- exact marker `{multigolem:{variant:"diamond"}}` shows the Diamond Golem Spawn Egg icon;
- unmarked iron golem spawn egg uses the vanilla icon;
- marker plus any extra custom data key still matches the variant icon in 26.1.2 because `CustomData.matchedBy(...)` uses subset-style compound matching;
- this subset-match behavior is documented for future versions: the stack factory must remain the sole writer to keep V4-authored stacks exact, and if future Minecraft versions switch to whole-compound matching, extra keys will require updated item model cases or `minecraft:custom_model_data`.

- [ ] **Step 4.7: Commit visuals**

Run:

```powershell
git add scripts/generate-textures.py scripts/test-generate-textures.py src/main/resources/assets/minecraft/items/iron_golem_spawn_egg.json src/main/resources/assets/multigolem/models/item src/main/resources/assets/multigolem/textures/item build-inputs/textures/spawn_egg/LICENSE-AND-PROVENANCE.md
git commit -m "feat: add variant spawn egg icons"
```

If no provenance file was needed, omit it from `git add`.

Expected: commit succeeds.

---

## Task 5: Creative Tab Entries

Add the five marked stacks to the vanilla spawn eggs tab with the narrowest registration side that works for modded clients on a modded dedicated server.

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/MultiGolem.java` or `src/client/java/dev/charles/multigolem/client/MultiGolemClient.java`

- [ ] **Step 5.1: Add creative tab registration**

Start with the narrowest side verified by Task 1. If common registration is required for dedicated-server tab population, add this to `MultiGolem.onInitialize()`:

```java
private static void registerCreativeSpawnEggs() {
    ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
        entries.accept(SpawnEggStacks.create(GolemVariant.COPPER));
        entries.accept(SpawnEggStacks.create(GolemVariant.GOLD));
        entries.accept(SpawnEggStacks.create(GolemVariant.EMERALD));
        entries.accept(SpawnEggStacks.create(GolemVariant.DIAMOND));
        entries.accept(SpawnEggStacks.create(GolemVariant.NETHERITE));
    });
}
```

If client registration is sufficient for integrated and modded dedicated-server clients, place the same event registration in `MultiGolemClient.onInitializeClient()`.

Do not create a new creative tab. Do not register new items. Do not introduce registry-sync-breaking entries.

- [ ] **Step 5.2: Compile**

Run:

```powershell
.\gradlew.bat --quiet compileJava
```

Expected: compile passes.

- [ ] **Step 5.3: Integrated client verification**

Start a modded integrated client and verify the vanilla Spawn Eggs tab contains:

```text
Copper Golem Spawn Egg
Gold Golem Spawn Egg
Emerald Golem Spawn Egg
Diamond Golem Spawn Egg
Netherite Golem Spawn Egg
```

Expected: each entry is a marked `minecraft:iron_golem_spawn_egg` stack and spawns the matching variant.

- [ ] **Step 5.4: Modded dedicated server verification**

Start a modded dedicated server and connect with a modded client.

Expected:
- the five marked entries appear to the modded client in `CreativeModeTabs.SPAWN_EGGS`;
- using each entry on the server spawns the matching variant;
- denied permissions on the server block marked egg use and do not consume the egg.

- [ ] **Step 5.5: Vanilla client fallback verification**

Connect a vanilla client to the modded dedicated server.

Expected:
- no registry-sync-breaking entries are introduced;
- the vanilla client is not required to know any new item ID, entity type, creative tab, or data component;
- if the vanilla client sees or receives a marked egg stack, it is still a vanilla iron golem spawn egg with acceptable vanilla icon fallback.

- [ ] **Step 5.6: Commit creative tab registration**

Run:

```powershell
git add src/main/java/dev/charles/multigolem/MultiGolem.java src/client/java/dev/charles/multigolem/client/MultiGolemClient.java
git commit -m "feat: add variant eggs to spawn eggs tab"
```

Stage only the file actually modified.

Expected: commit succeeds.

---

## Task 6: Spawner Support (Final Isolated, Cuttable To V4.1)

Make marked eggs configure spawners as variant iron golem spawners. If this task becomes brittle, cut it to V4.1 without blocking normal V4 eggs.

**Files:**
- Create: `src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java`
- Create: `src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java`
- Create: `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerAccessor.java`
- Create: `src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java`
- Modify: `src/main/resources/multigolem.mixins.json`

- [ ] **Step 6.1: Write marker tests**

Create `SpawnerVariantMarkerTest` with pure `CompoundTag` tests:

```java
@Test
void writesMarkerIntoSpawnerEntityTagWithoutChangingEntityId() {
    CompoundTag entity = new CompoundTag();
    entity.putString("id", "minecraft:iron_golem");
    SpawnerVariantMarker.write(entity, GolemVariant.DIAMOND);
    assertEquals("minecraft:iron_golem", entity.getStringOrEmpty("id"));
    assertEquals(GolemVariant.DIAMOND, SpawnerVariantMarker.read(entity).orElseThrow());
}

@Test
void unmarkedSpawnerEntityTagReadsEmpty() {
    CompoundTag entity = new CompoundTag();
    entity.putString("id", "minecraft:iron_golem");
    assertTrue(SpawnerVariantMarker.read(entity).isEmpty());
}

@Test
void clearRemovesOnlyMultiGolemMarker() {
    CompoundTag entity = new CompoundTag();
    entity.putString("id", "minecraft:iron_golem");
    entity.putString("CustomName", "\"Bob\"");
    SpawnerVariantMarker.write(entity, GolemVariant.GOLD);
    SpawnerVariantMarker.clear(entity);
    assertEquals("\"Bob\"", entity.getStringOrEmpty("CustomName"));
    assertTrue(SpawnerVariantMarker.read(entity).isEmpty());
}
```

- [ ] **Step 6.2: Implement `SpawnerVariantMarker`**

Create helper methods:

```java
public static void write(CompoundTag entityTag, GolemVariant variant) {
    CompoundTag multigolem = new CompoundTag();
    multigolem.putString("variant", variant.id());
    entityTag.put("multigolem", multigolem);
}

public static Optional<GolemVariant> read(CompoundTag entityTag) {
    CompoundTag multigolem = entityTag.getCompoundOrEmpty("multigolem");
    return GolemVariant.fromId(multigolem.getStringOrEmpty("variant"))
        .filter(variant -> variant != GolemVariant.IRON);
}
```

This marker lives in `SpawnData.entity` for future spawner-spawned entities. It does not add keys to the egg stack `minecraft:custom_data`.

- [ ] **Step 6.3: Run marker tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
```

Expected: pass.

- [ ] **Step 6.4: Add spawner configuration permission check before mutation**

In `SpawnEggItemMixin`, inject into `useOn(UseOnContext)` before the `Spawner.setEntityId(...)` invocation:

```java
@Inject(
    method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Spawner;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V"
    ),
    cancellable = true
)
private void multigolem$denyMarkedSpawnerUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
    Optional<GolemVariant> variant = SpawnEggStacks.variantFrom(context.getItemInHand());
    if (variant.isEmpty()) return;
    if (context.getPlayer() == null) {
        cir.setReturnValue(InteractionResult.FAIL);
        return;
    }
    if (context.getPlayer() instanceof ServerPlayer player && !MultiGolemPermissions.canCreate(player, variant.get())) {
        MultiGolemPermissions.sendCreateDenied(player, variant.get());
        cir.setReturnValue(InteractionResult.FAIL);
    }
}
```

Expected: denied spawner configuration cancels before `Spawner.setEntityId(...)`, leaves the spawner unchanged, and does not consume the egg. A marked egg used on a spawner without a player context is denied because V4's spawner configuration permission is checked at configuration time and there is no responsible player to authorize.

- [ ] **Step 6.5: Define accessor for the existing spawner data**

Create `BaseSpawnerAccessor` as a field accessor for the already-selected `nextSpawnData`; do not expose or call `getOrCreateNextSpawnData(...)` from the marker-write hook.

```java
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
    @Accessor("nextSpawnData")
    SpawnData multigolem$getNextSpawnData();
}
```

Expected: the marker-write hook mutates the existing `SpawnData` object that vanilla just updated through `Spawner.setEntityId(...)`. It must never create a new `SpawnData` that could be stale or discarded.

- [ ] **Step 6.6: Add marker write after vanilla `setEntityId`**

Add a second inject after the same invoke:

```java
@Inject(
    method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Spawner;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V",
        shift = At.Shift.AFTER
    )
)
private void multigolem$markSpawnerSpawnData(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
    Optional<GolemVariant> variant = SpawnEggStacks.variantFrom(context.getItemInHand());
    if (variant.isEmpty()) return;
    if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SpawnerBlockEntity blockEntity)) return;
    BaseSpawner spawner = blockEntity.getSpawner();
    SpawnData data = ((BaseSpawnerAccessor) spawner).multigolem$getNextSpawnData();
    if (data == null) return;
    SpawnerVariantMarker.write(data.getEntityToSpawn(), variant.get());
}
```

Document beside this hook that permission check, vanilla `setEntityId`, marker write, and later save all run on the same server thread inside one `useOn` call; there is no async save window between the injects.

- [ ] **Step 6.7: Add no-dependency `BaseSpawner.serverTick` Path A**

Use no Mixin Extras dependency. Add a thread-local stash in `BaseSpawnerMixin`:

```java
private static final ThreadLocal<Optional<GolemVariant>> MULTIGOLEM_SPAWNER_VARIANT =
    ThreadLocal.withInitial(Optional::empty);
```

Inject early enough in `serverTick(ServerLevel, BlockPos)` to read the current `SpawnData.entity` marker and stash the variant for this spawn attempt.

Before coding the inject, cite the Task 1 source check that vanilla `BaseSpawner.serverTick` only attempts one entity add per successful tick path. The `finally` cleanup below is correct only under that one entity per tick vanilla shape; if a future Minecraft version loops over multiple `tryAddFreshEntityWithPassengers` calls in one tick, replace the thread-local stash with a per-entity or local-capture approach before shipping.

Redirect `ServerLevel.tryAddFreshEntityWithPassengers(entity)`:

```java
@Redirect(
    method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
    at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
    )
)
private boolean multigolem$applyVariantBeforeSpawnerAdd(ServerLevel level, Entity entity) {
    try {
        Optional<GolemVariant> variant = MULTIGOLEM_SPAWNER_VARIANT.get();
        if (variant.isPresent() && entity instanceof IronGolem golem) {
            GolemVariantAttachment.set(golem, variant.get());
            VariantAttributes.apply(golem);
        }
        return level.tryAddFreshEntityWithPassengers(entity);
    } finally {
        MULTIGOLEM_SPAWNER_VARIANT.remove();
    }
}
```

Do not call `setPlayerCreated(true)`. Spawner-spawned golems remain non-player-created.

- [ ] **Step 6.8: Register spawner mixins**

Add the spawner mixins/accessors to `src/main/resources/multigolem.mixins.json`.

- [ ] **Step 6.9: Build and startup smoke**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
.\gradlew.bat --quiet build
```

Then start a local server.

Expected:
- no required mixin apply errors;
- if the redirect target is brittle or local marker stashing cannot be proven cleanly, stop and cut this task to V4.1 rather than adding Mixin Extras immediately.

- [ ] **Step 6.10: Manual spawner verification**

Verify on a modded server:
- denied marked egg use on a spawner checks `multigolem.create.<variant>`, sends overlay denial, leaves the spawner unchanged, and does not consume the egg;
- allowed marked egg use lets vanilla set the spawner entity ID to iron golem, then writes the MultiGolem marker into `SpawnData.entity`;
- spawner-spawned variants get `GolemVariantAttachment` and variant attributes before entering the world;
- spawner-spawned variants are not player-created;
- thread-local cleanup cannot leak after successful spawn, failed spawn, or exception path because cleanup happens in `finally`.

- [ ] **Step 6.11: Commit spawner support**

Run:

```powershell
git add src/test/java/dev/charles/multigolem/spawn/SpawnerVariantMarkerTest.java src/main/java/dev/charles/multigolem/spawn/SpawnerVariantMarker.java src/main/java/dev/charles/multigolem/mixin/BaseSpawnerAccessor.java src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java src/main/resources/multigolem.mixins.json
git commit -m "feat: support marked eggs on spawners"
```

Expected: commit succeeds. If cut to V4.1, do not commit partial spawner code.

---

## Task 7: Docs, Changelog, And Playtest Checklist

Document V4 behavior for players/server admins and preserve release boundaries.

**Files:**
- Modify: `CHANGELOG.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`
- Optional modify if nearby V4 wording exists: `README.md`, `docs/modrinth-listing.md`, `docs/curseforge-listing.md`

- [ ] **Step 7.1: Add player/admin changelog**

Under `## Unreleased` in `CHANGELOG.md`, add:

```markdown
- Added Copper, Gold, Emerald, Diamond, and Netherite Golem Spawn Egg variants as marked vanilla iron golem spawn eggs.
- Server permissions for creating each MultiGolem tier now also apply to marked spawn egg use.
- Vanilla iron golem spawn eggs remain unchanged when they are not marked as a MultiGolem variant.
```

If spawner support shipped in Task 6, add:

```markdown
- Marked MultiGolem spawn eggs can configure monster spawners to spawn the matching MultiGolem variant.
```

- [ ] **Step 7.2: Add V4 playtest rows**

Add a V4 section to `docs/playtest-checklist.md` covering:

```markdown
## V4 - marked spawn eggs

- [ ] Unmarked vanilla iron golem spawn egg still spawns a vanilla-owned iron golem.
- [ ] Copper Golem Spawn Egg spawns a copper MultiGolem `IronGolem`.
- [ ] Gold Golem Spawn Egg spawns a gold MultiGolem `IronGolem`.
- [ ] Emerald Golem Spawn Egg spawns an emerald MultiGolem `IronGolem`.
- [ ] Diamond Golem Spawn Egg spawns a diamond MultiGolem `IronGolem`.
- [ ] Netherite Golem Spawn Egg spawns a netherite MultiGolem `IronGolem`.
- [ ] Denied normal marked egg use does not consume the egg.
- [ ] Denied normal marked egg use may arm-swing; overlay denial feedback appears and no golem spawns.
- [ ] Egg-spawned MultiGolem variants are not player-created.
- [ ] Creative tab entries appear in an integrated modded client.
- [ ] Creative tab entries appear for a modded client connected to a modded dedicated server.
- [ ] Vanilla client fallback behavior is acceptable and no registry-sync-breaking entries are introduced.
- [ ] Vanilla client shows the vanilla iron golem spawn egg icon for marked stacks with no missing-texture error.
- [ ] Exact `minecraft:custom_data` model selection works for every variant.
- [ ] The stack factory is the sole writer to spawn egg `minecraft:custom_data`.
- [ ] If shipped, denied spawner configuration leaves the spawner unchanged and does not consume the egg.
- [ ] If shipped, spawner-spawned variants get attachment/stats and are not player-created.
- [ ] If shipped, spawner thread-local cleanup cannot leak across spawn attempts.
```

Add matching labels to `docs/playtest.html` without breaking the existing local-storage progress script.

- [ ] **Step 7.3: Run docs gates**

Run:

```powershell
python scripts\check-changelog-style.py --section Unreleased
.\gradlew.bat --quiet checkChangelog checkReleaseNotesStyle
```

Expected: pass.

- [ ] **Step 7.4: Commit docs**

Run:

```powershell
git add CHANGELOG.md docs/playtest-checklist.md docs/playtest.html README.md docs/modrinth-listing.md docs/curseforge-listing.md
git commit -m "docs: document V4 spawn eggs"
```

Stage only files actually modified.

Expected: commit succeeds.

---

## Task 8: Final Verification

Run the full verification set before declaring V4 implementation complete.

**Files:**
- No expected edits unless verification finds a focused issue.

- [ ] **Step 8.1: Static scope scan**

Run:

```powershell
rg -n "Registry\\.register|DataComponentType|EntityType\\.Builder|CreativeModeTab\\.builder|Recipe|custom_model_data|setPlayerCreated\\(true\\)|LuckPerms" src
rg -n "CUSTOM_DATA|custom_data|CustomData|multigolem" src/main/java src/test/java src/main/resources/assets/minecraft/items/iron_golem_spawn_egg.json
rg -n "IRON_GOLEM_SPAWN_EGG|DataComponents\\.CUSTOM_DATA|\\.set\\(DataComponents\\.CUSTOM_DATA" src/main/java src/client/java src/test/java
```

Expected:
- no new item IDs, data component types, entity types, creative tabs, or recipes;
- no direct LuckPerms APIs;
- no `custom_model_data` fallback in V4 unless the plan is explicitly revised;
- `setPlayerCreated(true)` remains only in T-pattern creation;
- `SpawnEggStacks` is the sole writer to spawn egg stack `minecraft:custom_data`.

- [ ] **Step 8.2: Focused unit tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnEggStacksTest
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.SpawnerVariantMarkerTest
python scripts\test-generate-textures.py
```

Expected: pass. If spawner support was cut to V4.1, omit `SpawnerVariantMarkerTest`.

- [ ] **Step 8.3: Full build**

Run:

```powershell
.\gradlew.bat --quiet build
```

Expected: pass, including changelog/style/check companion gates and texture generation.

- [ ] **Step 8.4: Manual game verification matrix**

Verify all required scenarios:
- unmarked iron golem egg stays vanilla;
- each marked egg spawns the correct variant;
- denied normal egg use does not consume the egg;
- denied normal egg use may arm-swing and this is accepted;
- egg-spawned variants are not player-created;
- creative tab entries appear in integrated and modded-dedicated-server contexts;
- vanilla client fallback behavior is acceptable;
- exact `custom_data` model selection works;
- stack factory is sole writer to `custom_data`;
- spawner configuration permission behavior works if Task 6 shipped;
- spawner-spawned variants get attachment/stats and are not player-created if Task 6 shipped;
- spawner thread-local cleanup cannot leak if Task 6 shipped;
- no registry-sync-breaking entries are introduced.

- [ ] **Step 8.5: V4 release boundary**

Do not start release prep in this implementation phase. Tyler does not want a release until V4 is done, and release prep requires showing Tyler the exact versioned public `CHANGELOG.md` section before publishing or tagging.

---

## Self-Review Checklist

- Plan adds five marked vanilla `minecraft:iron_golem_spawn_egg` stacks and no new item IDs.
- Plan does not add a new `DataComponentType`, entity type, creative tab, or recipe.
- `minecraft:custom_data` under `multigolem.variant` is the authoritative marker.
- Stack factory is the sole writer to spawn egg stack `minecraft:custom_data`.
- Item model selection uses `minecraft:component` on `minecraft:custom_data` with vanilla fallback; 26.1.2 matches expected marker compounds as subsets, so V4-authored stacks stay exact through the stack factory.
- Unmarked vanilla iron golem spawn eggs remain vanilla-owned.
- Normal egg use hooks `SpawnEggItem.spawnMob` via narrow redirect of `EntityType.spawn(...)`.
- Permission denial reuses `multigolem.create.<variant>` and returns `null` before spawn/consume.
- Plan documents accepted denial arm-swing artifact.
- Egg-spawned and spawner-spawned variants do not call `setPlayerCreated(true)`.
- Creative tab verification covers integrated client, modded dedicated server, and vanilla client fallback.
- Spawner support is final, isolated, and cuttable to V4.1.
- Spawner denial happens before `Spawner.setEntityId(...)`, leaves the spawner unchanged, and consumes nothing.
- Spawner marker write and save ordering note says the permission check, vanilla setEntityId, marker write, and later save run on the same server thread.
- Spawner thread-local cleanup uses `finally`.
- Verification includes no registry-sync-breaking entries.
