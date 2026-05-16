# MultiGolem V2 — Design

**Date:** 2026-05-16
**Authors:** Tyler & Charles
**Status:** Draft, awaiting review (human + Codex)
**Predecessor:** V1 design at `docs/superpowers/specs/2026-05-15-multigolem-design.md` (shipped as v0.1.0+mc26.1.2)

## 1. Summary

V2 adds **two big pieces** on top of V1's server-side golem-variant system:

1. **Client-side variant textures** — modded clients see each variant with its own colored skin (vanilla iron golem model with a different texture per tier). Vanilla clients continue to work unchanged and see all variants as plain iron golems. Textures generated programmatically from the vanilla iron golem template so UV layout is guaranteed correct.
2. **Five special abilities** — one per non-iron variant: Copper (lightning heals), Gold (+25% speed + vanity particles), Emerald (heals when villagers are nearby), Diamond (combined passive + on-attack lightning with shared cooldown), Netherite (fire/lava immunity + ignite-on-hit).

V2 also introduces a per-tier exclusion list (`ignored_target_types`) so any tier can be configured to skip certain mob types — defaulting to ignoring creepers on all non-iron tiers to prevent collateral block damage and avoid charged-creeper events from the diamond zap.

V2 ships as a **single release** (v0.2.0+mc26.1.2).

## 2. Goals & Non-goals

**Goals**

- Five distinct, visible abilities — each tier feels mechanically unique, not just statistically different
- Modded clients see colored variant golems without requiring a resource pack
- Vanilla clients still work — V2 keeps V1's "server-side functional" promise
- All ability behaviors configurable per-tier in `multigolem.json`
- Persistent ability state (e.g., diamond cooldown timestamps) survives chunk unload, save/load, and server restarts
- V1 → V2 config migration is automatic and lossless

**Non-goals**

- Custom entity types (still vanilla `IronGolem` + attachments)
- Custom blocks, items, or recipes
- A GUI/config screen (still a hand-edited JSON file)
- Resource pack delivery (textures ship with the mod)
- Multi-MC-version compatibility (single jar targets MC 26.1.2)
- Lava-walking in baseline V2 (deferred — see §5)

## 3. Scope (release shape)

| Phase | Contents |
|---|---|
| **V2 (this release, v0.2.0+mc26.1.2)** | Client source set + 5 variant textures; 5 special abilities; per-tier `ignored_target_types`; `GolemAbilityState` attachment for persistent ability state; extended config schema. |
| **V3 (future)** | Village natural-spawn variant weighting (sketched in V1 spec §3). |
| **Backlog (post-V2, pre-V3 or later)** | Lava-walking for netherite golems (deferred — see §5.5). |

## 4. Architecture

**Foundation unchanged from V1:** vanilla `IronGolem` entities plus Fabric attachments. No new entity types, no new blocks, no new items.

### 4.1 Client / Server split

V2 is the first version that uses both source sets:

| Source set | What's in it | Required to run? |
|---|---|---|
| `src/main/java` | Server logic, abilities, mixins, config | Always (server side) |
| `src/client/java` | Client mod initializer + renderer mixin + texture selection | Only on modded clients |

Vanilla clients connecting to a V2 server work normally — they see iron golem models for every variant (because the renderer mixin is client-side only). All gameplay (stats, abilities, drops, healing) is server-driven and works regardless of client.

### 4.2 Persistent attachments

V1 introduced `GolemVariant`. V2 adds a second:

| Attachment | Type | Purpose |
|---|---|---|
| `multigolem:variant` (V1) | `GolemVariant` enum | Which tier this golem is |
| `multigolem:ability_state` (V2) | `GolemAbilityState` record | Per-entity ability state — currently just `nextDiamondAbilityGameTime: long`; designed to extend in V3+ |

Both persist via Fabric's `AttachmentRegistry` with codec-based serialization. Cooldown timestamps survive chunk unload, save/load, and server restart, so reloading can't reset the diamond cooldown.

**V2 also configures `GolemVariant` to sync to client** (via Fabric's attachment sync API — exact builder call confirmed by spike 9, §7). This is required so the client renderer can read the variant per entity and pick the right texture. The sync change is backwards-compatible: V1 servers without sync configured still work (clients just don't see variant textures). `GolemAbilityState` is NOT synced — clients don't need cooldown timestamps.

### 4.3 Stat strategy (carried over from V1, extended)

V1's `VariantAttributes` (Plan B from V1 §4) applies transient `AttributeModifier`s for `MAX_HEALTH` and `ATTACK_DAMAGE` on entity load and on attachment-set. V2 extends this with a third modifier for **Gold's MOVEMENT_SPEED** (+25% by default).

### 4.4 Damage handling — events first, mixins as fallback

Per Codex feedback (review on 2026-05-16): prefer `ServerLivingEntityEvents.ALLOW_DAMAGE` for damage interception. Only fall back to a `hurtServer` mixin if the event can't express the needed behavior.

