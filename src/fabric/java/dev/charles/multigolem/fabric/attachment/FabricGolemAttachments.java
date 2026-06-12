package dev.charles.multigolem.fabric.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemAbilityState;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.attachment.GolemStorageAdapter;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemIdentityStorage;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class FabricGolemAttachments {
    private static final AttachmentType<GolemIdentity> IDENTITY = AttachmentRegistry
        .<GolemIdentity>builder()
        .persistent(GolemIdentity.CODEC)
        .syncWith(GolemIdentity.STREAM_CODEC, AttachmentSyncPredicate.all())
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "identity"));

    private static final AttachmentType<GolemVariant> VARIANT = AttachmentRegistry
        .<GolemVariant>builder()
        .persistent(GolemVariant.CODEC)
        .syncWith(GolemVariant.STREAM_CODEC, AttachmentSyncPredicate.all())
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant"));

    private static final AttachmentType<GolemSpawnOrigin> SPAWN_ORIGIN = AttachmentRegistry
        .<GolemSpawnOrigin>builder()
        .persistent(GolemSpawnOrigin.CODEC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "spawn_origin"));

    private static final AttachmentType<GolemAbilityState> ABILITY_STATE = AttachmentRegistry
        .<GolemAbilityState>builder()
        .persistent(GolemAbilityState.CODEC)
        .initializer(GolemAbilityState::fresh)
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "ability_state"));

    private FabricGolemAttachments() {}

    public static void register() {
        // Static field initialization registers Fabric attachment types.
        // MultiGolemFabric intentionally creates and registers the common storage adapter before this call,
        // matching the reviewed plan's loader/common initialization order.
    }

    public static GolemStorageAdapter storageAdapter() {
        return new FabricGolemStorageAdapter();
    }

    private static final class FabricGolemStorageAdapter implements GolemStorageAdapter {
        @Override
        public GolemIdentityStorage identityStorage(Entity entity) {
            return new GolemIdentityStorage() {
                @Override
                public Optional<GolemIdentity> rawIdentity() {
                    return Optional.ofNullable(entity.getAttached(IDENTITY));
                }

                @Override
                public void setRawIdentity(GolemIdentity identity) {
                    entity.setAttached(IDENTITY, identity);
                }

                @Override
                public void clearRawIdentity() {
                    entity.removeAttached(IDENTITY);
                }

                @Override
                public Optional<GolemVariant> rawVariant() {
                    return FabricGolemStorageAdapter.this.rawVariant(entity);
                }

                @Override
                public void setRawVariant(GolemVariant variant) {
                    FabricGolemStorageAdapter.this.setRawVariant(entity, variant);
                }

                @Override
                public void clearRawVariant() {
                    FabricGolemStorageAdapter.this.clearRawVariant(entity);
                }
            };
        }

        @Override
        public Optional<GolemVariant> rawVariant(Entity entity) {
            return Optional.ofNullable(entity.getAttached(VARIANT));
        }

        @Override
        public void setRawVariant(Entity entity, GolemVariant variant) {
            entity.setAttached(VARIANT, variant);
        }

        @Override
        public void clearRawVariant(Entity entity) {
            entity.removeAttached(VARIANT);
        }

        @Override
        public GolemSpawnOrigin spawnOrigin(Entity entity) {
            GolemSpawnOrigin attached = entity.getAttached(SPAWN_ORIGIN);
            return attached != null ? attached : GolemSpawnOrigin.UNKNOWN;
        }

        @Override
        public Optional<GolemSpawnOrigin> rawSpawnOrigin(Entity entity) {
            return Optional.ofNullable(entity.getAttached(SPAWN_ORIGIN));
        }

        @Override
        public void setSpawnOrigin(Entity entity, GolemSpawnOrigin origin) {
            if (origin == GolemSpawnOrigin.UNKNOWN) {
                entity.removeAttached(SPAWN_ORIGIN);
                return;
            }
            entity.setAttached(SPAWN_ORIGIN, origin);
        }

        @Override
        public GolemAbilityState abilityState(Entity entity) {
            GolemAbilityState attached = entity.getAttached(ABILITY_STATE);
            return attached != null ? attached : GolemAbilityState.fresh();
        }

        @Override
        public void setAbilityState(Entity entity, GolemAbilityState state) {
            entity.setAttached(ABILITY_STATE, state);
        }
    }
}
