package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class LapisAbility {

    private LapisAbility() {}

    public static void onTick(ServerLevel world) {
        TierStats stats = MultiGolem.config().tier(GolemVariant.LAPIS);
        if (!Boolean.TRUE.equals(stats.lapisWardEnabled())) {
            return;
        }

        for (IronGolem golem : world.getEntities(EntityTypeTest.forClass(IronGolem.class), e -> true)) {
            if (GolemVariantAttachment.get(golem) != GolemVariant.LAPIS) continue;
            try {
                tickWard(world, golem, stats);
            } catch (Throwable t) {
                MultiGolem.LOG.error("LapisAbility tick failed for golem {}", golem.getId(), t);
            }
        }
    }

    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity == null || source == null || !isMagicDamage(source)) return true;

        TierStats stats = MultiGolem.config().tier(GolemVariant.LAPIS);
        if (!blocksMagicDamage(stats) || !shouldProtectTarget(entity, stats)) return true;

        return !hasNearbyActiveWard(entity, stats);
    }

    public static boolean allowEffectApplication(MobEffectInstance effect, LivingEntity entity) {
        if (effect == null || entity == null) return true;

        TierStats stats = MultiGolem.config().tier(GolemVariant.LAPIS);
        if (!blocksConfiguredEffects(stats) || !isConfiguredEffect(effect, stats)) return true;
        if (!shouldProtectTarget(entity, stats)) return true;

        return !hasNearbyActiveWard(entity, stats);
    }

    public static boolean shouldScan(long now, long lastScan, int intervalTicks) {
        return now - lastScan >= Math.max(1, intervalTicks);
    }

    public static boolean isMagicDamage(DamageSource source) {
        return source != null && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC));
    }

    public static boolean shouldProtectTargetClass(
        Class<? extends LivingEntity> targetClass,
        GolemVariant targetVariant,
        boolean playersEnabled
    ) {
        if (Player.class.isAssignableFrom(targetClass)) {
            return playersEnabled;
        }
        if (Villager.class.isAssignableFrom(targetClass)
            || WanderingTrader.class.isAssignableFrom(targetClass)) {
            return true;
        }
        if (IronGolem.class.isAssignableFrom(targetClass)) {
            return targetVariant != GolemVariant.ZOMBIE;
        }
        return false;
    }

    public static boolean blocksMagicDamage(TierStats stats) {
        return stats != null
            && Boolean.TRUE.equals(stats.lapisWardEnabled())
            && Boolean.TRUE.equals(stats.lapisWardMagicDamageEnabled());
    }

    public static boolean blocksConfiguredEffects(TierStats stats) {
        return stats != null
            && Boolean.TRUE.equals(stats.lapisWardEnabled())
            && Boolean.TRUE.equals(stats.lapisWardEffectCleanupEnabled())
            && stats.lapisWardEffectIds() != null
            && !stats.lapisWardEffectIds().isEmpty();
    }

    public static boolean isConfiguredEffect(MobEffectInstance effect, TierStats stats) {
        if (effect == null || stats == null || stats.lapisWardEffectIds() == null) {
            return false;
        }
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        return id != null && stats.lapisWardEffectIds().contains(id.toString());
    }

    static boolean isWithinWardRange(AABB targetBox, AABB wardBox, TierStats stats) {
        return targetBox != null && wardBox != null && targetBox.inflate(wardRange(stats)).intersects(wardBox);
    }

    private static void tickWard(ServerLevel world, IronGolem golem, TierStats stats) {
        if (!activeLapisWard(golem, stats)) return;

        int interval = Math.max(1, stats.lapisWardScanIntervalTicks());
        long now = world.getGameTime();
        if (Math.floorMod(now + golem.getId(), interval) != 0L) return;

        if (blocksConfiguredEffects(stats)) {
            AABB box = golem.getBoundingBox().inflate(wardRange(stats));
            for (LivingEntity target : world.getEntities(EntityTypeTest.forClass(LivingEntity.class), box,
                target -> target != golem && target.isAlive() && !target.isRemoved() && shouldProtectTarget(target, stats))) {
                removeConfiguredEffects(target, stats);
            }
        }

        if (Boolean.TRUE.equals(stats.lapisParticlesEnabled())) {
            world.sendParticles(ParticleTypes.ENCHANT,
                golem.getX(), golem.getEyeY() + 0.1, golem.getZ(),
                2, 0.35, 0.25, 0.35, 0.02);
        }
    }

    private static void removeConfiguredEffects(LivingEntity target, TierStats stats) {
        if (!blocksConfiguredEffects(stats)) return;

        for (MobEffectInstance active : List.copyOf(target.getActiveEffects())) {
            if (isConfiguredEffect(active, stats)) {
                target.removeEffect(active.getEffect());
            }
        }
    }

    private static boolean hasNearbyActiveWard(LivingEntity entity, TierStats stats) {
        if (!(entity.level() instanceof ServerLevel world)) return false;

        AABB box = entity.getBoundingBox().inflate(wardRange(stats));
        return !world.getEntities(EntityTypeTest.forClass(IronGolem.class), box, golem ->
            golem != entity
                && activeLapisWard(golem, stats)
                && isWithinWardRange(entity.getBoundingBox(), golem.getBoundingBox(), stats)
        ).isEmpty();
    }

    private static boolean activeLapisWard(IronGolem golem, TierStats stats) {
        return Boolean.TRUE.equals(stats.lapisWardEnabled())
            && golem.isAlive()
            && !golem.isRemoved()
            && GolemVariantAttachment.get(golem) == GolemVariant.LAPIS;
    }

    private static boolean shouldProtectTarget(LivingEntity target, TierStats stats) {
        GolemVariant targetVariant = target instanceof IronGolem golem
            ? GolemVariantAttachment.get(golem)
            : null;
        return shouldProtectTargetClass(target.getClass(), targetVariant,
            Boolean.TRUE.equals(stats.lapisWardAffectsPlayers()));
    }

    private static int wardRange(TierStats stats) {
        return Math.max(1, stats.lapisWardRange());
    }

}
