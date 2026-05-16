package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.AABB;

public final class GoldAbility {

    private static final double MOTION_THRESHOLD_SQR = 0.001;
    private static final int SHINE_INTERVAL_TICKS = 20;
    private static final AABB WORLD_BOUNDS = new AABB(-30_000_000, -64, -30_000_000, 30_000_000, 320, 30_000_000);

    private GoldAbility() {}

    public static void register() {
        ServerTickEvents.START_LEVEL_TICK.register(GoldAbility::onTick);
    }

    private static void onTick(ServerLevel world) {
        var stats = MultiGolem.config().tier(GolemVariant.GOLD);
        if (!stats.goldSprintParticlesEnabled() && !stats.goldSunlightShineEnabled()) return;
        for (IronGolem golem : world.getEntitiesOfClass(IronGolem.class, WORLD_BOUNDS)) {
            if (GolemVariantAttachment.get(golem) != GolemVariant.GOLD) continue;
            try {
                tickGold(world, golem, stats);
            } catch (Throwable t) {
                MultiGolem.LOG.error("GoldAbility tick failed for golem {}", golem.getId(), t);
            }
        }
    }

    private static void tickGold(ServerLevel world, IronGolem golem, dev.charles.multigolem.config.TierStats stats) {
        boolean moving = golem.getDeltaMovement().horizontalDistanceSqr() > MOTION_THRESHOLD_SQR;
        if (moving && stats.goldSprintParticlesEnabled()) {
            world.sendParticles(ParticleTypes.POOF,
                golem.getX(), golem.getY() + 0.1, golem.getZ(),
                2, 0.3, 0.05, 0.3, 0.0);
        }
        if (!moving
                && world.getGameTime() % SHINE_INTERVAL_TICKS == 0
                && stats.goldSunlightShineEnabled()
                && world.canSeeSky(golem.blockPosition())
                && world.getOverworldClockTime() % 24000L < 12000L
                && !world.isThundering()
                && !world.isRaining()) {
            world.sendParticles(ParticleTypes.END_ROD,
                golem.getX(), golem.getEyeY() + 0.5, golem.getZ(),
                1, 0.2, 0.0, 0.2, 0.0);
        }
    }
}
