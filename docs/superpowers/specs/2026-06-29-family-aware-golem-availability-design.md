# Family-Aware Golem Availability Design

## Context

Server admins can currently make specific golems practically unavailable through a mix of village spawn weights, zombie village spawning options, and permissions. That is not the same as a first-class disable switch. Variants still exist in static catalog surfaces such as creative spawn eggs, client-facing summaries, player construction checks, and future Golempedia/customization UI.

The availability model must be aware of both golem family and variant because future vanilla copper golems may have their own variant set. A single global `copper` key would be ambiguous once `iron_golem/copper` and `copper_golem/copper` or other copper-family variants can coexist.

## Goals

- Allow server config to disable an entire golem family.
- Allow server config to disable individual variants within an enabled family.
- Keep the model forward-compatible with future `copper_golem` variants.
- Make disabled families or variants unavailable through authoritative server behavior.
- Hide or omit disabled families and variants from modded-client surfaces where the server can communicate that state.
- Preserve already-spawned golems by default.

## Non-Goals

- Do not delete existing world entities when a family or variant is disabled.
- Do not promise that a server-only config can hide pre-join client creative-tab entries without client cooperation.
- Do not collapse family and variant identity into one flat namespace.
- Do not replace the existing permissions system. Availability is a stronger server rule; permissions remain useful for enabled variants.

## Config Shape

Use a family-keyed config with a family enabled flag and variant overrides:

```json
"golem_availability": {
  "iron_golem": {
    "enabled": true,
    "variants": {
      "copper": true,
      "redstone": true,
      "gold": true,
      "emerald": true,
      "diamond": false,
      "netherite": false,
      "zombie": true
    }
  },
  "copper_golem": {
    "enabled": false,
    "variants": {
      "iron": true,
      "gold": true
    }
  }
}
```

Semantics:

- Missing families default to enabled when the catalog supports that family.
- Missing variants default to enabled when the catalog supports that family/variant.
- `family.enabled=false` makes every variant in that family unavailable, regardless of individual variant values.
- Unknown family or variant keys are preserved by config canonicalization when practical, but ignored with a warning.
- Availability should be checked by `GolemIdentity` or an equivalent `(family, variant)` pair, not by `GolemVariant` alone.

## Behavior

Disabled families or variants should be blocked from new creation paths:

- Player-built golem patterns.
- Marked spawn egg spawning.
- Marked spawner setup and spawner-driven variant spawning.
- Village variant rolls.
- Zombie village golem spawning.
- Future family-specific spawn logic.

Disabled families or variants should also be omitted or marked unavailable in modded-client-visible surfaces where the server has a current customization payload:

- Creative spawn egg additions when the client is able to apply server availability.
- Golempedia or equivalent catalog pages.
- Server customization summaries.
- Any future status or help text that lists supported golems.

The server remains authoritative. If a client still has a marked egg, command, copied item stack, stale UI entry, or older cached surface, the server must reject disabled family/variant creation.

## Existing Entities

Existing golems stay in the world. Disabling controls future availability, not cleanup. If cleanup is ever needed, it should be a separate explicit admin action or migration option so config changes do not unexpectedly remove player builds.

## Testing

Focused tests should cover:

- Family disabled overrides enabled variant entries.
- Variant disabled blocks only that family/variant pair.
- Missing config defaults to enabled for known catalog entries.
- Village rolls exclude disabled variants and return empty when all available weights are disabled.
- Player construction and spawn egg spawning reject disabled identities before permissions are considered.
- Client customization summaries omit or mark disabled identities consistently.
