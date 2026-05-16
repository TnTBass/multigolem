# MultiGolem — CurseForge Listing

## Features

- 5 new Iron Golem variants: Copper, Gold, Emerald, Diamond, Netherite
- Stats scale with material; netherite kills a Warden in ~6 hits
- T-pattern creation: 4 body blocks + carved pumpkin (same shape as vanilla iron golem)
- Heal each variant with its matching ingot
- Per-tier `anger_on_hit` toggle in config
- Server-side functional; vanilla clients welcome
- Custom textures per variant on modded clients

## Special Abilities (V2)

- **Copper:** lightning strikes heal instead of damage
- **Gold:** +25% movement speed + sprint/sunlight vanity particles
- **Emerald:** heals passively near villagers or wandering traders
- **Diamond:** passive LOS lightning zap (30–60s cooldown) + on-attack lightning; self-immune to lightning; creepers excluded by default
- **Netherite:** fire and lava immune; ignites hit mobs for 5s (skips other netherite golems)

Cross-tier `ignored_target_types` per-tier list (default: creepers on all non-iron tiers to prevent collateral block damage).

## Creation Recipes

Build a T-shape (1 base, 1 center, 2 arms) out of one of:

- Copper Block → Copper Golem
- Gold Block → Gold Golem
- Emerald Block → Emerald Golem
- Diamond Block → Diamond Golem
- Netherite Block → Netherite Golem

Place a carved pumpkin on top. Iron golem recipe is unchanged from vanilla.

## Configuration

Edit `config/multigolem.json` (created on first server start) to tune per-tier `max_health`, `attack_damage`, and `anger_on_hit`. Set `allow_golem_healing: false` to disable ingot-based healing globally.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.148.0+26.1.2

## Known Limitations

- No village natural-spawn variant weighting yet — arriving in V3.
