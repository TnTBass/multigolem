package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemAbilityState;
import dev.charles.multigolem.attachment.GolemAbilityStateAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class DiamondAbility {

    private DiamondAbility() {}

    public static boolean allowDamage(Entity entity, DamageSource source, float amount) {
        if (!(entity instanceof IronGolem golem)) return true;
        if (GolemVariantAttachment.get(golem) != GolemVariant.DIAMOND) return true;
        if (!MultiGolem.config().tier(GolemVariant.DIAMOND).diamondLightningProof()) return true;
        if (source.is(DamageTypes.LIGHTNING_BOLT)) return false;
        return true;
    }

    public static void onTick(ServerLevel world) {
        var stats = MultiGolem.config().tier(GolemVariant.DIAMOND);
        if ("NONE".equals(stats.diamondTargetMode())) return;

        for (IronGolem golem : world.getEntities(EntityTypeTest.forClass(IronGolem.class), e -> true)) {
            if (GolemVariantAttachment.get(golem) != GolemVariant.DIAMOND) continue;
            try {
                tickDiamond(world, golem, stats);
            } catch (Throwable t) {
                MultiGolem.LOG.error("DiamondAbility tick failed for golem {}", golem.getId(), t);
            }
        }
    }

    private static void tickDiamond(ServerLevel world, IronGolem golem, dev.charles.multigolem.config.TierStats stats) {
        GolemAbilityState ability = GolemAbilityStateAttachment.get(golem);
        long now = world.getGameTime();
        GolemAbilityState clamped = ability.clampDiamondCooldown(now, maxCooldownTicks(stats));
        if (clamped != ability) {
            GolemAbilityStateAttachment.set(golem, clamped);
            ability = clamped;
        }

        // Cooldown-ready visual: emit END_ROD every second while primed
        if (ability.diamondCooldownReady(now) && now % 20 == 0) {
            world.sendParticles(ParticleTypes.END_ROD,
                golem.getX(), golem.getEyeY() + 0.4, golem.getZ(),
                1, 0.05, 0.05, 0.05, 0.0);
        }

        if (!ability.diamondCooldownReady(now)) return;
        if (!ability.diamondScanReady(now)) return;

        int range = stats.diamondAuraRange();
        var modePredicate = TargetFilter.DiamondTargetPredicate.of(stats.diamondTargetMode());
        var excludeFilter = TargetFilter.fromIgnoredList(stats.ignoredTargetTypes());

        AABB box = golem.getBoundingBox().inflate(range);
        List<Entity> candidates = world.getEntities(golem, box, e ->
            e.isAlive() && !e.isRemoved()
                && matchesTarget(modePredicate, e)
                && !excludeFilter.isExcluded(e)
                && hasLineOfSight(world, golem, e));
        if (candidates.isEmpty()) {
            GolemAbilityStateAttachment.set(golem, ability.withDiamondScanBackoff(now + 40L));
            return;
        }

        Entity target = candidates.stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(golem)))
            .orElseThrow();

        var bolt = EntityTypes.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setPos(target.getX(), target.getY(), target.getZ());
            world.addFreshEntity(bolt);
        }

        long min = Math.max(0, stats.diamondCooldownMinSeconds()) * 20L;
        long max = Math.max(min, stats.diamondCooldownMaxSeconds()) * 20L;
        long span = Math.max(1, max - min + 1);
        long nextAt = now + min + Math.floorMod(world.getRandom().nextLong(), span);
        GolemAbilityStateAttachment.set(golem, ability.withDiamondCooldown(nextAt));
    }

    public static long maxCooldownTicks(dev.charles.multigolem.config.TierStats stats) {
        return Math.max(0, stats.diamondCooldownMaxSeconds()) * 20L;
    }

    private static boolean matchesTarget(TargetFilter.DiamondTargetPredicate predicate, Entity target) {
        if (target instanceof IronGolem golem) {
            return predicate.matchesGolemVariant(GolemVariantAttachment.get(golem));
        }
        return predicate.matches(target);
    }

    private static boolean hasLineOfSight(ServerLevel world, LivingEntity from, Entity to) {
        Vec3 start = new Vec3(from.getX(), from.getEyeY(), from.getZ());
        Vec3 end = new Vec3(to.getX(), to.getEyeY(), to.getZ());
        var hit = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from));
        return hit.getType() == HitResult.Type.MISS;
    }
}
