# MultiGolem

A Fabric mod for Minecraft 26.1.2 that adds Copper, Gold, Emerald, Diamond, and Netherite golem variants alongside the vanilla Iron Golem. Built by Tyler and Charles.

## What it does

- Five new golem tiers, built like an Iron Golem (T-pattern + carved pumpkin) but with a different body block.
- Each tier has a unique texture on modded clients. Vanilla clients see all variants as regular iron golems.
- Stats scale: Copper is weakest, Netherite is strongest (designed to beat a Warden 1v1).
- Heal each golem with its matching ingot.
- Per-tier special abilities (see below).
- Per-tier `ignored_target_types` — copper/gold/emerald/diamond/netherite ignore creepers by default to prevent collateral block damage.
- **Server-side functional.** Vanilla clients can connect with no mod installed; stats, drops, and creation all behave correctly.

## Recipes

Build a T-shape (1 base block, 1 center, 2 arms) out of one of:

| Body block | Golem |
|---|---|
| Copper Block | Copper Golem |
| Iron Block | Iron Golem (vanilla, unchanged) |
| Gold Block | Gold Golem |
| Emerald Block | Emerald Golem |
| Diamond Block | Diamond Golem |
| Netherite Block | Netherite Golem |

Place a carved pumpkin on top. The vanilla single-copper-block + pumpkin recipe still spawns the vanilla Copper Golem (with its chest behavior).

## Stats (defaults)

| Tier | Max HP | Avg Atk Damage |
|---|---:|---:|
| Copper | 60 | 8.5 |
| Iron | 100 | 15.0 (vanilla) |
| Gold | 130 | 22.5 |
| Emerald | 200 | 40.0 |
| Diamond | 350 | 62.5 |
| Netherite | 600 | 85.0 |

## Special Abilities

| Tier | Ability |
|---|---|
| Copper | Lightning strikes heal instead of damage |
| Gold | +25% movement speed; sprint-dust and sunlight-shine particles |
| Emerald | Heals passively while any villager or wandering trader is within 8 blocks |
| Diamond | Passive LOS lightning zap of nearby hostiles (30–60s cooldown) + on-attack lightning; self-immune to lightning damage |
| Netherite | Fully immune to fire and lava; ignites any mob it hits for 5 seconds |

All ability parameters are configurable per-tier in `config/multigolem.json`.

## Building

```
./gradlew build
```

Output jar lives at `build/libs/multigolem-<version>.jar`.

## Running in dev

```
./gradlew runServer
./gradlew runClient
```

## Configuration

Edit `config/multigolem.json` (created on first server start). Per-tier fields include `max_health`, `attack_damage`, `anger_on_hit`, `ignored_target_types`, and ability-specific toggles and parameters. The global `allow_golem_healing` toggle disables ingot-based healing for all tiers. V1 config files migrate losslessly to V2 schema.

## Roadmap

- **V1** ✅: Variants, stats, drops, healing, anger toggle, config. Server-side only.
- **V2** ✅: Client textures, five special abilities, `ignored_target_types`, lossless V1→V2 config migration.
- **V3** (next): Village natural-spawn variant weighting for Iron Golems. Configurable weights per tier; default `Iron 50% / Copper 20% / Gold 10% / Emerald 14% / Diamond 5% / Netherite 1%`, plus a documented "Charles preset" of all tiers at 19% except Netherite at 5%. Sketched in the V1 design spec (`docs/superpowers/specs/2026-05-15-multigolem-design.md` §3 and §6.1.1). Server-side only; no new items or textures.
- **V4**: Spawn eggs for the 5 Iron Golem variants (Copper, Gold, Emerald, Diamond, Netherite). New items extending the vanilla `SpawnEggItem` pattern; each item's `useOn` spawns a vanilla `IronGolem` and attaches the matching `GolemVariant`. Requires 5 new spawn-egg sprite textures (16×16 PNG, two-color, recolored from the vanilla iron-golem spawn egg) and a creative-tab placement decision (vanilla Spawn Eggs tab vs. custom MultiGolem tab). No new entities or attachments — just items that reuse all V1+V2 golem machinery.
- **V5**: Copper Golem variants — extend the V1+V2 variant pattern to the vanilla `CopperGolem` entity (a separate vanilla entity introduced in MC 26.1.2 with chest-bearing and weathering behavior). Five alternative tiers: Iron, Gold, Emerald, Diamond, Netherite, each spawned by a non-copper body block in the vanilla 1-tall pillar pattern (single block + carved pumpkin). New attachment (`CopperGolemVariant`), parallel mixin into `CarvedPumpkinBlock.trySpawnGolem` for non-copper materials, per-variant stats/textures/abilities. **Open design questions for V5 brainstorming:** do variants inherit the chest-bearing behavior? Do abilities mirror Iron Golem variants or get distinct copper-golem-flavored powers? How do weathering states interact with non-copper materials?

### Where to pick up

For agentic workers (Codex, Claude, etc.) resuming work on this project:

**Read [`docs/LESSONS-LEARNED.md`](docs/LESSONS-LEARNED.md) first.** Every item there cost real time the first time we learned it — read once, save a day later. Covers process patterns (spike first; three-round Codex review; subagent stall modes), technical gotchas (mixin inheritance, layered vanilla AI, config-layer edge cases), and release plumbing details.



- **V3 starting point:** `docs/superpowers/specs/2026-05-15-multigolem-design.md` §3 (V3 scope) and §6.1.1 (config weights including the Charles preset). Follow the same brainstorm → spec → Codex review → plan → execute flow used by V1 and V2.
- **V4 starting point:** no spec yet. Start with a fresh brainstorming session. The vanilla `SpawnEggItem` lives at `net.minecraft.world.item.SpawnEggItem` — see V1's source-inspection spike pattern (`docs/26.1.2-mojang-targets.md`) for how to confirm API specifics before implementation.
- **V5 starting point:** no spec yet. Start with a fresh brainstorming session. The vanilla `CopperGolem` entity is at `net.minecraft.world.entity.animal.golem.CopperGolem` and its spawn flow is documented in `docs/26.1.2-mojang-targets.md` under the "Copper Golem already exists in vanilla 26.1.2" finding.

V3, V4, V5 are independent of each other after V3's natural-spawn ships — they can be released in any order, or interleaved with other work. The recommended order above (V3 → V4 → V5) honors the original V1 roadmap promise first.

## License

MIT — see [LICENSE](LICENSE).
