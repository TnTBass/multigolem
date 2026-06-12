package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class EmeraldAbility {

    private EmeraldAbility() {}

    public static void register() {
        ServerTickEvents.START_LEVEL_TICK.register(EmeraldAbility::onTick);
    }

    private static void onTick(ServerLevel world) {
        var stats = MultiGolem.config().tier(GolemVariant.EMERALD);
        long intervalTicks = (long) Math.max(1, stats.emeraldHealIntervalSeconds() * 20.0);
        if (world.getGameTime() % intervalTicks != 0) return;

        int range = stats.emeraldAuraRange();
        boolean countWandering = stats.emeraldCountWanderingTraders();
        float healAmount = stats.emeraldHealPerTick().floatValue();

        for (IronGolem golem : world.getEntities(EntityTypeTest.forClass(IronGolem.class), e -> true)) {
            if (GolemVariantAttachment.get(golem) != GolemVariant.EMERALD) continue;
            if (golem.getHealth() >= golem.getMaxHealth()) continue;
            try {
                tickEmerald(world, golem, range, countWandering, healAmount);
            } catch (Throwable t) {
                MultiGolem.LOG.error("EmeraldAbility tick failed for golem {}", golem.getId(), t);
            }
        }
    }

    private static void tickEmerald(ServerLevel world, IronGolem golem, int range, boolean countWandering, float healAmount) {
        AABB box = golem.getBoundingBox().inflate(range);
        List<AbstractVillager> nearby = world.getEntitiesOfClass(AbstractVillager.class, box, v ->
            v.isAlive() && !v.isRemoved() && (countWandering || v instanceof Villager));
        if (nearby.isEmpty()) return;

        AbstractVillager source = nearby.get(0);
        if (!source.isAlive() || source.isRemoved()) return;

        golem.heal(healAmount);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
            golem.getX(), golem.getEyeY() + 0.3, golem.getZ(),
            1, 0.2, 0.2, 0.2, 0.0);
    }
}
