package dev.charles.multigolem.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class GolemVariantAttachment {

    public static final AttachmentType<GolemVariant> TYPE = AttachmentRegistry
        .<GolemVariant>builder()
        .persistent(GolemVariant.CODEC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant"));

    private GolemVariantAttachment() {}

    public static GolemVariant get(Entity entity) {
        GolemVariant attached = entity.getAttached(TYPE);
        return attached != null ? attached : GolemVariant.IRON;
    }

    public static Optional<GolemVariant> getRaw(Entity entity) {
        return Optional.ofNullable(entity.getAttached(TYPE));
    }

    public static void set(Entity entity, GolemVariant variant) {
        entity.setAttached(TYPE, variant);
    }

    public static void touch() {
        // Calling this from MultiGolem.onInitialize forces class load and TYPE registration.
    }
}
