# MultiGolem

A Fabric mod for Minecraft 26.1.2 that adds Copper, Gold, Emerald, Diamond, and Netherite golem variants alongside the vanilla Iron Golem. Built by Tyler and Charles.

## What it does

- Five new golem tiers, built like an Iron Golem (T-pattern + carved pumpkin) but with a different body block.
- Each tier has a unique texture on modded clients. Vanilla clients see all variants as regular iron golems.
- Stats scale: Copper is weakest, Netherite is strongest (designed to beat a Warden 1v1).
- Heal each golem with its matching ingot.
- Per-tier special abilities (see below).
- Per-tier `ignored_target_types` — copper/gold/emerald/diamond/netherite ignore creepers by default to prevent collateral block damage.
- Marked vanilla iron golem spawn eggs for Copper, Gold, Emerald, Diamond, and Netherite variants.
- **Server-side functional.** Vanilla clients can connect with no mod installed; stats, drops, and creation all behave correctly.

## Recipes

Build a T-shape (1 base block, 1 center, 2 arms) out of one of:

| Body block | Golem |
|---|---|
| Copper Block, including waxed or oxidized variants | Copper Golem |
| Iron Block | Iron Golem (vanilla, unchanged) |
| Gold Block | Gold Golem |
| Emerald Block | Emerald Golem |
| Diamond Block | Diamond Golem |
| Netherite Block | Netherite Golem |

Place a carved pumpkin on top. Waxed and oxidized copper blocks work for MultiGolem's iron-golem-style Copper Golem. The vanilla single-copper-block + pumpkin recipe still spawns the vanilla Copper Golem (with its chest behavior).

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
| Gold | +75% movement speed; sprint-dust and sunlight-shine particles |
| Emerald | Heals passively while any villager or wandering trader is within 8 blocks |
| Diamond | Passive LOS lightning zap of nearby hostiles (30–60s cooldown) + on-attack lightning; self-immune to lightning damage |
| Netherite | Fully immune to fire and lava; ignites any mob it hits for 5 seconds |

All ability parameters are configurable per-tier in `config/multigolem.json`.

## Configuration

Edit `config/multigolem.json`, created on first server start. Restart the server after changing it.

Existing V1 config files migrate automatically to the V2 schema. Unknown fields are preserved where possible, so server owners can keep notes or future fields in the file.

Top-level fields:

| Field | Default | What it does |
|---|---:|---|
| `allow_golem_healing` | `true` | Enables or disables ingot-based healing for all golem tiers. |

Each tier lives under `tiers.<tier_id>`, where `tier_id` is one of `copper`, `iron`, `gold`, `emerald`, `diamond`, or `netherite`.

Shared per-tier fields:

| Field | Default range | What it does |
|---|---:|---|
| `max_health` | `1`-`2048` | Maximum HP for that tier. |
| `attack_damage` | `0`-`2048` | Attack damage for that tier. |
| `anger_on_hit` | `true` / `false` | Whether that tier gets angry when attacked. |
| `ignored_target_types` | list | Target categories this tier will not attack. Recognized values: `CREEPERS`, `ENDERMEN`, `PLAYERS`, `ALL_BOSSES`. Copper, Gold, Emerald, Diamond, and Netherite ignore creepers by default. |

Ability fields:

| Tier | Field | Default | What it does |
|---|---|---:|---|
| Copper | `copper_lightning_immune` | `true` | Lightning heals Copper golems instead of damaging them. |
| Copper | `copper_lightning_heal_amount` | `null` | HP restored by lightning. `null` means heal to full. |
| Gold | `gold_speed_multiplier` | `1.75` | Movement speed multiplier. |
| Gold | `gold_sprint_particles_enabled` | `true` | Enables sprint-dust particles while moving. |
| Gold | `gold_sunlight_shine_enabled` | `true` | Enables sunlight-shine particles while idle outdoors. |
| Emerald | `emerald_aura_range` | `8` | Block radius for finding villagers and wandering traders. |
| Emerald | `emerald_heal_interval_seconds` | `2.0` | Seconds between passive healing pulses. Minimum `0.5`. |
| Emerald | `emerald_heal_per_tick` | `1.0` | HP restored per healing pulse. |
| Emerald | `emerald_count_wandering_traders` | `true` | Whether wandering traders count for the Emerald healing aura. |
| Diamond | `diamond_target_mode` | `ALL_HOSTILE_MOBS` | Passive lightning targeting mode. Values: `ALL_HOSTILE_MOBS`, `ALL_HOSTILE_MOBS_AND_PLAYERS`, `BOSSES_ONLY`, `NONE`. |
| Diamond | `diamond_cooldown_min_seconds` | `30` | Minimum passive lightning cooldown. |
| Diamond | `diamond_cooldown_max_seconds` | `60` | Maximum passive lightning cooldown. |
| Diamond | `diamond_aura_range` | `12` | Block radius for passive lightning target scans. |
| Diamond | `diamond_lightning_proof` | `true` | Makes Diamond golems immune to lightning damage. |
| Netherite | `netherite_fire_immune` | `true` | Makes Netherite golems immune to fire and lava damage. |
| Netherite | `netherite_ignite_seconds` | `5` | Seconds of fire applied to mobs hit by Netherite golems. Set to `0` to disable ignite-on-hit. |

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

These nodes affect player-built MultiGolem T-pattern creation, ingot-based golem healing, marked spawn egg use, and marked spawn egg spawner configuration. Village spawns, commands, unmarked vanilla spawn eggs, unmarked vanilla mob spawners, existing golems, drops, stats, abilities, targeting, and anger behavior are unchanged.

## Roadmap

- **V1** ✅: Variants, stats, drops, healing, anger toggle, config. Server-side only.
- **V2** ✅: Client textures, five special abilities, `ignored_target_types`, lossless V1→V2 config migration.
- **V3** ✅: Village natural-spawn variant weighting.
- **V3.1** ✅: LuckPerms-compatible permission nodes for creation and healing.
- **V4** ✅: Spawn eggs for the 5 Iron Golem variants.
- **V5**: Copper Golem variants.

## License

MIT — see [LICENSE](LICENSE).
