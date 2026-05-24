# MultiGolem V4.1 Netherite Village Ignite Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Netherite golem ignite-on-hit duration configurable separately for village-spawned Netherite golems, so villages can opt into Netherite defenders without necessarily opting into village fire spread.

**Architecture:** Add a small persistent spawn-origin attachment for MultiGolem iron golems, set it only when a villager natural spawn roll applies a variant, and resolve Netherite ignite seconds through an origin-aware helper. Keep the existing `netherite_ignite_seconds` field as the base value for player-built, spawn egg, spawner, command, and legacy/unknown-origin Netherite golems; add a new Netherite-only `netherite_village_ignite_seconds` override that defaults to `0`.

**Tech Stack:** Java 25 · Fabric Loader 0.19.2 · Fabric API attachment API · Minecraft 26.1.2 · Mixin · JUnit 5 · JSON config migration with Gson

---

## Design Notes

This refinement exists because V4 made Netherite village spawning opt-in by defaulting the village spawn weight to `0`, but a server owner can still set a positive Netherite village weight. That may be desirable for combat-heavy servers, custom villages, arenas, or players who enjoy the extra hazard. The dangerous part is not the village spawn itself; it is that Netherite golems currently use the same ignite-on-hit duration regardless of spawn source.

V4.1 should let admins express this policy directly:

```json
"tiers": {
  "netherite": {
    "netherite_ignite_seconds": 2,
    "netherite_village_ignite_seconds": 0
  }
}
```

Under that config, player-built, spawn egg, spawner, command, and legacy/unknown-origin Netherite golems ignite targets for `2` seconds. Village-spawned Netherite golems do not ignite targets.

Do not use `IronGolem.isPlayerCreated()` as the origin model. Spawn eggs and spawners are intentionally not player-created, and command-spawned golems are also not player-created. V4.1 only needs to identify the village-spawned case; every other origin can keep using the base Netherite setting.

---

## File Structure

**New Java files:**

```text
src/main/java/dev/charles/multigolem/attachment/GolemSpawnOrigin.java
src/main/java/dev/charles/multigolem/attachment/GolemSpawnOriginAttachment.java
src/test/java/dev/charles/multigolem/attachment/GolemSpawnOriginTest.java
```

**Modified Java files:**

```text
src/main/java/dev/charles/multigolem/MultiGolem.java
src/main/java/dev/charles/multigolem/config/TierStats.java
src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java
src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java
src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java
src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java
src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java
src/test/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandlerTest.java
src/test/java/dev/charles/multigolem/stats/GolemStatsResolverTest.java
```

**Modified docs:**

```text
README.md
docs/playtest-checklist.md
docs/playtest.html
CHANGELOG.md
```

---

## Task 1: Add A Minimal Spawn Origin Attachment

**Files:**
- Create: `src/main/java/dev/charles/multigolem/attachment/GolemSpawnOrigin.java`
- Create: `src/main/java/dev/charles/multigolem/attachment/GolemSpawnOriginAttachment.java`
- Create: `src/test/java/dev/charles/multigolem/attachment/GolemSpawnOriginTest.java`
- Modify: `src/main/java/dev/charles/multigolem/MultiGolem.java`

- [ ] **Step 1.1: Write the failing origin enum test**

Create `GolemSpawnOriginTest`:

```java
package dev.charles.multigolem.attachment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemSpawnOriginTest {

    @Test
    void parsesKnownOriginIds() {
        assertEquals(GolemSpawnOrigin.UNKNOWN, GolemSpawnOrigin.fromId("unknown").orElseThrow());
        assertEquals(GolemSpawnOrigin.VILLAGE, GolemSpawnOrigin.fromId("village").orElseThrow());
    }

    @Test
    void rejectsUnknownOriginIds() {
        assertTrue(GolemSpawnOrigin.fromId("").isEmpty());
        assertTrue(GolemSpawnOrigin.fromId("spawn_egg").isEmpty());
    }
}
```

