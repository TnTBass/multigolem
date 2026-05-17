# Changelog

## Unreleased

- Expanded the README with a full `config/multigolem.json` reference for server admins and a shorter public roadmap.
- Updated the Modrinth and CurseForge listing copy to match the README's clearer gameplay and config documentation.

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
