# MultiGolem Permissions — Design

**Date:** 2026-05-17
**Status:** Draft, approved for spec write-up
**Target phase:** V3.1 permissions phase
**Dependency direction:** integrate through `fabric-permissions-api`, not direct LuckPerms APIs

## 1. Summary

This phase adds optional server permissions for player actions in MultiGolem.

Servers that use LuckPerms can control who may create and heal MultiGolem variants. MultiGolem talks to the permissions ecosystem through `fabric-permissions-api`, so LuckPerms remains the expected provider without hard-coupling the mod to LuckPerms internals.

Permissions are permissive by default. Existing servers keep current behavior unless a permissions provider explicitly denies a node.

## 2. Goals And Non-Goals

**Goals**

- Add permission checks for player-built MultiGolem T-pattern creation.
- Add permission checks for ingot-based golem healing.
- Support LuckPerms through `fabric-permissions-api`.
- Keep behavior permissive by default when no permission provider is installed.
- Send a short player-facing denial message when a player lacks permission.
- Add docs and playtest rows for permission behavior.

**Non-goals**

- No direct LuckPerms API integration.
- No permission checks for village natural spawns.
- No permission checks for drops, stats, abilities, targeting, anger, existing golems, mob spawners, spawn eggs, or commands.
- No custom group or role system in MultiGolem config.
- No per-world, per-biome, or per-village permission rules.

## 3. Permission Nodes

Creation permissions:

| Node | Meaning |
|---|---|
| `multigolem.create.copper` | Build Copper MultiGolem T-patterns. |
| `multigolem.create.gold` | Build Gold MultiGolem T-patterns. |
| `multigolem.create.emerald` | Build Emerald MultiGolem T-patterns. |
| `multigolem.create.diamond` | Build Diamond MultiGolem T-patterns. |
| `multigolem.create.netherite` | Build Netherite MultiGolem T-patterns. |

Healing permissions:

| Node | Meaning |
|---|---|
| `multigolem.heal.copper` | Heal Copper golems with copper ingots. |
| `multigolem.heal.iron` | Heal Iron golems with iron ingots. |
| `multigolem.heal.gold` | Heal Gold golems with gold ingots. |
| `multigolem.heal.emerald` | Heal Emerald golems with emeralds. |
| `multigolem.heal.diamond` | Heal Diamond golems with diamonds. |
| `multigolem.heal.netherite` | Heal Netherite golems with netherite ingots. |

Admin bypass:

| Node | Meaning |
|---|---|
| `multigolem.admin.bypass` | Bypass MultiGolem creation and healing permission restrictions. |

Iron creation has no node because vanilla iron T-pattern creation remains vanilla-owned. MultiGolem does not intercept or permission-gate vanilla iron golem creation.

## 4. Default Behavior

Permission checks default to `true`.

This means:

- A server with no permissions provider behaves exactly like current MultiGolem.
- A server with LuckPerms installed can explicitly deny nodes.
- A missing node does not block the player.
- `multigolem.admin.bypass` grants access even if a tier-specific node is denied.

This compatibility rule is release-blocking. Permissions must not surprise existing servers by making golem creation or healing restrictive after upgrade.

## 5. Architecture

Add a small permissions package:

| Component | Expected path | Job |
|---|---|---|
| `MultiGolemPermissions` | `src/main/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java` | Builds permission node names, calls `fabric-permissions-api`, applies permissive defaults and bypass behavior. |
| `PermissionMessages` or inline helper | same package or same file | Sends denial messages consistently. Keep separate only if message code grows. |

Expected public helper methods:

```java
public static boolean canCreate(ServerPlayer player, GolemVariant variant);
public static boolean canHeal(Player player, GolemVariant variant);
public static void sendCreateDenied(ServerPlayer player, GolemVariant variant);
public static void sendHealDenied(Player player, GolemVariant variant);
```

The helper should be the only place that knows permission node strings. Mixins and handlers should not manually assemble node names.

## 6. Data Flow

### 6.1 Player-built variant creation

