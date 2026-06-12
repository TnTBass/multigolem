package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;

public final class CopperAbility {

    private CopperAbility() {}

    public static boolean allowDamage(Entity entity, DamageSource source, float amount) {
        if (!(entity instanceof IronGolem golem)) return true;
        if (GolemVariantAttachment.get(golem) != GolemVariant.COPPER) return true;
        if (!source.is(DamageTypes.LIGHTNING_BOLT)) return true;
        if (!MultiGolem.config().tier(GolemVariant.COPPER).copperLightningImmune()) return true;

        try {
            Double configHeal = MultiGolem.config().tier(GolemVariant.COPPER).copperLightningHealAmount();
            float missingHp = golem.getMaxHealth() - golem.getHealth();
            float healAmount = configHeal == null ? missingHp : (float) Math.min(configHeal, missingHp);
            if (healAmount > 0) golem.heal(healAmount);
            if (golem.level() instanceof ServerLevel sw) {
                sw.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    golem.getX(), golem.getY() + 1.0, golem.getZ(),
                    40, 0.6, 1.0, 0.6, 0.1);
            }
        } catch (Throwable t) {
            MultiGolem.LOG.error("CopperAbility heal/visual failed; damage still cancelled", t);
        }

        return false;
    }
}
