# MultiGolem

A Fabric mod for Minecraft 26.1.2 that adds Copper, Gold, Emerald, Diamond, and Netherite golem variants alongside the vanilla Iron Golem. Built by Tyler and Charles.

## What it does

- Five new golem tiers, built like an Iron Golem (T-pattern + carved pumpkin) but with a different body block.
- Stats scale: Copper is weakest, Netherite is strongest (designed to beat a Warden 1v1).
- Heal each golem with its matching ingot.
- Per-tier configuration: HP, attack damage, whether the tier retaliates when hit.
- **Server-side functional.** Vanilla clients can connect with no mod installed and see all variants as regular iron golems; stats, drops, and creation all behave correctly.

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

Place a carved pumpkin on top. The vanilla single-copper-block + pumpkin recipe also still spawns the vanilla Copper Golem (with its chest behavior).

## Stats (defaults)

| Tier | Max HP | Avg Atk Damage |
|---|---:|---:|
| Copper | 60 | 8.5 |
| Iron | 100 | 15.0 (vanilla) |
| Gold | 130 | 22.5 |
| Emerald | 200 | 40.0 |
| Diamond | 350 | 62.5 |
| Netherite | 600 | 85.0 |

All values are configurable per-tier in `config/multigolem.json`.

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

Edit `config/multigolem.json` (created on first server start). Per-tier `max_health`, `attack_damage`, `anger_on_hit`, and the global `allow_golem_healing` toggle.

## Roadmap

- **V1** (this release): Variants, stats, drops, healing, anger toggle, config. Server-side only.
- **V2**: Client texture, special abilities (copper lightning heal, gold speed, emerald villager heal, diamond lightning, netherite fire immunity + ignite).
- **V3**: Village natural-spawn variant weighting.

See [`docs/superpowers/specs/2026-05-15-multigolem-design.md`](docs/superpowers/specs/2026-05-15-multigolem-design.md) for the full design.

## License

MIT.