- [ ] **Step 1.2: Run the failing origin test**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.attachment.GolemSpawnOriginTest
```

Expected: compile failure because `GolemSpawnOrigin` does not exist.

- [ ] **Step 1.3: Add `GolemSpawnOrigin`**

Create:

```java
package dev.charles.multigolem.attachment;

import com.mojang.serialization.Codec;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GolemSpawnOrigin {
    UNKNOWN("unknown"),
    VILLAGE("village");

    public static final Codec<GolemSpawnOrigin> CODEC = Codec.STRING.flatXmap(
        id -> fromId(id)
            .map(com.mojang.serialization.DataResult::success)
            .orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "Unknown GolemSpawnOrigin id: " + id)),
        origin -> com.mojang.serialization.DataResult.success(origin.id)
    );

    private static final Map<String, GolemSpawnOrigin> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(GolemSpawnOrigin::id, Function.identity()));

    private final String id;

    GolemSpawnOrigin(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<GolemSpawnOrigin> fromId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }
}
```

- [ ] **Step 1.4: Add `GolemSpawnOriginAttachment`**

Create:

```java
package dev.charles.multigolem.attachment;

import dev.charles.multigolem.MultiGolem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class GolemSpawnOriginAttachment {

    public static final AttachmentType<GolemSpawnOrigin> TYPE = AttachmentRegistry
        .<GolemSpawnOrigin>builder()
        .persistent(GolemSpawnOrigin.CODEC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "spawn_origin"));

    private GolemSpawnOriginAttachment() {}

    public static GolemSpawnOrigin get(Entity entity) {
        GolemSpawnOrigin attached = entity.getAttached(TYPE);
        return attached != null ? attached : GolemSpawnOrigin.UNKNOWN;
    }

    public static Optional<GolemSpawnOrigin> getRaw(Entity entity) {
        return Optional.ofNullable(entity.getAttached(TYPE));
    }

    public static void set(Entity entity, GolemSpawnOrigin origin) {
        if (origin == GolemSpawnOrigin.UNKNOWN) {
            entity.removeAttached(TYPE);
            return;
        }
        entity.setAttached(TYPE, origin);
    }

    public static void touch() {
        // Calling this from MultiGolem.onInitialize forces class load and TYPE registration.
    }
}
```

- [ ] **Step 1.5: Register the attachment on mod init**

In `MultiGolem.onInitialize()`, call:

```java
GolemSpawnOriginAttachment.touch();
```

Place it next to `GolemVariantAttachment.touch()` and `GolemAbilityStateAttachment.touch()`.

- [ ] **Step 1.6: Run the origin tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.attachment.GolemSpawnOriginTest
```

Expected: pass.

---

## Task 2: Mark Village-Rolled MultiGolem Variants

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java`
- Create: `src/test/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandlerTest.java`

- [ ] **Step 2.1: Write the failing handler test**

Create `VillageGolemSpawnHandlerTest`:

```java
package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.attachment.GolemSpawnOriginAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VillageGolemSpawnHandlerTest {

    @Test
    void applyVariantMarksVillageOriginForNonIronVariant() throws ReflectiveOperationException {
        IronGolem golem = mock(IronGolem.class);
        when(golem.getMaxHealth()).thenReturn(100.0F);

        try (MockedStatic<GolemVariantAttachment> variants = mockStatic(GolemVariantAttachment.class);
             MockedStatic<GolemSpawnOriginAttachment> origins = mockStatic(GolemSpawnOriginAttachment.class)) {
            invokeApplyVariant(golem, GolemVariant.NETHERITE);

            variants.verify(() -> GolemVariantAttachment.set(golem, GolemVariant.NETHERITE));
            origins.verify(() -> GolemSpawnOriginAttachment.set(golem, GolemSpawnOrigin.VILLAGE));
        }
        verify(golem).setHealth(100.0F);
    }

    @Test
    void applyVariantDoesNotMarkIronOrigin() throws ReflectiveOperationException {
        IronGolem golem = mock(IronGolem.class);

        try (MockedStatic<GolemVariantAttachment> variants = mockStatic(GolemVariantAttachment.class);
             MockedStatic<GolemSpawnOriginAttachment> origins = mockStatic(GolemSpawnOriginAttachment.class)) {
            invokeApplyVariant(golem, GolemVariant.IRON);

            variants.verifyNoInteractions();
            origins.verifyNoInteractions();
        }
        verifyNoInteractions(golem);
    }

    private static void invokeApplyVariant(IronGolem golem, GolemVariant variant) throws ReflectiveOperationException {
        Method method = VillageGolemSpawnHandler.class.getDeclaredMethod("applyVariant", IronGolem.class, GolemVariant.class);
        method.setAccessible(true);
        method.invoke(null, golem, variant);
    }
}
```

- [ ] **Step 2.2: Run the failing handler test**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest
```