1. Player places a carved pumpkin or jack-o-lantern.
2. `CarvedPumpkinBlockMixin` calls `GolemCreationHandler.trySpawnVariant(...)`.
3. `GolemCreationHandler` finds a non-iron MultiGolem T-pattern.
4. Before clearing blocks or spawning the golem, determine the nearest responsible player using the existing vanilla trigger context available from the block placement path if possible.
5. If a responsible `ServerPlayer` is available and `canCreate(player, variant)` is false:
   - do not clear blocks.
   - do not spawn a golem.
   - send a denial message.
   - cancel vanilla only if the matched MultiGolem T-pattern would otherwise be consumed incorrectly; preserve block state.
6. If permission passes or no responsible player can be safely identified, current behavior proceeds.

The implementation plan must spike the exact creation-context shape before coding. The current V1 creation path starts from `CarvedPumpkinBlock.trySpawnGolem(Level, BlockPos)`, which may not expose the placing player. If no responsible player can be identified from this path, the plan must choose between:

- tracking recent pumpkin placement by player and position, or
- only gating creation when a player context is available.

Do not add a broad global event fallback without review.

### 6.2 Healing

1. Player right-clicks an `IronGolem`.
2. `IronGolemMixin#mobInteract` identifies the golem variant and matching ingot.
3. Before healing or consuming the item, call `canHeal(player, variant)`.
4. If denied:
   - send a denial message.
   - return an interaction result that prevents vanilla healing feedback and item consumption.
5. If allowed, current healing behavior proceeds.

Healing permission checks apply to Iron as well as non-iron variants because MultiGolem already controls global `allow_golem_healing` behavior for iron-ingot interactions.

## 7. Denial Messages

Messages should be short and practical:

- Creation: `You do not have permission to create a Diamond golem.`
- Healing: `You do not have permission to heal a Netherite golem.`

Use variant display names derived from `GolemVariant`, not hardcoded in multiple call sites. Prefer actionbar if simple and available; chat is acceptable if that better matches the current Minecraft API surface.

No message is sent for non-player causes because the phase only gates player actions.

## 8. Dependency And Compatibility

Use `fabric-permissions-api`.

Expected Gradle shape should be verified during implementation planning against current Fabric API packaging:

- If `fabric-permissions-api` is bundled in the current Fabric API dependency, use it directly.
- If it needs a separate module dependency, add only the narrow module dependency required.

The mod must not require LuckPerms specifically. LuckPerms is the expected server provider, but any provider compatible with Fabric permissions should work.

If the permissions API is absent or no provider answers a node, checks default to `true`.

## 9. Testing

Pure helpers get failing tests first:

- Node names are exactly `multigolem.create.<tier>` and `multigolem.heal.<tier>`.
- `multigolem.admin.bypass` is checked before tier-specific denial.
- Missing provider or missing node returns the permissive default.
- Denial returns false only when a provider explicitly denies the node.
- Variant display names used in denial messages are stable.

Integration/build verification:

- Build passes with the permissions dependency.
- Server starts without LuckPerms installed.
- Server starts with LuckPerms installed.
- Denying `multigolem.create.diamond` prevents Diamond T-pattern creation and leaves blocks intact.
- Granting `multigolem.create.diamond` allows Diamond T-pattern creation.
- Denying `multigolem.heal.netherite` prevents Netherite healing and item consumption.
- Granting `multigolem.heal.netherite` allows healing.
- `multigolem.admin.bypass` allows both actions even when tier-specific nodes are denied.

## 10. Docs And Release Notes

Update:

- `README.md` config/admin docs.
- `docs/modrinth-listing.md`.
- `docs/curseforge-listing.md`.
- `docs/playtest-checklist.md`.
- `docs/playtest.html`.
- `CHANGELOG.md` under `## Unreleased`.

Release-note wording should be player/admin-facing:

- Server owners can now control who may create or heal each MultiGolem tier using LuckPerms-compatible permission nodes.
- Existing servers remain permissive by default unless a permissions plugin denies a node.

## 11. Open Items For Implementation Planning

- Verify the exact `fabric-permissions-api` package, dependency, and check method signatures for Minecraft 26.1.2 / current Fabric API.
- Spike how to identify the responsible player for T-pattern creation from the current `CarvedPumpkinBlock` mixin path.
- Decide whether denial messages should use actionbar or chat after checking available API names.
- Confirm the correct `InteractionResult` for denied healing so vanilla repair feedback and item consumption do not occur.
