package dev.charles.multigolem.attachment;

import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemIdentityMigration;
import dev.charles.multigolem.identity.GolemIdentityStorage;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;

import java.util.Optional;

public final class GolemIdentityAttachment {
    private GolemIdentityAttachment() {}

    public static GolemIdentity get(Entity entity) {
        return GolemIdentityMigration.resolve(storageFor(entity));
    }

    /**
     * Returns only a valid stored identity. Use {@link #get(Entity)} for the authoritative
     * migration-aware read path that can inspect invalid new data and fall back to old variant data.
     */
    public static Optional<GolemIdentity> getRaw(Entity entity) {
        return storageFor(entity).rawIdentity().filter(GolemIdentity::isValidForPhase2);
    }

    public static void set(Entity entity, GolemIdentity identity) {
        GolemIdentityMigration.write(storageFor(entity), identity);
        if (entity instanceof IronGolem golem) {
            VariantAttributes.apply(golem);
        }
    }

    private static GolemIdentityStorage storageFor(Entity entity) {
        return GolemStorage.adapter().identityStorage(entity);
    }
}
