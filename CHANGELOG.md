# Changelog

## Unreleased

- Fixed golem HP being cut to 100 on chunk reload for Gold, Emerald, Diamond, and Netherite variants — caused by MAX_HEALTH and ATTACK_DAMAGE modifiers using addTransientModifier (not saved to NBT) instead of addPermanentModifier.
- Fixed a server watchdog crash caused by Diamond, Gold, and Emerald golem abilities scanning too many entities each tick.
- Improved variant textures with clearer material details: copper patina, brighter gold, diamond facets, netherite ember glows, and emerald gem accents.
- Fixed the GitHub Actions release build so generated textures can be rebuilt on clean runners.
- Added a maintenance workflow for updating the Modrinth project listing without publishing a new mod version.
- Added a release-notes style gate so future changelogs stay focused on players and server admins instead of development-task history.
- Fixed the changelog gate so generated texture files do not cause false release-blocking failures during CI builds.

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
