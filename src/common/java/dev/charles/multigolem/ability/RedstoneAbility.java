package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemAbilityState;
import dev.charles.multigolem.attachment.GolemAbilityStateAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class RedstoneAbility {
    private static final Identifier OVERCHARGE_ATTACK_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "redstone_overcharge_attack");

    private RedstoneAbility() {}

    public static void onTick(ServerLevel world) {
        for (IronGolem golem : world.getEntities(EntityTypeTest.forClass(IronGolem.class), e -> true)) {
            if (GolemVariantAttachment.get(golem) != GolemVariant.REDSTONE) continue;
            try {
                updateOvercharge(world, golem);
            } catch (Throwable t) {
                MultiGolem.LOG.error("RedstoneAbility tick failed for golem {}", golem.getId(), t);
            }
        }
    }

    public static void updateOvercharge(ServerLevel world, IronGolem golem) {
        TierStats stats = MultiGolem.config().tier(GolemVariant.REDSTONE);
        GolemAbilityState state = GolemAbilityStateAttachment.get(golem);
        long now = world.getGameTime();
        boolean active = state.redstoneOverchargeActive(now);
        boolean belowThreshold = golem.getHealth() <= thresholdHealth(golem.getMaxHealth(),
            safeDouble(stats.redstoneOverchargeHealthThresholdPercent(), 0.25));
        boolean shouldTrigger = !state.redstoneWasBelowThreshold()
            && Boolean.TRUE.equals(stats.redstoneOverchargeEnabled())
            && shouldTriggerOvercharge(golem.getHealth(), golem.getMaxHealth(),
                safeDouble(stats.redstoneOverchargeHealthThresholdPercent(), 0.25),
                active, state.redstoneCooldownReady(now));

        if (shouldTrigger) {
            long activeUntil = now + durationTicks(safeDouble(stats.redstoneOverchargeDurationSeconds(), 0.0));
            long cooldownUntil = activeUntil + durationTicks(safeDouble(stats.redstoneOverchargeCooldownSeconds(), 0.0));
            state = state.withRedstoneOvercharge(activeUntil).withRedstoneCooldown(cooldownUntil);
            active = state.redstoneOverchargeActive(now);
            if (Boolean.TRUE.equals(stats.redstoneParticlesEnabled())) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    golem.getX(), golem.getEyeY() + 0.25, golem.getZ(),
                    12, 0.45, 0.35, 0.45, 0.08);
            }
        }

        if (state.redstoneWasBelowThreshold() != belowThreshold) {
            state = state.withRedstoneWasBelowThreshold(belowThreshold);
        }
        GolemAbilityStateAttachment.set(golem, state);

        if (active) {
            applyAttackMultiplier(golem, safeDouble(stats.redstoneOverchargeAttackMultiplier(), 1.0));
            int resistanceTicks = resistanceEffectDurationTicks(stats);
            if (resistanceTicks > 0) {
                golem.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, resistanceTicks,
                    resistanceEffectAmplifier(stats), true, true));
            }
            if (Boolean.TRUE.equals(stats.redstoneParticlesEnabled()) && now % 10L == 0L) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    golem.getX(), golem.getEyeY() + 0.2, golem.getZ(),
                    1, 0.25, 0.2, 0.25, 0.02);
            }
        } else {
            removeAttackMultiplier(golem);
        }
    }

    public static void emitDeathPulse(ServerLevel world, IronGolem golem) {
        if (GolemVariantAttachment.get(golem) != GolemVariant.REDSTONE) return;

        TierStats stats = MultiGolem.config().tier(GolemVariant.REDSTONE);
        if (!Boolean.TRUE.equals(stats.redstoneDeathPulseEnabled())) return;

        int radius = Math.max(1, safeInt(stats.redstoneDeathPulseRadius(), 8));
        int duration = deathPulseDurationTicks(stats);
        if (duration <= 0) return;

        TargetFilter ignored = TargetFilter.fromIgnoredList(stats.ignoredTargetTypes());
        AABB box = golem.getBoundingBox().inflate(radius);
        double radiusSquared = radius * radius;
        List<LivingEntity> targets = world.getEntities(EntityTypeTest.forClass(LivingEntity.class), box, target ->
            target != golem
                && target.isAlive()
                && !target.isRemoved()
                && target.distanceToSqr(golem) <= radiusSquared
                && shouldAffectDeathPulseTarget(target, ignored));

        for (LivingEntity target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration,
                deathPulseSlownessAmplifier(stats), true, true));
        }

        if (Boolean.TRUE.equals(stats.redstoneDeathPulseParticlesEnabled())) {
            world.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                golem.getX(), golem.getEyeY() + 0.1, golem.getZ(),
                Math.max(16, radius * 4), radius * 0.35, 0.35, radius * 0.35, 0.12);
        }
    }

    public static boolean shouldTriggerOvercharge(
        float health,
        float maxHealth,
        double thresholdPercent,
        boolean alreadyActive,
        boolean cooldownReady
    ) {
        if (alreadyActive || !cooldownReady || maxHealth <= 0.0F) return false;
        return health <= thresholdHealth(maxHealth, thresholdPercent);
    }

    public static double overchargedAttackDamage(double baseAttackDamage, double attackMultiplier) {
        return baseAttackDamage * Math.max(1.0, attackMultiplier);
    }

    public static int resistanceEffectAmplifier(TierStats stats) {
        return Math.max(0, safeInt(stats.redstoneOverchargeResistanceAmplifier(), 0));
    }

    public static int resistanceEffectDurationTicks(TierStats stats) {
        return durationTicks(safeDouble(stats.redstoneOverchargeResistanceRefreshSeconds(), 0.0));
    }

    public static int deathPulseSlownessAmplifier(TierStats stats) {
        return Math.max(0, safeInt(stats.redstoneDeathPulseSlownessAmplifier(), 0));
    }

    public static int deathPulseDurationTicks(TierStats stats) {
        return durationTicks(safeDouble(stats.redstoneDeathPulseSlownessSeconds(), 0.0));
    }

    public static int durationTicks(double seconds) {
        return Math.max(0, (int) Math.ceil(seconds * 20.0));
    }

    public static boolean shouldAffectDeathPulseTargetClass(
        Class<? extends LivingEntity> targetClass,
        GolemVariant targetVariant,
        TargetFilter ignored
    ) {
        if (Player.class.isAssignableFrom(targetClass)
            || Villager.class.isAssignableFrom(targetClass)
            || WanderingTrader.class.isAssignableFrom(targetClass)) {
            return false;
        }
        if (IronGolem.class.isAssignableFrom(targetClass)) {
            return targetVariant == GolemVariant.ZOMBIE;
        }
        return !ignored.isExcludedClass(targetClass) && Enemy.class.isAssignableFrom(targetClass);
    }

    private static boolean shouldAffectDeathPulseTarget(LivingEntity target, TargetFilter ignored) {
        GolemVariant targetVariant = target instanceof IronGolem golem
            ? GolemVariantAttachment.get(golem)
            : null;
        return shouldAffectDeathPulseTargetClass(target.getClass(), targetVariant, ignored);
    }

    private static float thresholdHealth(float maxHealth, double thresholdPercent) {
        return (float) (maxHealth * Math.max(0.0, thresholdPercent));
    }

    private static void applyAttackMultiplier(IronGolem golem, double multiplier) {
        AttributeInstance attack = golem.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;
        double amount = Math.max(1.0, multiplier) - 1.0;
        AttributeModifier existing = attack.getModifier(OVERCHARGE_ATTACK_MODIFIER_ID);
        if (existing != null && existing.amount() == amount) return;
        attack.removeModifier(OVERCHARGE_ATTACK_MODIFIER_ID);
        if (amount != 0.0) {
            attack.addTransientModifier(new AttributeModifier(OVERCHARGE_ATTACK_MODIFIER_ID, amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeAttackMultiplier(IronGolem golem) {
        AttributeInstance attack = golem.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(OVERCHARGE_ATTACK_MODIFIER_ID);
        }
    }

    private static double safeDouble(Double value, double fallback) {
        return value != null ? value : fallback;
    }

    private static int safeInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }
}