V2 uses ALLOW_DAMAGE for:

- **Copper lightning heal:** if `source.is(DamageTypes.LIGHTNING_BOLT)` and target is copper-variant → return false (cancel damage), then in the same listener trigger the heal and particles
- **Netherite fire/lava immunity:** if `source.is(DamageTypeTags.IS_FIRE)` and target is netherite-variant → return false
- **Diamond lightning self-immunity:** if `source.is(DamageTypes.LIGHTNING_BOLT)` and target is diamond-variant → return false (no heal, just no damage)

### 4.5 Attack hooks — one narrow mixin, two abilities

`IronGolem#doHurtTarget(ServerLevel, Entity)` is the single mixin point for two abilities:

- **Diamond on-attack:** if cooldown ready (read from `GolemAbilityState`), spawn `LightningBolt` on target, update cooldown timestamp
- **Netherite ignite:** call the verified 26.1.2 fire-ticks API on the target (likely `Entity#igniteForSeconds(int)` or `Entity#setRemainingFireTicks(int)` — confirmed by spike)

Mixin uses `@Inject(at = @At("TAIL"))` to run after vanilla damage resolves but before the method returns. Both branches gated by variant check.

### 4.6 Renderer (client-only) — spike-first

The renderer approach is **not finalized in this spec** because Minecraft 26.1 introduced a render-state pipeline that changes where mixin points live. The spike (see §7) confirms the exact targets.

Expected shape (subject to spike findings):

- Mixin into the render-state extraction step on `IronGolemRenderer` (or its state class) to capture the entity's `GolemVariant` attachment into a custom field on the render state
- Mixin into the texture-resolution step to read from that field and return the matching variant texture path
- All client mixins live in a separate config file (`multigolem.client.mixins.json`) with `"environment": "client"` so server-side jars don't try to load them
- No client classes referenced from `src/main/java`

