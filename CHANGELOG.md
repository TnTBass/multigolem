# Changelog

## Unreleased

- Fixed disabled golem healing giving misleading visual feedback when players use matching ingots on damaged golems.
- Villages can now naturally spawn MultiGolem variants as defenders.
- Added server config for village spawn weights. Iron, Copper, Gold, and Emerald are common by default, while Diamond and Netherite are rare.
- Existing golems, player-built golems, and command-spawned iron golems are unchanged.
- Server owners can now control who may create or heal each MultiGolem tier using LuckPerms-compatible permission nodes.
- Permissions are permissive by default, so existing servers keep their current creation and healing behavior unless a permissions plugin denies a node.
- Permission checks apply to player-built MultiGolem creation, ingot-based healing, marked spawn egg use, and marked spawn egg spawner configuration; village spawns, commands, unmarked vanilla spawn eggs, and existing golems are unchanged.
- Added Copper, Gold, Emerald, Diamond, and Netherite Golem Spawn Egg variants as marked vanilla iron golem spawn eggs.
- Server permissions for creating each MultiGolem tier now also apply to marked spawn egg use.
- Vanilla iron golem spawn eggs remain unchanged when they are not marked as a MultiGolem variant.
- Marked MultiGolem spawn eggs can configure monster spawners to spawn the matching MultiGolem variant.

## 0.2.2+mc26.1.2 — 2026-05-17

- Fixed waxed and oxidized copper blocks not creating MultiGolem Copper golems in the iron-golem T-pattern.
- Fixed Diamond golems getting stuck with an overlong lightning cooldown after firing once, so they can recharge again within the configured cooldown window.
- Fixed Netherite golems showing the burning state even though fire and lava damage was blocked.
- Increased the default Gold golem movement speed multiplier from `1.25` to `1.75`.

## 0.2.1+mc26.1.2 — 2026-05-17

- Fixed Gold, Emerald, Diamond, and Netherite golems losing their extra health after chunk unloads or server restarts. High-tier golems now keep their configured max HP instead of dropping back to 100 HP.
- Fixed a server watchdog crash that could happen in busy worlds when Gold, Emerald, or Diamond golems checked nearby entities.
- Improved variant textures so Gold reads as brighter metal, Emerald is greener, Diamond looks clean instead of grimy, and Netherite has darker material shading with lava cracks visible from multiple sides.

## 0.2.0+mc26.1.2 — 2026-05-16

- Added client-side variant textures so Copper, Gold, Emerald, Diamond, and Netherite golems are visually distinct in-game.
- Added material abilities:
  - Gold golems move faster and show light particle effects.
  - Emerald golems heal nearby villagers.
  - Copper golems absorb lightning and heal from it.
  - Diamond golems can call lightning in combat and resist lightning damage.
  - Netherite golems resist fire and lava damage and can ignite enemies they hit.
- Added client sync for golem variants so multiplayer clients can render the correct texture.
- Added the first V2 config migration pieces, including persistent ability state and target-filter support for iron golem targeting.
- Fixed release automation issues for Modrinth project description updates and CurseForge uploads.

## 0.1.0+mc26.1.2 — 2026-05-15

- Initial release for Minecraft 26.1.2.
- Added six iron golem variants: Copper, Iron, Gold, Emerald, Diamond, and Netherite.
- Added T-pattern creation using each variant's material block.
- Added per-variant health and attack scaling.
- Added healing with matching ingots, configurable with `allow_golem_healing`.
- Added configurable `anger_on_hit` behavior per variant.
- Added material-specific drops when golems die.
- Vanilla clients can still join servers running the mod.
