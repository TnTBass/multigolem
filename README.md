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
- **V3**: Village natural-spawn variant weighting.

## License

MIT — see [LICENSE](LICENSE).