Expected: fail with Mockito reporting `GolemSpawnOriginAttachment.set(golem, GolemSpawnOrigin.VILLAGE)` was wanted but not invoked.

- [ ] **Step 2.3: Set the origin when applying a village variant**

In `VillageGolemSpawnHandler.applyVariant(...)`, set the origin immediately after setting the variant:

```java
GolemVariantAttachment.set(golem, variant);
GolemSpawnOriginAttachment.set(golem, GolemSpawnOrigin.VILLAGE);
golem.setHealth(golem.getMaxHealth());
```

Keep the origin marker only in the branch that actually applies a non-iron village variant. Do not mark iron/no-roll village golems; they remain vanilla iron golems.

- [ ] **Step 2.4: Run compile and village handler/resolver tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageGolemSpawnHandlerTest
.\gradlew.bat --quiet test --tests dev.charles.multigolem.spawn.VillageGolemSpawnResolverTest
.\gradlew.bat --quiet compileJava
```

Expected: pass.

---

## Task 3: Add Netherite Village Ignite Config

**Files:**
- Modify: `src/main/java/dev/charles/multigolem/config/TierStats.java`
- Modify: `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java`
- Modify: `src/main/java/dev/charles/multigolem/mixin/IronGolemAttackMixin.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV3Test.java`
- Modify: `src/test/java/dev/charles/multigolem/config/MultiGolemConfigV2Test.java`
- Modify: `src/test/java/dev/charles/multigolem/stats/GolemStatsResolverTest.java`

- [ ] **Step 3.1: Write failing config default assertions**

Add assertions wherever Netherite defaults are already asserted:

```java
assertEquals(5, MultiGolemConfig.defaults().tier(GolemVariant.NETHERITE).netheriteIgniteSeconds());
assertEquals(0, MultiGolemConfig.defaults().tier(GolemVariant.NETHERITE).netheriteVillageIgniteSeconds());
```

If V4.1 chooses to change the base default from `5` to `2`, change the first assertion to `2` and make that a deliberate release-note item. The V4.1 feature itself does not require changing the base default.

- [ ] **Step 3.2: Write failing migration assertion**

In a config migration test that loads an older config without the new field, assert the on-disk JSON is filled:

```java
String after = Files.readString(file);
assertTrue(after.contains("\"netherite_village_ignite_seconds\": 0"));
```

Also add an explicit parse test:

```java
@Test
void netheriteVillageIgniteSeconds_parsesAndClamps(@TempDir Path tmp) throws IOException {
    Path file = tmp.resolve("multigolem.json");
    Files.writeString(file, """
        {
          "tiers": {
            "netherite": {
              "netherite_ignite_seconds": 2,
              "netherite_village_ignite_seconds": 0
            }
          }
        }
        """);

    TierStats stats = MultiGolemConfig.loadOrCreate(file).tier(GolemVariant.NETHERITE);
    assertEquals(2, stats.netheriteIgniteSeconds());
    assertEquals(0, stats.netheriteVillageIgniteSeconds());
}
```

- [ ] **Step 3.3: Run failing config tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigV3Test
```

