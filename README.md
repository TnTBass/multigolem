# MultiGolem

A Fabric and NeoForge mod for Minecraft 26.2 that adds Copper, Redstone, Gold, Lapis, Emerald, Diamond, Netherite, and Zombie golem variants alongside the vanilla Iron Golem. Built by Tyler and Charles.

## What it does

- Eight new golem tiers, built like an Iron Golem (T-pattern + carved pumpkin) but with a different body block.
- Each tier has a unique texture on modded clients. Vanilla clients see all variants as regular iron golems.
- Each variant has its own default stats and role: Copper is lighter than vanilla, Redstone trades raw strength for emergency-control effects, Lapis is fragile anti-magic support, and Netherite is the durable endgame bruiser.
- Heal each golem with its matching material.
- Per-tier special abilities (see below).
- Per-tier `ignored_target_types` — copper/redstone/gold/lapis/emerald/diamond/netherite ignore creepers by default to prevent collateral block damage.
- Hostile Zombie Golems built from Mossy Cobblestone, healed with Rotten Flesh, allied with zombies, and maintained in zombie-villager village areas.
- Marked vanilla iron golem spawn eggs for Copper, Redstone, Gold, Lapis, Emerald, Diamond, Netherite, and Zombie variants.
- **Server-side functional.** Vanilla clients can connect with no mod installed; stats, drops, and creation all behave correctly.

## Recipes

Build a T-shape (1 base block, 1 center, 2 arms) out of one of:

| Body block | Golem |
|---|---|
| Copper Block, including fresh, exposed, weathered, oxidized, and waxed variants | Copper Iron Golem |
| Iron Block | Iron Golem (vanilla, unchanged) |
| Redstone Block | Redstone Golem |
| Gold Block | Gold Golem |
| Lapis Block | Lapis Golem |
| Emerald Block | Emerald Golem |
| Diamond Block | Diamond Golem |
| Netherite Block | Netherite Golem |
| Mossy Cobblestone | Zombie Golem |

Place a carved pumpkin on top. Copper Iron Golems preserve the fresh, exposed, weathered, oxidized, and waxed state of the copper blocks used to build them, with matching textures on modded clients. The vanilla single-copper-block + pumpkin recipe still spawns the vanilla Copper Golem (with its chest behavior).

## Stats (defaults)

| Tier | Max HP | Avg Atk Damage |
|---|---:|---:|
| Copper | 60 | 8.5 |
| Iron | 100 | 15.0 (vanilla) |
| Redstone | 90 | 13.0 |
| Gold | 130 | 22.5 |
| Lapis | 50 | 7.5 |
| Emerald | 200 | 40.0 |
| Diamond | 350 | 62.5 |
| Netherite | 600 | 85.0 |
| Zombie | 100 | 15.0 |

## Special Abilities

| Tier | Ability |
|---|---|
| Copper | Lightning strikes heal instead of damage |
| Redstone | Overcharges at or below 25% health for attack and resistance without speed; releases a Slowness X overload pulse on death |
| Gold | +75% movement speed; sprint-dust and sunlight-shine particles |
| Lapis | Protects nearby allied village entities from magic damage and configured harmful magical effects; player protection is disabled by default |
| Emerald | Heals passively while any villager or wandering trader is within 8 blocks |
| Diamond | Passive LOS lightning zap of nearby hostiles (30–60s cooldown) + on-attack lightning; self-immune to lightning damage |
| Netherite | Fully immune to fire and lava; ignites any mob it hits for 5 seconds |
| Zombie | Hostile corrupted golem; heals from Rotten Flesh, converts civilians into zombie villagers, applies sickness to players, fights village defenders, and stays allied with zombies |

All ability parameters are configurable per-tier in `config/multigolem.json`.

## Configuration

Edit `config/multigolem.json`, created on first server start. Restart the server after changing it.

Existing V1 config files migrate automatically to the V2 schema. Unknown fields are preserved where possible, so server owners can keep notes or future fields in the file.

