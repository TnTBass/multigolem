package dev.charles.multigolem.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public final class GolemVariantAttachment {
    private GolemVariantAttachment() {}

    public static GolemVariant get(Entity entity) {
        return GolemIdentityAttachment.get(entity).variant();
    }

    public static Optional<GolemVariant> getRaw(Entity entity) {
        return GolemStorage.adapter().rawVariant(entity);
    }

    public static void set(Entity entity, GolemVariant variant) {
        GolemIdentityAttachment.set(entity, GolemIdentity.ofIronVariant(variant));
    }

    static Optional<GolemVariant> getRawOld(Entity entity) {
        return GolemStorage.adapter().rawVariant(entity);
    }

    static void setOldOnly(Entity entity, GolemVariant variant) {
        GolemStorage.adapter().setRawVariant(entity, variant);
    }

    static void clearOld(Entity entity) {
        GolemStorage.adapter().clearRawVariant(entity);
    }
}
