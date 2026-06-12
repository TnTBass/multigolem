package dev.charles.multigolem.attachment;

import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class GolemSpawnOriginAttachment {
    private GolemSpawnOriginAttachment() {}

    public static GolemSpawnOrigin get(Entity entity) {
        return GolemStorage.adapter().spawnOrigin(entity);
    }

    public static Optional<GolemSpawnOrigin> getRaw(Entity entity) {
        return GolemStorage.adapter().rawSpawnOrigin(entity);
    }

    public static void set(Entity entity, GolemSpawnOrigin origin) {
        GolemStorage.adapter().setSpawnOrigin(entity, origin);
    }
}