Top-level fields:

| Field | Default | What it does |
|---|---:|---|
| `allow_golem_healing` | `true` | Enables or disables ingot-based healing for all golem tiers. |
| `golem_availability` | object | Controls which golem families and variants can be created or shown in server-known listing surfaces. It does not delete existing golems. |
| `village_spawning` | object | Controls villager-called MultiGolem village defenders. |
| `zombie_village_spawning` | object | Maintains Zombie Golems near zombie-villager village areas. |
| `tiers` | object | Per-tier stats, targeting, and ability settings. |

### Golem availability

`golem_availability` lets server owners turn off future creation for whole golem families or individual family/variant identities. Disabled golems are also removed from server-known listing surfaces such as server customizations and Golempedia data sent by the server. Existing spawned golems are not deleted.

Known family keys:

| Family key | Current variants |
|---|---|
| `iron_golem` | `copper`, `iron`, `redstone`, `gold`, `lapis`, `emerald`, `diamond`, `netherite`, `zombie` |
| `copper_golem` | Reserved for future copper-golem-family support; no current MultiGolem variants. |

Each family has:

| Field | Default | What it does |
|---|---:|---|
| `enabled` | `true` | Enables or disables the whole family. A disabled family overrides enabled variant entries. |
| `variants` | all known variants `true` | Per-variant availability. Missing known variants default to enabled. Unknown variant keys are preserved where possible but ignored. |

Example:

```json
{
  "golem_availability": {
    "iron_golem": {
      "enabled": true,
      "variants": {
        "netherite": false,
        "zombie": false
      }
    }
  }
}
```

Availability is checked before permissions. If a golem is unavailable, permission grants do not make it creatable.

### Village spawning

`village_spawning` controls villager-called MultiGolem defenders. It does not affect player-built golems, marked spawn eggs, spawners, existing golems, or Zombie Golem village maintenance.

| Field | Default | What it does |
|---|---:|---|
| `enabled` | `true` | Enables MultiGolem variant selection for villager-called iron golem spawns. If disabled, village spawns remain vanilla Iron. |
| `weights.iron` | `19` | Village-spawn roll weight for Iron Golems. |
| `weights.copper` | `19` | Village-spawn roll weight for Copper Iron Golems. |
| `weights.redstone` | `19` | Village-spawn roll weight for Redstone Golems. |
| `weights.gold` | `19` | Village-spawn roll weight for Gold Golems. |
| `weights.lapis` | `5` | Village-spawn roll weight for Lapis Golems. |
| `weights.emerald` | `19` | Village-spawn roll weight for Emerald Golems. |
| `weights.diamond` | `5` | Village-spawn roll weight for Diamond Golems. |
| `weights.netherite` | `0` | Village-spawn roll weight for Netherite Golems. Netherite is opt-in because villages are flammable. |

Weights below `0` are clamped to `0`. If all available weights are `0`, villager-called spawns fall back to vanilla behavior.

### Zombie village spawning

`zombie_village_spawning` maintains Zombie Golems around zombie-villager-heavy village areas. It is separate from normal `village_spawning`.

| Field | Default | What it does |
|---|---:|---|
| `enabled` | `true` | Enables Zombie Golem village maintenance. |
| `min_zombie_villagers` | `1` | Minimum nearby zombie villagers before maintenance can request Zombie Golems. |
| `zombie_villagers_per_golem` | `5` | Adds desired Zombie Golems as zombie villager count rises. |
| `regular_zombie_bonus_enabled` | `true` | Allows regular zombies to add one extra desired Zombie Golem. |
| `regular_zombie_bonus_threshold` | `3` | Regular zombie count needed for the bonus. |
| `max_zombie_golems_per_village` | `2` | Maximum maintained Zombie Golems per village area. |

Each tier lives under `tiers.<tier_id>`, where `tier_id` is one of `copper`, `iron`, `redstone`, `gold`, `lapis`, `emerald`, `diamond`, `netherite`, or `zombie`.

