package dev.charles.multigolem.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemIdentityMigration;
import dev.charles.multigolem.identity.GolemIdentityStorage;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;

import java.util.Optional;

public final class GolemIdentityAttachment {
    public static final AttachmentType<GolemIdentity> TYPE = AttachmentRegistry
        .<GolemIdentity>builder()
        .persistent(GolemIdentity.CODEC)
        .syncWith(GolemIdentity.STREAM_CODEC, AttachmentSyncPredicate.all())
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "identity"));

    private GolemIdentityAttachment() {}

    public static GolemIdentity get(Entity entity) {
        return GolemIdentityMigration.resolve(storageFor(entity));
    }

    /**
     * Returns only a valid stored identity. Use {@link #get(Entity)} for the authoritative
     * migration-aware read path that can inspect invalid new data and fall back to old variant data.
     */
    public static Optional<GolemIdentity> getRaw(Entity entity) {
        GolemIdentity attached = entity.getAttached(TYPE);
        return attached != null && attached.isValidForPhase2() ? Optional.of(attached) : Optional.empty();
    }

    public static void set(Entity entity, GolemIdentity identity) {
        GolemIdentityMigration.write(storageFor(entity), identity);
        if (entity instanceof IronGolem golem) {
            VariantAttributes.apply(golem);
        }
    }

    private static GolemIdentityStorage storageFor(Entity entity) {
        return new GolemIdentityStorage() {
            @Override
            public Optional<GolemIdentity> rawIdentity() {
                return Optional.ofNullable(entity.getAttached(TYPE));
            }

            @Override
            public void setRawIdentity(GolemIdentity identity) {
                entity.setAttached(TYPE, identity);
            }

            @Override
            public void clearRawIdentity() {
                entity.removeAttached(TYPE);
            }

            @Override
            public Optional<GolemVariant> rawVariant() {
                return GolemVariantAttachment.getRawOld(entity);
            }

            @Override
            public void setRawVariant(GolemVariant variant) {
                GolemVariantAttachment.setOldOnly(entity, variant);
            }

            @Override
            public void clearRawVariant() {
                GolemVariantAttachment.clearOld(entity);
            }
        };
    }

    public static void touch() {
        // Calling this from MultiGolem.onInitialize forces class load and TYPE registration.
    }
}
