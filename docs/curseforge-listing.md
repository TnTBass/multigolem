# MultiGolem — CurseForge Listing

## Features

- 5 new Iron Golem variants: Copper, Gold, Emerald, Diamond, Netherite
- Stats scale with material; netherite kills a Warden in ~6 hits
- T-pattern creation: 4 body blocks + carved pumpkin (same shape as vanilla iron golem)
- Heal each variant with its matching ingot
- Per-tier `anger_on_hit` toggle in config
- Server-side functional; vanilla clients welcome

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

## V1 Limitations

- No client texture yet — all variants visually appear as iron golems on modded and vanilla clients alike. Custom textures coming in V2.
- No special abilities yet — they arrive in V2 (copper-lightning-heal, gold-speed, emerald-villager-heal, diamond-lightning, netherite-fireproof + ignite).
- No village natural-spawn variants yet — arriving in V3.