Shared per-tier fields:

| Field | Default range | What it does |
|---|---:|---|
| `max_health` | `1`-`2048` | Maximum HP for that tier. |
| `attack_damage` | `0`-`2048` | Attack damage for that tier. |
| `anger_on_hit` | `true` / `false` | Whether that tier gets angry when attacked. |
| `ignored_target_types` | list | Target categories this tier will not attack. Recognized values: `CREEPERS`, `ENDERMEN`, `PLAYERS`, `ALL_BOSSES`. Copper, Redstone, Gold, Lapis, Emerald, Diamond, and Netherite ignore creepers by default. |

Ability fields:

| Tier | Field | Default | What it does |
|---|---|---:|---|
| Copper | `copper_lightning_immune` | `true` | Lightning heals Copper golems instead of damaging them. |
| Copper | `copper_lightning_heal_amount` | `null` | HP restored by lightning. `null` means heal to full. |
| Redstone | `redstone_overcharge_enabled` | `true` | Enables emergency overcharge when health crosses at or below the configured threshold. |
| Redstone | `redstone_overcharge_health_threshold_percent` | `0.25` | Health percentage threshold for starting overcharge. |
| Redstone | `redstone_overcharge_duration_seconds` | `12.0` | Seconds an overcharge remains active. |
| Redstone | `redstone_overcharge_cooldown_seconds` | `45.0` | Seconds before a new overcharge can trigger after expiry. |
| Redstone | `redstone_overcharge_attack_multiplier` | `1.5` | Attack multiplier while overcharged. |
| Redstone | `redstone_overcharge_resistance_amplifier` | `1` | Resistance effect amplifier while overcharged. `1` is Resistance II. |
| Redstone | `redstone_overcharge_resistance_refresh_seconds` | `3.0` | Resistance refresh duration while overcharged. |
| Redstone | `redstone_death_pulse_enabled` | `true` | Enables the Slowness death overload pulse. |
| Redstone | `redstone_death_pulse_radius` | `8` | Death pulse radius in blocks. |
| Redstone | `redstone_death_pulse_slowness_seconds` | `6.0` | Slowness duration applied by the death pulse. |
| Redstone | `redstone_death_pulse_slowness_amplifier` | `9` | Slowness effect amplifier. `9` is Slowness X. |
| Redstone | `redstone_particles_enabled`, `redstone_death_pulse_particles_enabled` | `true` | Enables overcharge and death pulse particles. |
| Gold | `gold_speed_multiplier` | `1.75` | Movement speed multiplier. |
| Gold | `gold_sprint_particles_enabled` | `true` | Enables sprint-dust particles while moving. |
| Gold | `gold_sunlight_shine_enabled` | `true` | Enables sunlight-shine particles while idle outdoors. |
| Lapis | `lapis_ward_enabled` | `true` | Enables the Lapis ward. |
| Lapis | `lapis_ward_range` | `15` | Ward range in blocks. Valid range: `1`-`64`. |
| Lapis | `lapis_ward_scan_interval_ticks` | `5` | Ticks between ward cleanup scans. Valid range: `1`-`200`. |
| Lapis | `lapis_ward_affects_players` | `false` | Whether Lapis wards protect players. Disabled by default. |
| Lapis | `lapis_ward_magic_damage_enabled` | `true` | Blocks magic and indirect magic damage for protected targets in range. |
| Lapis | `lapis_ward_effect_cleanup_enabled` | `true` | Removes configured harmful effects from protected targets in range and blocks new matching effect applications. |
| Lapis | `lapis_ward_effect_ids` | default list | Namespaced mob effect IDs cleaned up by the ward. Defaults to `minecraft:poison`, `minecraft:wither`, `minecraft:weakness`, `minecraft:slowness`, `minecraft:blindness`, `minecraft:nausea`, `minecraft:levitation`, `minecraft:darkness`, and `minecraft:mining_fatigue`. |
| Lapis | `lapis_particles_enabled` | `true` | Enables Lapis ward particles. |
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
| Netherite | `netherite_village_ignite_seconds` | `0` | Seconds of fire applied by village-spawned Netherite golems. Village-spawned Netherite golems avoid ignite-on-hit by default so opt-in Netherite village defenders do not start fires unless configured. |
| Zombie | `zombie_rotten_flesh_heal_amount` | `25.0` | HP restored when healing a Zombie Golem with Rotten Flesh. |
| Zombie | `zombie_hunger_enabled`, `zombie_hunger_seconds`, `zombie_hunger_amplifier` | `true`, `12`, `0` | Hunger applied to players hit by Zombie Golems. |
| Zombie | `zombie_nausea_enabled`, `zombie_nausea_seconds`, `zombie_nausea_amplifier` | `true`, `4`, `0` | Nausea applied to players hit by Zombie Golems. |
| Zombie | `zombie_poison_enabled`, `zombie_poison_seconds`, `zombie_poison_amplifier` | `true`, `4`, `0` | Poison applied to players hit by Zombie Golems. |
| Zombie | `zombie_convert_villagers_enabled`, `zombie_villager_conversion_chance` | `true`, `1.0` | Villager conversion to zombie villagers. Failed rolls deal no normal Iron Golem damage. |
| Zombie | `zombie_convert_wandering_traders_enabled`, `zombie_wandering_trader_conversion_chance` | `true`, `1.0` | Wandering trader conversion to zombie villagers. Trader llamas are not special-cased. |

