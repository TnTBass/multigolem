package dev.charles.multigolem.attachment;

import dev.charles.multigolem.MultiGolem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class GolemSpawnOriginAttachment {

    public static final AttachmentType<GolemSpawnOrigin> TYPE = AttachmentRegistry
        .<GolemSpawnOrigin>builder()
        .persistent(GolemSpawnOrigin.CODEC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "spawn_origin"));

    private GolemSpawnOriginAttachment() {}

    public static GolemSpawnOrigin get(Entity entity) {
        GolemSpawnOrigin attached = entity.getAttached(TYPE);
        return attached != null ? attached : GolemSpawnOrigin.UNKNOWN;
    }

    public static Optional<GolemSpawnOrigin> getRaw(Entity entity) {
        return Optional.ofNullable(entity.getAttached(TYPE));
    }

    public static void set(Entity entity, GolemSpawnOrigin origin) {
        if (origin == GolemSpawnOrigin.UNKNOWN) {
            entity.removeAttached(TYPE);
            return;
        }
        entity.setAttached(TYPE, origin);
    }

    public static void touch() {
        // Force class load + TYPE registration when called from MultiGolem.onInitialize.
    }
}
