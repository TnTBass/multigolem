# Fabric Touchpoints

## Entrypoints

- `dev.charles.multigolem.MultiGolem` implements Fabric `ModInitializer`.
- `dev.charles.multigolem.client.MultiGolemClient` implements Fabric `ClientModInitializer`.

## Loader Metadata And Config

- `MultiGolem` uses `FabricLoader.getConfigDir()`.
- `MultiGolemStatus` uses Fabric metadata for current version lookup.

## Attachments

- `GolemIdentityAttachment`
- `GolemVariantAttachment`
- `GolemSpawnOriginAttachment`
- `GolemAbilityStateAttachment`

## Events

- `ServerEntityEvents`
- `CreativeModeTabEvents`
- `LootTableEvents`
- `ServerTickEvents`
- `ServerLivingEntityEvents`

## Networking

- `MultiGolemStatusNetworking`
- `ServerCustomizationsNetworking`
- `MultiGolemStatusClient`
- `ServerCustomizationsClient`

## Permissions

- `MultiGolemPermissions` currently imports `me.lucko.fabric.api.permissions.v0.Permissions`.

## ModMenu

- `MultiGolemModMenu`
- Shared ModMenu screens under `src/client/java/dev/charles/multigolem/client/modmenu`

## Mixins And Resources

- `fabric.mod.json`
- `multigolem.mixins.json`
- `multigolem.client.mixins.json`