### Permissions

MultiGolem supports LuckPerms-compatible permissions through Fabric Permissions API. Permissions are permissive by default: existing servers keep working unless a permissions provider explicitly denies a node.

Creation permissions:

- `multigolem.create.copper`
- `multigolem.create.redstone`
- `multigolem.create.gold`
- `multigolem.create.lapis`
- `multigolem.create.emerald`
- `multigolem.create.diamond`
- `multigolem.create.netherite`
- `multigolem.create.zombie`

Healing permissions:

- `multigolem.heal.copper`
- `multigolem.heal.iron`
- `multigolem.heal.redstone`
- `multigolem.heal.gold`
- `multigolem.heal.lapis`
- `multigolem.heal.emerald`
- `multigolem.heal.diamond`
- `multigolem.heal.netherite`
- `multigolem.heal.zombie`

Bypass:

- `multigolem.admin.bypass`

These nodes affect player-built MultiGolem T-pattern creation, ingot-based golem healing, marked spawn egg use, and marked spawn egg spawner configuration. Village spawns, commands, unmarked vanilla spawn eggs, unmarked vanilla mob spawners, existing golems, drops, stats, abilities, targeting, and anger behavior are unchanged.

## Roadmap

- **V1** ✅: Variants, stats, drops, healing, anger toggle, config. Server-side only.
- **V2** ✅: Client textures, five special abilities, `ignored_target_types`, lossless V1→V2 config migration.
- **V3** ✅: Village natural-spawn variant weighting.
- **V3.1** ✅: LuckPerms-compatible permission nodes for creation and healing.
- **V4** ✅: Spawn eggs for the Iron Golem variants.
- **V5** ✅: Zombie Golem, with hostile Mossy Cobblestone golems, Rotten Flesh healing, sickness effects, civilian conversion, marked eggs, and zombie-village maintenance.
- **V6** ✅: NeoForge support.
- **V7** ✅: Redstone Golem, a lower-strength emergency-control defender that overcharges at or below 25% health, gains attack/resistance without speed, and releases a Slowness X death overload pulse.
- **V8** ✅: Lapis Golem, a fragile anti-magic support defender that protects nearby allied village entities from magic damage and configured harmful magical effects.
- **V8.1** ✅: Server-side golem availability config for disabling specific golem types or whole golem families.
- **V9**: Copper Golem variants.

## License

MIT — see [LICENSE](LICENSE).
