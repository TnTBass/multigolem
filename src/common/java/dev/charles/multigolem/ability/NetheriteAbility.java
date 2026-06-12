package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;

public final class NetheriteAbility {

    private NetheriteAbility() {}

    public static boolean isFireImmuneVariant(GolemVariant variant, boolean enabled) {
        return enabled && variant == GolemVariant.NETHERITE;
    }

    public static boolean allowDamage(Entity entity, DamageSource source, float amount) {
        if (!(entity instanceof IronGolem golem)) return true;
        if (GolemVariantAttachment.get(golem) != GolemVariant.NETHERITE) return true;
        if (!MultiGolem.config().tier(GolemVariant.NETHERITE).netheriteFireImmune()) return true;
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        return true;
    }
}