Iron variant returns the vanilla texture path unchanged (no `iron_golem.png` ships in this mod's assets).

### 4.7 Mojang identity in 26.1.2 (carry-over)

V1 documented in `docs/26.1.2-mojang-targets.md`:

- `IronGolem` at `net.minecraft.world.entity.animal.golem.IronGolem`
- `Identifier` (not `ResourceLocation`) at `net.minecraft.resources.Identifier`
- `LivingEntity#setLastHurtByMob` mixin must target `LivingEntity.class` not `IronGolem.class`
- `EntitySpawnReason.TRIGGERED` for golem spawns
- `ItemStack.consume(int, Player)` not `shrink(int)`

V2 adds findings during its spikes (renderer pipeline, fire-ticks API, ALLOW_DAMAGE signature, mixin compatibility level for Java 25) to the same file.

### 4.8 Mod identity (unchanged)

- Mod id: `multigolem`
- Java package: `dev.charles.multigolem`
- Archives base name: `multigolem`

## 5. Per-ability detail

All five abilities are configurable per-tier in `multigolem.json` (see §6). All particle effects are emitted server-side so both vanilla and modded clients render them identically.

### 5.1 Copper — Lightning Heal

| | |
|---|---|
| **Trigger** | `ServerLivingEntityEvents.ALLOW_DAMAGE` listener: damage source is `DamageTypes.LIGHTNING_BOLT` AND target is iron golem with `COPPER` variant |
| **Effect** | Cancel damage; heal by `copper_lightning_heal_amount` (default = null → heal to max HP); emit visual |
| **Visual** | ~40 `ELECTRIC_SPARK` particles in a sphere around the golem, distributed over ~3 seconds |
| **Sources** | Both naturally-occurring lightning and trident-channeled lightning (same event fires for both) |
| **Edge cases** | At full HP: damage still cancelled, visual still plays. Target despawns mid-strike: vanilla handles gracefully. |
| **Config** | `copper_lightning_immune` (bool, default true), `copper_lightning_heal_amount` (nullable double, null = full heal) |

### 5.2 Gold — Speed Boost + Vanity

| | |
|---|---|
| **Trigger A (stat)** | `VariantAttributes.apply()` adds a transient `MOVEMENT_SPEED` modifier on `GOLD` variant. Operation: `ADD_MULTIPLIED_TOTAL` with `(gold_speed_multiplier − 1.0)` (default 0.25 → 25% boost) |
| **Trigger B (sprint dust)** | Gold-tick handler: if golem's horizontal velocity² > threshold (`0.001`), emit ~2 `POOF` particles at feet per server tick |
| **Trigger C (sunlight shine)** | Gold-tick handler: if velocity ≈ 0 AND `level.canSeeSky(blockPosition())` AND `level.isDay()` AND no rain/thunder → emit 1 `END_ROD` particle near head every ~20 ticks |
| **Config** | `gold_speed_multiplier` (double, default 1.25), `gold_sprint_particles_enabled` (bool, default true), `gold_sunlight_shine_enabled` (bool, default true) |

### 5.3 Emerald — Villager Aura Heal

| | |
|---|---|
| **Trigger** | Emerald-tick handler: every `20 × emerald_heal_interval_seconds` ticks (default 40 ticks = 2s), scan a `(2 × emerald_aura_range + 1)`-block box for entities subclassing `AbstractVillager` |
| **Filter** | `emerald_count_wandering_traders` decides whether wandering traders count (default true, includes both regular villagers and wandering traders). Always excludes zombie villagers and illagers (they don't subclass `AbstractVillager`). |
| **Effect** | If at least one matching villager found AND golem HP < max HP: heal by `emerald_heal_per_tick` (default 1.0 HP); emit visual |
| **Visual** | Single `HAPPY_VILLAGER` particle floating up from the golem on each heal tick |
| **Config** | `emerald_aura_range` (int, default 8), `emerald_heal_interval_seconds` (double, default 2.0; clamped ≥ 0.5), `emerald_heal_per_tick` (double, default 1.0), `emerald_count_wandering_traders` (bool, default true) |

### 5.4 Diamond — Combined Lightning

| | |
|---|---|
| **State** | `GolemAbilityState.nextDiamondAbilityGameTime` (long, absolute world tick when next ability is allowed; 0 = ready) |
| **Trigger A (passive)** | Diamond-tick handler: if `world.getGameTime() >= nextDiamondAbilityGameTime`, scan box radius `diamond_aura_range` (default 12) for entities matching the `diamond_target_mode` predicate AND NOT in this tier's `ignored_target_types`. Pick nearest with line-of-sight (single `world.clip()` ray, golem-eye → target-eye). If found: spawn `LightningBolt`, set `nextDiamondAbilityGameTime = currentTime + randomBetween(diamond_cooldown_min_seconds, diamond_cooldown_max_seconds) × 20` |
| **Trigger B (on-attack)** | `IronGolem#doHurtTarget` mixin tail injection: if variant is DIAMOND AND cooldown ready AND target passes both `diamond_target_mode` predicate and `ignored_target_types` filter → spawn `LightningBolt` on target, update cooldown |
| **Visual (cooldown ready)** | Diamond-tick handler: when cooldown ready, every 20 ticks emit 1 subtle `END_ROD` particle near golem's head. When on cooldown, no particle. |
| **Diamond self-immunity** | `ServerLivingEntityEvents.ALLOW_DAMAGE` listener: if target is DIAMOND variant AND source is `DamageTypes.LIGHTNING_BOLT` AND `diamond_lightning_proof` is true → cancel damage (no heal, just immunity) |
| **Target mode** | `diamond_target_mode` recognizes: `ALL_HOSTILE_MOBS` (default — predicate matches `Enemy` interface implementers), `ALL_HOSTILE_MOBS_AND_PLAYERS` (Enemy + Player), `BOSSES_ONLY` (WitherBoss, EnderDragon, Warden), `NONE` (zap disabled — golem keeps stats and melee AI) |
| **Edge cases** | Target dies between scan and bolt spawn: vanilla `LightningBolt.create` handles missing target by striking the position. LOS via single ray; one block of cover blocks the zap. `diamond_target_mode: NONE` makes the diamond golem behave like a beefier iron golem (no lightning at all). |
| **Config** | `diamond_target_mode` (string, default `ALL_HOSTILE_MOBS`), `diamond_cooldown_min_seconds` (int, default 30), `diamond_cooldown_max_seconds` (int, default 60), `diamond_aura_range` (int blocks, default 12), `diamond_lightning_proof` (bool, default true) |

### 5.5 Netherite — Fire/Lava Immunity + Ignite-on-Hit

| | |
|---|---|
| **Trigger A (immunity)** | `ServerLivingEntityEvents.ALLOW_DAMAGE` listener: if target is NETHERITE variant AND `source.is(DamageTypeTags.IS_FIRE)` → cancel damage |
| **Trigger B (ignite)** | `IronGolem#doHurtTarget` mixin tail injection (shared with diamond): if variant is NETHERITE, call `target.igniteForSeconds(netherite_ignite_seconds)` (or the spike-confirmed equivalent — see §7) |
| **Visual** | Standard vanilla fire on victim. No custom particles on the netherite golem itself in V2 baseline. |
| **Lava-walking** | **Deferred** — see §5.5.1. Config field `netherite_lava_walking` ships in V2's schema for forward compatibility; if the spike doesn't find a clean hook, the field is read but has no effect (logged once at startup). |
| **Edge cases** | Ignite on fire-immune mobs (blazes, magma cubes, other netherite golems): vanilla fire-ticks API is a no-op on immune entities; no special handling needed. |
| **Config** | `netherite_fire_immune` (bool, default true), `netherite_ignite_seconds` (int, default 5), `netherite_lava_walking` (bool, default true — see §5.5.1) |

#### 5.5.1 Lava-walking — deferred from V2 baseline

Per Codex review: fluid-pushability mixins are broad and mod-compat-sensitive. V2 ships fire/lava damage immunity AND ignite-on-hit. Lava-walking moves to a **stretch goal** evaluated by spike (§7).

- If the spike finds a clean, mod-friendly hook (an entity flag, an attribute, a specific Fabric event), lava-walking ships in v0.2.0.
- If not, the field stays in the config schema (logged as "not yet implemented") and the feature ships in a follow-up release (v0.2.1 or later) once a clean implementation is found.

Charles already approved deferral.

### 5.6 Cross-tier — `ignored_target_types`

Per-tier exclusion list that filters out specific mob types from BOTH normal melee AI targeting AND the diamond zap. Designed to prevent collateral terrain damage from creeper explosions and to avoid the diamond zap creating charged creepers.

**Recognized values in V2:**

| Value | Excludes |
|---|---|
| `CREEPERS` | Entities tagged `#minecraft:creepers` (vanilla creepers and any mod additions tagged as such) |
| `ENDERMEN` | `net.minecraft.world.entity.monster.EnderMan` |
| `PLAYERS` | `net.minecraft.world.entity.player.Player` |
| `ALL_BOSSES` | `WitherBoss`, `EnderDragon`, `Warden` |

Unknown values log a WARN and are dropped from the list at load time (other valid entries preserved).

**Defaults:**

- `iron`: `[]` — vanilla iron golem behavior preserved
- `copper`, `gold`, `emerald`, `diamond`, `netherite`: `["CREEPERS"]` — new tiers default to ignoring creepers

**Implementation:** spike (§7) confirms whether to mixin into a Mob targeting goal predicate or use a per-tick targeting reset. For the diamond zap, the exclusion is applied as a post-filter on the target candidate list — no separate mixin needed.

**Documentation:** the v0.2.0 CHANGELOG entry explicitly notes this is a default behavior change for the new tiers (and notes that iron is unchanged). Players who prefer vanilla iron-vs-creeper behavior keep it; players who want creeper pacifism for iron edit one config line.

## 6. Config schema (extends V1)

Full V2 default config:

```json
{
  "allow_golem_healing": true,
  "tiers": {
    "copper": {
      "max_health": 60,
      "attack_damage": 8.5,
      "anger_on_hit": true,
      "ignored_target_types": ["CREEPERS"],
      "copper_lightning_immune": true,
      "copper_lightning_heal_amount": null
    },
    "iron": {
      "max_health": 100,
      "attack_damage": 15.0,
      "anger_on_hit": true,
      "ignored_target_types": []
    },
    "gold": {
      "max_health": 130,
      "attack_damage": 22.5,
      "anger_on_hit": true,
      "ignored_target_types": ["CREEPERS"],
      "gold_speed_multiplier": 1.25,
      "gold_sprint_particles_enabled": true,
      "gold_sunlight_shine_enabled": true
    },
    "emerald": {
      "max_health": 200,
      "attack_damage": 40.0,
      "anger_on_hit": true,
      "ignored_target_types": ["CREEPERS"],
      "emerald_aura_range": 8,
      "emerald_heal_interval_seconds": 2.0,
      "emerald_heal_per_tick": 1.0,
      "emerald_count_wandering_traders": true
    },
    "diamond": {
      "max_health": 350,
      "attack_damage": 62.5,
      "anger_on_hit": true,
      "ignored_target_types": ["CREEPERS"],
      "diamond_target_mode": "ALL_HOSTILE_MOBS",
      "diamond_cooldown_min_seconds": 30,
      "diamond_cooldown_max_seconds": 60,
      "diamond_aura_range": 12,
      "diamond_lightning_proof": true
    },
    "netherite": {
      "max_health": 600,
      "attack_damage": 85.0,
      "anger_on_hit": true,
      "ignored_target_types": ["CREEPERS"],
      "netherite_fire_immune": true,
      "netherite_ignite_seconds": 5,
      "netherite_lava_walking": true
    }
  }
}
```

### 6.1 Validation rules (V1 carried over + V2 additions)

V1 rules (still in force):

- Missing top-level fields → fill with defaults, log INFO
- Missing per-tier fields → fill with defaults for that tier, log INFO
- `max_health` outside [1, 2048] → clamp, log WARN
- `attack_damage` outside [0.0, 2048.0] → clamp, log WARN
- Malformed JSON → load all defaults, log WARN, do not overwrite user's file

V2 additions:

- Unknown `ignored_target_types` value → drop with WARN, preserve other valid entries
- Unknown `diamond_target_mode` value → fall back to `ALL_HOSTILE_MOBS` with WARN
- `diamond_cooldown_min_seconds > diamond_cooldown_max_seconds` → swap silently with WARN
- `emerald_heal_interval_seconds <= 0` → clamp to 0.5 with WARN
- `emerald_aura_range` outside [1, 64] → clamp with WARN
- `copper_lightning_heal_amount` < 0 → clamp to 0 (no heal) with WARN
- `gold_speed_multiplier <= 0` → clamp to 0.1 (10% min so the golem can still move) with WARN
- `diamond_aura_range` outside [1, 64] → clamp with WARN
- `netherite_ignite_seconds` outside [0, 300] → clamp with WARN

### 6.2 Migration from V1

V1 config files load without errors:

- V1 fields (`max_health`, `attack_damage`, `anger_on_hit`) preserved exactly
- Missing V2 fields filled with defaults at load time
- After load, the populated config is written back to disk so the user sees the V2 schema with their V1 values preserved
- The write-back also serves as automatic schema upgrade — no manual config migration needed

## 7. Spike list (required BEFORE implementation)

Codex's review surfaced areas where the right mixin point or API call isn't knowable without inspecting 26.1.2 source. These spikes run first and update `docs/26.1.2-mojang-targets.md`:

| # | Spike | Why it matters |
|---|---|---|
| 1 | Renderer pipeline in 26.1.2 — `IronGolemRenderer` + render-state class | Determines the two mixin targets for variant texture selection. The plan assumes a render-state pattern, but the exact class names and method points need confirmation. |
| 2 | `ServerLivingEntityEvents.ALLOW_DAMAGE` signature in Fabric API 0.148.0+26.1.2 | Determines the listener signature for damage interception. Used by copper, netherite-fire-immunity, diamond-self-immunity. |
| 3 | Fire-ticks API method name in 26.1.2 — `Entity#igniteForSeconds(int)` vs `setRemainingFireTicks(int)` | Used by netherite ignite-on-hit. Verify against decompiled source. |
| 4 | Mixin `compatibilityLevel` for Java 25 source | Verify `JAVA_25` exists in `CompatibilityLevel` enum of the Mixin library bundled with Fabric Loader 0.19.2. If not present, use the highest available and note why. |
| 5 | Damage tag for fire — `DamageTypeTags.IS_FIRE` (or equivalent) | Determines the predicate for netherite fire-immunity. |
| 6 | Mob targeting hook for `ignored_target_types` enforcement | Find the right mixin point (likely a target-selection goal predicate). If invasive, fall back to a per-tick target reset. |
| 7 | Lava-walking hook | Spike-or-defer. Look for a clean, mod-friendly mechanism (entity attribute, flag, Fabric event). If none found, lava-walking deferred from v0.2.0. |
| 8 | Custom particle scattering for `ELECTRIC_SPARK` cluster | Confirm the right server-side particle emission method on `ServerLevel` (likely `sendParticles(...)` with a count and spread). |
| 9 | Attachment client sync | Confirm Fabric AttachmentRegistry builder option to sync attachment data to the client. Required for the client renderer to read `GolemVariant` per entity. Likely a `.syncWith(...)` or similar on the builder; if unavailable, fall back to a custom S2C packet announcing variant on entity spawn. **Must complete before renderer mixin work.** |

Each spike updates the targets doc with **exact class/method names, signatures, and a one-line rationale**. No mixin code is written until the relevant spike completes.

## 8. Components & file structure

### 8.1 New files

```
src/client/java/dev/charles/multigolem/client/
  MultiGolemClient.java                # ClientModInitializer entrypoint
  render/
    GolemRenderStateExtension.java     # Custom field added to IronGolem render state (variant)
    GolemTextureSelector.java          # Pure helper: GolemVariant → texture Identifier
  mixin/client/
    IronGolemRenderStateMixin.java     # Capture variant into render state
    IronGolemRendererMixin.java        # Use captured variant to pick texture

src/client/resources/
  multigolem.client.mixins.json        # Client-side mixin config

src/main/java/dev/charles/multigolem/
  ability/
    AbilityRegistry.java               # register() helper - wires events from MultiGolem.onInitialize
    CopperAbility.java                 # ALLOW_DAMAGE listener + heal + particles
    GoldAbility.java                   # Tick particles (sprint dust, sunlight shine); MOVEMENT_SPEED modifier handled by VariantAttributes
    EmeraldAbility.java                # Tick handler: villager scan + heal + particles
    DiamondAbility.java                # Tick handler: passive zap, cooldown management, cooldown-ready particle
    NetheriteAbility.java              # ALLOW_DAMAGE listener for fire immunity; ignite-on-hit handled by IronGolemAttackMixin
  attachment/
    GolemAbilityState.java             # Record + AttachmentType for nextDiamondAbilityGameTime (and future fields)
  ability/
    TargetFilter.java                  # Pure helper: predicates for ignored_target_types + diamond_target_mode
  mixin/
    IronGolemAttackMixin.java          # @Inject TAIL on doHurtTarget - dispatches to DiamondAbility + NetheriteAbility
    GolemTargetingMixin.java           # Mixin point for ignored_target_types - target spike confirms

src/main/resources/assets/multigolem/textures/entity/
  copper_golem.png                     # Generated
  gold_golem.png                       # Generated
  emerald_golem.png                    # Generated
  diamond_golem.png                    # Generated
  netherite_golem.png                  # Generated
  (no iron_golem.png - falls through to vanilla)

build-inputs/textures/
  iron_golem.template.png              # Committed copy of vanilla MC 26.1.2 iron_golem.png
  LICENSE-AND-PROVENANCE.md            # Source (vanilla MC client jar), SHA-256, Minecraft EULA note

scripts/
  generate-textures.py                 # Reads template, applies per-tier transforms, writes 5 PNGs

src/test/java/dev/charles/multigolem/
  ability/
    TargetFilterTest.java              # Predicate tests for both filters
    DiamondCooldownTest.java           # Pure-Java cooldown bookkeeping
    EmeraldHealMathTest.java           # Tick interval + heal-amount math
  attachment/
    GolemAbilityStateTest.java         # Codec round-trip
  config/
    MultiGolemConfigV2Test.java        # New V2 fields, V1→V2 migration
```

### 8.2 Modified files

- `src/main/java/dev/charles/multigolem/MultiGolem.java` — wire `AbilityRegistry.register()`, register `GolemAbilityState` attachment
- `src/main/java/dev/charles/multigolem/attribute/VariantAttributes.java` — add MOVEMENT_SPEED modifier branch for Gold
- `src/main/java/dev/charles/multigolem/config/MultiGolemConfig.java` — new fields, validation rules, V1→V2 migration
- `src/main/java/dev/charles/multigolem/config/TierStats.java` — extend record with V2 ability fields (or split into a richer per-tier config record)
- `src/main/java/dev/charles/multigolem/stats/GolemStatsResolver.java` — accessors for V2 fields
- `src/main/resources/multigolem.mixins.json` — add `IronGolemAttackMixin` and `GolemTargetingMixin`
- `src/main/resources/fabric.mod.json` — add `client` entrypoint, reference `multigolem.client.mixins.json`
- `build.gradle` — `clientImplementation` for Fabric API client; `genTextures` task that runs `scripts/generate-textures.py` and copies output to `src/main/resources/assets/multigolem/textures/entity/` before `processResources`
- `gradle.properties` — bump `mod_version` to `0.2.0+mc26.1.2` (done at release time)
- `docs/26.1.2-mojang-targets.md` — append spike findings
- `CHANGELOG.md` — V2 release notes

### 8.3 Build flow for textures

```
build-inputs/textures/iron_golem.template.png
                 │
                 ▼
   scripts/generate-textures.py (run by `genTextures` Gradle task)
                 │
                 ▼
   src/main/resources/assets/multigolem/textures/entity/{copper,gold,emerald,diamond,netherite}_golem.png
                 │
                 ▼
   processResources → built jar
```

The template is committed (not fetched at build time). The generated PNGs are committed too so reviewers can see what's shipping without running the script.

### 8.4 Art direction reference

**See [`docs/v2-texture-art-direction.png`](../../v2-texture-art-direction.png)** — a four-angle (front / back / left / right) rendered concept of all five variants in the target visual style. Generated by ChatGPT during V2 brainstorming; serves as the **canonical visual target** for the `generate-textures.py` script outputs.

For each variant, this reference establishes:

| Tier | Palette | Detail character |
|---|---|---|
| Copper | Warm orange-brown base, sparse green oxidation streaks | Worn edges and seams |
| Gold | Rich yellow with blocky panel highlights | Slightly polished, matte (not cartoon-bright) |
| Emerald | Greenish stone | Embedded emerald cabochons on chest/head/limbs; chiseled, carved look |
| Diamond | Pale cyan with faceted panels | Clean, crystalline armor feel |
| Netherite | Dark charcoal with subtle purple undertones | Glowing orange ember-crack lines (sparse — flavor, not overwhelming) |

This reference is **art direction, not a texture sheet** — the rendered images use vanilla UVs implicitly but aren't directly usable as `entity/<tier>_golem.png`. The generator script reads the vanilla iron_golem.png template (for UV correctness) and applies per-tier color transforms + overlay detail aimed at matching this reference. Visual fidelity to the reference is a manual review criterion during implementation.

## 9. Implementation order

Low-risk first, riskier mixins last. Validates the foundation before the renderer.

1. Run all 8 spikes (§7), update `docs/26.1.2-mojang-targets.md`
2. Extend `MultiGolemConfig` + `TierStats` with V2 fields + validation (TDD)
3. Add `GolemStatsResolver` accessors for V2 fields (TDD)
4. Add `GolemAbilityState` attachment (codec round-trip test)
5. Add `TargetFilter` pure helper (predicate tests)
6. Add `AbilityRegistry` skeleton with empty `register()`
7. Commit vanilla iron golem template + LICENSE-AND-PROVENANCE; add `generate-textures.py`; add `genTextures` Gradle task
8. **Gold ability** — extend `VariantAttributes` with MOVEMENT_SPEED modifier; add particle tick handler
9. **Emerald ability** — tick handler with villager scan and heal
10. **Copper ability** — ALLOW_DAMAGE event listener
11. **Netherite fire-immunity** — ALLOW_DAMAGE event listener
12. **`IronGolemAttackMixin`** — single mixin dispatching to Diamond and Netherite branches
13. **Netherite ignite-on-hit** — wire into the attack mixin
14. **Diamond on-attack lightning** — wire into the attack mixin, gated by cooldown
15. **Diamond passive lightning** — tick handler with LOS scan, cooldown update
16. **Diamond self-immunity** — ALLOW_DAMAGE event listener
17. **Diamond cooldown-ready visual** — tick handler particle emission
18. **`GolemTargetingMixin`** — enforce `ignored_target_types` on melee AI (spike output drives the exact mixin shape)
19. **Enable client sync on `GolemVariant` attachment** — per spike 9, add the `.syncWith(...)` (or fallback packet) so clients see per-entity variant
20. **Client source set + renderer mixins** — variant texture selection (spike-confirmed targets)
21. **Lava-walking** — spike result either ships it here or defers to a later release
22. CHANGELOG entry, listing description updates, regression playtest

## 10. Error handling

- **Unknown attachment data on load** (e.g., a V2 client loads a V3 save with a future variant): treat as IRON, log WARN once per session (V1 behavior, unchanged)
- **Texture generation script failure during build:** Gradle task fails the build with the Python script's stderr — never silently produce a partial set. Build is gated on `genTextures` succeeding.
- **Texture file missing at runtime:** renderer falls back to vanilla iron golem texture for the affected variant; log WARN once per variant per session
- **Renderer mixin fails to apply on a modded client:** falls back to vanilla iron golem visual; gameplay continues server-driven; log ERROR
- **Ability event listener throws:** wrap each ability's hook body in a try/catch that logs the exception and proceeds (one ability's bug must not break the others or the entity)
- **`GolemAbilityState` codec deserialization failure:** treat as new state (cooldowns reset); log WARN
- **Config validation failures:** §6.1 rules apply — clamp, drop, fall back, log; never throw at config load time

## 11. Testing

### 11.1 Unit tests

- `MultiGolemConfigV2Test` — new V2 fields parse correctly; validation clamps and warns; V1→V2 migration preserves V1 values and fills V2 defaults
- `TargetFilterTest` — `ignored_target_types` filters; `diamond_target_mode` modes; unknown values dropped gracefully
- `DiamondCooldownTest` — cooldown math: next-allowed timestamps, random within [min, max], persistence round-trip
- `EmeraldHealMathTest` — tick interval translates correctly to game ticks
- `GolemAbilityStateTest` — attachment codec round-trips both empty and populated states
- Existing V1 tests stay green (16 tests from V1)

### 11.2 Manual playtest (extends V1's `docs/playtest-checklist.md`)

New V2 rows to add:

**Textures** (modded client)

- [ ] Copper golem displays with copper-tinted skin
- [ ] Repeat for gold, emerald, diamond, netherite
- [ ] Iron golem displays as vanilla (no change)

**Textures** (vanilla client)

- [ ] Vanilla client connects to V2 server; sees all variants as iron golems
- [ ] Modded player and vanilla player share the same world; modded sees colors, vanilla sees iron — no desync

**Copper lightning heal**

- [ ] Stand near copper golem in thunderstorm OR strike with channeling trident — golem takes no damage AND heals
- [ ] Set `copper_lightning_heal_amount: 0` — lightning still doesn't damage, but no heal either
- [ ] Set `copper_lightning_immune: false` — lightning damages normally

**Gold speed + vanity**

- [ ] Gold golem moves visibly faster than iron
- [ ] Sprint-dust particles emit when gold golem walks
- [ ] Sunlight-shine particle appears when gold golem is stationary outdoors during day
- [ ] No shine particle indoors or at night

**Emerald villager aura**

- [ ] Damage emerald golem; stand near a villager; verify heal every 2s
- [ ] Move villager out of 8-block range; heal stops
- [ ] Wandering trader nearby with `emerald_count_wandering_traders: true` — heal proceeds
- [ ] Zombie villager nearby — no heal triggered
- [ ] Particle emits with each heal tick

**Diamond lightning**

- [ ] Approach diamond golem with creeper at 12 blocks → no zap (creeper in `ignored_target_types`)
- [ ] Approach with skeleton at 12 blocks → zap; verify cooldown timing (30-60s before next)
- [ ] Hit something with diamond golem when cooldown ready → swing summons lightning
- [ ] Block line-of-sight with a wall — passive zap doesn't fire through wall
- [ ] Lightning bolt hits diamond golem — no damage (self-immunity)
- [ ] Set `diamond_target_mode: BOSSES_ONLY`; restart; verify normal hostiles aren't zapped

**Netherite fire/lava + ignite**

- [ ] Netherite golem stands in lava — no damage
- [ ] Hit by ghast fireball — no damage
- [ ] Magma block — no damage
- [ ] Hit a mob — mob catches fire for 5 seconds
- [ ] Hit a fire-immune mob (blaze) — no fire effect (vanilla immunity), no error
- [ ] Lava-walking (if shipped) — netherite golem walks on lava surface

**`ignored_target_types`**

- [ ] Default: copper/gold/emerald/diamond/netherite ignore creepers in melee
- [ ] Default: iron attacks creepers (vanilla behavior preserved)
- [ ] Set `iron.ignored_target_types: ["CREEPERS"]` → iron now ignores creepers too
- [ ] Unknown value in list (`"DRAGONS_AND_CASTLES"`) → warning logged at startup, value dropped, other entries preserved

**Save/load + migration**

- [ ] V1 config (no V2 fields) loads into V2 server; defaults appear in file after first run
- [ ] World with active diamond cooldown saved, server restarted — cooldown still active when world loads
- [ ] V2 client connects to V1 server — variant textures don't appear (V1 doesn't sync the attachment to clients), no crash, no errors logged

