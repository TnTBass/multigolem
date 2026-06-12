package dev.charles.multigolem.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemIdentityStorage;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public interface GolemStorageAdapter {
    GolemIdentityStorage identityStorage(Entity entity);

    Optional<GolemVariant> rawVariant(Entity entity);

    void setRawVariant(Entity entity, GolemVariant variant);

    void clearRawVariant(Entity entity);

    GolemSpawnOrigin spawnOrigin(Entity entity);

    Optional<GolemSpawnOrigin> rawSpawnOrigin(Entity entity);

    void setSpawnOrigin(Entity entity, GolemSpawnOrigin origin);

    GolemAbilityState abilityState(Entity entity);

    void setAbilityState(Entity entity, GolemAbilityState state);
}
