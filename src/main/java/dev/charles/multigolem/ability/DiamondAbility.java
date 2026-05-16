package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemAbilityState;
import dev.charles.multigolem.attachment.GolemAbilityStateAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class DiamondAbility {

    private static final AABB WORLD_BOUNDS = new AABB(-30_000_000, -64, -30_000_000, 30_000_000, 320, 30_000_000);

    private DiamondAbility() {}

    public static void register() {
        ServerTickEvents.START_LEVEL_TICK.register(DiamondAbility::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof IronGolem golem)) return true;
            if (GolemVariantAttachment.get(golem) != GolemVariant.DIAMOND) return true;
            if (!MultiGolem.config().tier(GolemVariant.DIAMOND).diamondLightningProof()) return true;
            if (source.is(DamageTypes.LIGHTNING_BOLT)) return false;
            return true;
        });
    }

    private static void onTick(ServerLevel world) {
        var stats = MultiGolem.config().tier(GolemVariant.DIAMOND);
        if ("NONE".equals(stats.diamondTargetMode())) return;

        for (IronGolem golem : world.getEntitiesOfClass(IronGolem.class, WORLD_BOUNDS)) {
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

        // Cooldown-ready visual: emit END_ROD every second while primed
        if (ability.diamondCooldownReady(now) && now % 20 == 0) {
            world.sendParticles(ParticleTypes.END_ROD,
                golem.getX(), golem.getEyeY() + 0.4, golem.getZ(),
                1, 0.05, 0.05, 0.05, 0.0);
        }

        if (!ability.diamondCooldownReady(now)) return;

        int range = stats.diamondAuraRange();
        var modePredicate = TargetFilter.DiamondTargetPredicate.of(stats.diamondTargetMode());
        var excludeFilter = TargetFilter.fromIgnoredList(stats.ignoredTargetTypes());

        AABB box = golem.getBoundingBox().inflate(range);
        List<Entity> candidates = world.getEntities(golem, box, e ->
            e.isAlive() && !e.isRemoved()
                && modePredicate.matches(e)
                && !excludeFilter.isExcluded(e)
                && hasLineOfSight(world, golem, e));
        if (candidates.isEmpty()) return;

        Entity target = candidates.stream()
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(golem)))
            .orElseThrow();

        var bolt = EntityType.LIGHTNING_BOLT.create(world, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setPos(target.getX(), target.getY(), target.getZ());
            world.addFreshEntity(bolt);
        }

        long min = Math.max(0, stats.diamondCooldownMinSeconds()) * 20L;
        long max = Math.max(min, stats.diamondCooldownMaxSeconds()) * 20L;
        long span = Math.max(1, max - min + 1);
        long nextAt = now + min + (Math.abs(world.getRandom().nextLong()) % span);
        GolemAbilityStateAttachment.set(golem, ability.withDiamondCooldown(nextAt));
    }

    private static boolean hasLineOfSight(ServerLevel world, LivingEntity from, Entity to) {
        Vec3 start = new Vec3(from.getX(), from.getEyeY(), from.getZ());
        Vec3 end = new Vec3(to.getX(), to.getEyeY(), to.getZ());
        var hit = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from));
        return hit.getType() == HitResult.Type.MISS;
    }
}