**Vanilla parity preserved**

- [ ] Existing V1 playtest checklist (all rows) still passes — V2 adds, doesn't break

## 12. Build, release, packaging

- Bump `mod_version` to `0.2.0+mc26.1.2` (at release time, not during dev)
- CHANGELOG entry covers: textures, 5 abilities, ignored_target_types, V1 config migration, default behavior change for new tiers (creeper-pacifism)
- Modrinth listing description updated via `docs/modrinth-listing.md` (script syncs on release)
- CurseForge listing description updated by hand (no API sync)
- Tag `v0.2.0+mc26.1.2`; CI publishes to GitHub Release, Modrinth, CurseForge
- Existing description-sync fix from commit `36b58a5` activates on this release

## 13. Codex review — incorporated decisions (2026-05-16)

Codex reviewed an architectural sketch of V2 before this spec was written. Findings folded in:

1. **Renderer pipeline** — V2 doesn't assume `getTextureLocation(entity)`; renderer is spike-first (§4.6, §7 spike 1). ✅
2. **Damage immunity hooks** — `ServerLivingEntityEvents.ALLOW_DAMAGE` preferred over mixins for copper-lightning and netherite-fire (§4.4). LightningBolt mixin dropped. ✅
3. **Diamond/netherite attack hooks** — narrow `IronGolem#doHurtTarget` mixin (one mixin, two abilities) kept (§4.5). Fire API name confirmed via spike (§7 spike 3). ✅
4. **Lava-walking** — deferred from V2 baseline; ship-or-defer per spike outcome (§5.5.1, §7 spike 7). ✅
5. **Diamond cooldown state** — dedicated `GolemAbilityState` persistent attachment, separate from `GolemVariant` (§4.2). Stores `nextDiamondAbilityGameTime`. ✅
6. **AbilityRegistry** — kept as a thin `register()` helper (§8.1). No polymorphic framework. ✅
7. **Texture generation** — vanilla template committed under `build-inputs/textures/` with LICENSE-AND-PROVENANCE.md; `genTextures` Gradle task deterministic; iron variant falls through to vanilla (§4.6, §8.3). ✅
8. **Client/source-set gotchas** — `splitEnvironmentSourceSets()` already correct from V1; client entrypoint added to fabric.mod.json; separate client mixin config (§4.1). Mixin compatibility level for Java 25 is spike item (§7 spike 4). ✅
9. **V1 loot quirk** — reclassified as intentional design per Charles's playtest feedback ("bonus iron drops are cool"). Documented in V2 spec rather than fixed. ✅

## 14. Open items for V2 implementation

These are NOT blockers for moving forward — they get resolved during implementation per the plan that follows this spec:

- Exact behavior of `BOSSES_ONLY` in `diamond_target_mode` if a player builds a tagged-as-boss custom entity. Default test set: vanilla Wither, Ender Dragon, Warden.
- Whether the `END_ROD` particle for diamond cooldown-ready is too bright. Tunable in implementation; can swap to `SOUL_FIRE_FLAME` or another candidate during playtest.
- Whether `setTarget(null)` is sufficient to enforce `ignored_target_types`, or whether goal-level filtering is needed. Spike 6 decides.

---

*End of design. V2 implementation plan to be written next via the writing-plans skill once this design is approved.*