Expected: compile failure because `netheriteVillageIgniteSeconds()` does not exist.

- [ ] **Step 3.4: Extend `TierStats`**

Add a final Netherite field:

```java
Integer netheriteVillageIgniteSeconds
```

Update every `new TierStats(...)` call in production and tests. For tiers where Netherite fields do not apply, pass `null`. For the Netherite default, pass:

```java
boolean netheriteFireImmune = true;
int netheriteIgniteSeconds = 5;
int netheriteVillageIgniteSeconds = 0;
return new TierStats(...,
    netheriteFireImmune,
    netheriteIgniteSeconds,
    netheriteVillageIgniteSeconds);
```

If the release decision is to make non-village Netherite ignite `2` by default, pass:

```java
boolean netheriteFireImmune = true;
int netheriteIgniteSeconds = 2;
int netheriteVillageIgniteSeconds = 0;
return new TierStats(...,
    netheriteFireImmune,
    netheriteIgniteSeconds,
    netheriteVillageIgniteSeconds);
```

Do not add the new value by counting commas. Append `netheriteVillageIgniteSeconds` immediately after the existing `netheriteIgniteSeconds` record component and update `clampedHealthDamage()` with the same named local variables or one-field-per-line formatting.

- [ ] **Step 3.5: Parse, clamp, and write the new field**

In `MultiGolemConfig.canonicalizeTierInPlace(...)`, add:

```java
canonicalizeInt(t, "netherite_village_ignite_seconds", 0, 300);
```

In `parseTier(...)`, add:

```java
Integer netheriteVillageIgnite = def.netheriteVillageIgniteSeconds() != null
    ? clampInt(readInt(t, "netherite_village_ignite_seconds", def.netheriteVillageIgniteSeconds()),
        0, 300, "netherite_village_ignite_seconds")
    : null;
```

Pass `netheriteVillageIgnite` to the `TierStats` constructor.

In `toJson(...)`, write:

```java
if (s.netheriteVillageIgniteSeconds() != null) {
    t.addProperty("netherite_village_ignite_seconds", s.netheriteVillageIgniteSeconds());
}
```

- [ ] **Step 3.6: Add origin-aware resolver helper**

In `IronGolemAttackMixin`, add a helper:

```java
private static int netheriteIgniteSeconds(IronGolem self) {
    var stats = MultiGolem.config().tier(GolemVariant.NETHERITE);
    if (GolemSpawnOriginAttachment.get(self) == GolemSpawnOrigin.VILLAGE) {
        return stats.netheriteVillageIgniteSeconds();
    }
    return stats.netheriteIgniteSeconds();
}
```

Then replace:

```java
int seconds = MultiGolem.config().tier(GolemVariant.NETHERITE).netheriteIgniteSeconds();
```

with:

```java
int seconds = netheriteIgniteSeconds(self);
```

Keep `netherite_fire_immune` as the global gate. If fire immunity is disabled, no Netherite ignite path runs.

- [ ] **Step 3.7: Run focused config and compile tests**

Run:

```powershell
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigV3Test
.\gradlew.bat --quiet test --tests dev.charles.multigolem.config.MultiGolemConfigV2Test
.\gradlew.bat --quiet test --tests dev.charles.multigolem.stats.GolemStatsResolverTest
.\gradlew.bat --quiet compileJava
```

Expected: pass.

---

## Task 4: Document And Playtest The V4.1 Behavior

**Files:**
- Modify: `README.md`
- Modify: `docs/playtest-checklist.md`
- Modify: `docs/playtest.html`
- Modify: `CHANGELOG.md`

- [ ] **Step 4.1: Update README config table**

In the Netherite ability fields table, keep:

