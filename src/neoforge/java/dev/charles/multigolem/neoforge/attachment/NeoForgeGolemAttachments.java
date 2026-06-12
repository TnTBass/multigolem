package dev.charles.multigolem.neoforge.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemAbilityState;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.attachment.GolemStorageAdapter;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemIdentityStorage;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;

public final class NeoForgeGolemAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MultiGolem.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<GolemIdentity>> IDENTITY =
        ATTACHMENTS.register("identity", () -> AttachmentType.builder(GolemIdentity::defaultIron)
            .serialize(GolemIdentity.CODEC.fieldOf("value"))
            .sync(GolemIdentity.STREAM_CODEC)
            .build());

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<GolemVariant>> VARIANT =
        ATTACHMENTS.register("variant", () -> AttachmentType.builder(() -> GolemVariant.IRON)
            .serialize(GolemVariant.CODEC.fieldOf("value"))
            .sync(GolemVariant.STREAM_CODEC)
            .build());

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<GolemSpawnOrigin>> SPAWN_ORIGIN =
        ATTACHMENTS.register("spawn_origin", () -> AttachmentType.builder(() -> GolemSpawnOrigin.UNKNOWN)
            .serialize(GolemSpawnOrigin.CODEC.fieldOf("value"), origin -> origin != GolemSpawnOrigin.UNKNOWN)
            .build());

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<GolemAbilityState>> ABILITY_STATE =
        ATTACHMENTS.register("ability_state", () -> AttachmentType.builder(GolemAbilityState::fresh)
            .serialize(GolemAbilityState.CODEC.fieldOf("value"), state -> !GolemAbilityState.fresh().equals(state))
            .build());

    private NeoForgeGolemAttachments() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static GolemStorageAdapter storageAdapter() {
        return new NeoForgeGolemStorageAdapter();
    }

    private static final class NeoForgeGolemStorageAdapter implements GolemStorageAdapter {
        @Override
        public GolemIdentityStorage identityStorage(Entity entity) {
            return new GolemIdentityStorage() {
                @Override
                public Optional<GolemIdentity> rawIdentity() {
                    return entity.getExistingData(IDENTITY);
                }

                @Override
                public void setRawIdentity(GolemIdentity identity) {
                    entity.setData(IDENTITY, identity);
                    entity.syncData(IDENTITY);
                }

                @Override
                public void clearRawIdentity() {
                    entity.removeData(IDENTITY);
                    entity.syncData(IDENTITY);
                }

                @Override
                public Optional<GolemVariant> rawVariant() {
                    return NeoForgeGolemStorageAdapter.this.rawVariant(entity);
                }

                @Override
                public void setRawVariant(GolemVariant variant) {
                    NeoForgeGolemStorageAdapter.this.setRawVariant(entity, variant);
                }

                @Override
                public void clearRawVariant() {
                    NeoForgeGolemStorageAdapter.this.clearRawVariant(entity);
                }
            };
        }

        @Override
        public Optional<GolemVariant> rawVariant(Entity entity) {
            return entity.getExistingData(VARIANT);
        }

        @Override
        public void setRawVariant(Entity entity, GolemVariant variant) {
            entity.setData(VARIANT, variant);
            entity.syncData(VARIANT);
        }

        @Override
        public void clearRawVariant(Entity entity) {
            entity.removeData(VARIANT);
            entity.syncData(VARIANT);
        }

        @Override
        public GolemSpawnOrigin spawnOrigin(Entity entity) {
            return entity.getExistingData(SPAWN_ORIGIN).orElse(GolemSpawnOrigin.UNKNOWN);
        }

        @Override
        public Optional<GolemSpawnOrigin> rawSpawnOrigin(Entity entity) {
            return entity.getExistingData(SPAWN_ORIGIN);
        }

        @Override
        public void setSpawnOrigin(Entity entity, GolemSpawnOrigin origin) {
            if (origin == GolemSpawnOrigin.UNKNOWN) {
                entity.removeData(SPAWN_ORIGIN);
                return;
            }
            entity.setData(SPAWN_ORIGIN, origin);
        }

        @Override
        public GolemAbilityState abilityState(Entity entity) {
            return entity.getExistingData(ABILITY_STATE).orElseGet(GolemAbilityState::fresh);
        }

        @Override
        public void setAbilityState(Entity entity, GolemAbilityState state) {
            entity.setData(ABILITY_STATE, state);
        }
    }
}
