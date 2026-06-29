package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemIdentityAttachment;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.attachment.GolemSpawnOriginAttachment;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.world.entity.animal.golem.IronGolem;

public final class VillageGolemSpawnHandler {

    private VillageGolemSpawnHandler() {}

    public static void applyVillageRoll(IronGolem golem) {
        try {
            // Constructed per-spawn to pick up live config reloads.
            VillageGolemSpawnResolver resolver = new VillageGolemSpawnResolver(MultiGolem.config());
            resolver.rollIdentity(bound -> golem.getRandom().nextInt(bound))
                .ifPresent(identity -> applyIdentity(golem, identity));
        } catch (Throwable t) {
            MultiGolem.LOG.error("Failed to apply village golem variant roll to golem {}", golem.getId(), t);
        }
    }

    private static void applyVariant(IronGolem golem, GolemVariant variant) {
        applyIdentity(golem, GolemIdentity.ofIronVariant(variant), VillageGolemSpawnHandler::applyVariantAttachments);
    }

    static void applyVariant(IronGolem golem, GolemVariant variant, VillageVariantApplier applier) {
        applyIdentity(golem, GolemIdentity.ofIronVariant(variant), applier);
    }

    private static void applyIdentity(IronGolem golem, GolemIdentity identity) {
        applyIdentity(golem, identity, VillageGolemSpawnHandler::applyVariantAttachments);
    }

    static void applyIdentity(IronGolem golem, GolemIdentity identity, VillageVariantApplier applier) {
        if (identity.variant() == GolemVariant.IRON) return;

        applier.apply(golem, identity.variant(), GolemSpawnOrigin.VILLAGE);
        golem.setHealth(golem.getMaxHealth());
    }

    private static void applyVariantAttachments(IronGolem golem, GolemVariant variant, GolemSpawnOrigin origin) {
        GolemIdentityAttachment.set(golem, GolemIdentity.ofIronVariant(variant));
        GolemSpawnOriginAttachment.set(golem, origin);
    }

    @FunctionalInterface
    interface VillageVariantApplier {
        void apply(IronGolem golem, GolemVariant variant, GolemSpawnOrigin origin);
    }
}