```markdown
| Netherite | `netherite_ignite_seconds` | `5` | Seconds of fire applied to mobs hit by Netherite golems. Set to `0` to disable ignite-on-hit. |
```

If the base default changes to `2`, update the default column to `2`.

Add:

```markdown
| Netherite | `netherite_village_ignite_seconds` | `0` | Seconds of fire applied by village-spawned Netherite golems. Uses `0` by default so opt-in Netherite village defenders do not start fires unless configured. |
```

- [ ] **Step 4.2: Update playtest checklist**

Add rows:

```markdown
- [ ] Set `village_spawning.weights.netherite` to `1` and all other village weights to `0`.
- [ ] Set `netherite_ignite_seconds: 2` and `netherite_village_ignite_seconds: 0`.
- [ ] Trigger a villager-called Netherite golem spawn; have it hit a mob; no fire is applied.
- [ ] Spawn or build a Netherite golem by a non-village path; have it hit a mob; fire is applied for 2 seconds.
- [ ] Existing/legacy Netherite golems with no stored spawn origin use `netherite_ignite_seconds`.
```

Mirror the same rows in `docs/playtest.html` without breaking the checklist storage script.

- [ ] **Step 4.3: Update changelog**

Under `## Unreleased`, add:

```markdown
- Added a separate `netherite_village_ignite_seconds` config value so village-spawned Netherite golems can avoid ignite-on-hit while other Netherite golems keep the normal Netherite ignite duration.
```

- [ ] **Step 4.4: Run docs gates**

Run:

```powershell
python scripts\check-changelog-style.py --section Unreleased
.\gradlew.bat --quiet checkChangelog checkReleaseNotesStyle
```

Expected: pass.

---

## Task 5: Final Verification

**Files:**
- No expected edits unless verification finds a focused issue.

- [ ] **Step 5.1: Static scan**

Run:

```powershell
rg -n "netherite_ignite_seconds|netherite_village_ignite_seconds|GolemSpawnOrigin|spawn_origin" src README.md docs CHANGELOG.md
rg -n "isPlayerCreated|setPlayerCreated|playerCreated" src/main/java/dev/charles/multigolem
```

Expected:
- the new village override appears in config parse/write/docs/tests;
- no origin logic uses `isPlayerCreated()` as a proxy;
- `setPlayerCreated(true)` remains limited to player-built T-pattern creation.

- [ ] **Step 5.2: Full verification**

Run:

```powershell
.\gradlew.bat --quiet check
.\gradlew.bat --quiet clean build
```

Expected: pass.

- [ ] **Step 5.3: Manual verification**

Verify in a modded server:
- `netherite_ignite_seconds: 2`, `netherite_village_ignite_seconds: 0`, and village Netherite weight positive produces village-spawned Netherite golems that do not ignite targets.
- A player-built Netherite golem under the same config ignites targets for 2 seconds.
- A marked Netherite spawn egg golem under the same config ignites targets for 2 seconds.
- A marked Netherite spawner golem under the same config ignites targets for 2 seconds if V4 spawner support shipped.
- A legacy/command-spawned Netherite golem with no origin attachment uses `netherite_ignite_seconds`.

---

## Self-Review Checklist

- V4.1 captures why this is needed: village structures are vulnerable to Netherite ignite-on-hit, but server owners may still intentionally opt into Netherite village defenders.
- The design does not rely on `isPlayerCreated()` to infer origin.
- The only special origin needed for this release is `VILLAGE`; unknown/non-village origins use the existing base Netherite setting.
- The default village-specific ignite value is `0`.
- The existing `netherite_ignite_seconds` field remains the base value for player-built, spawn egg, spawner, command, and legacy/unknown-origin Netherite golems.
- The plan allows admins to configure player-spawned/non-village Netherite golems to `2` while keeping village-spawned Netherite golems at `0`.
- Config migration fills the new field into existing configs without dropping unknown fields.
- Docs and playtest checklist cover both village and non-village behavior.
