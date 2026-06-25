package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedstoneAbilityTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void overchargeTriggersAtOrBelowConfiguredThreshold() {
        assertTrue(RedstoneAbility.shouldTriggerOvercharge(25.0F, 100.0F, 0.25, false, true));
        assertTrue(RedstoneAbility.shouldTriggerOvercharge(24.9F, 100.0F, 0.25, false, true));
        assertFalse(RedstoneAbility.shouldTriggerOvercharge(25.1F, 100.0F, 0.25, false, true));
    }

    @Test
    void overchargeDoesNotRetriggerWhileActiveOrCoolingDown() {
        assertFalse(RedstoneAbility.shouldTriggerOvercharge(10.0F, 100.0F, 0.25, true, true));
        assertFalse(RedstoneAbility.shouldTriggerOvercharge(10.0F, 100.0F, 0.25, false, false));
    }

    @Test
    void overchargeUsesAttackAndResistanceWithoutSpeed() {
        TierStats redstone = MultiGolemConfig.defaults().tier(GolemVariant.REDSTONE);

        assertEquals(19.5, RedstoneAbility.overchargedAttackDamage(13.0, 1.5), 0.0001);
        assertEquals(1, RedstoneAbility.resistanceEffectAmplifier(redstone));
    }

    @Test
    void deathPulseUsesSlownessTenConfiguration() {
        TierStats redstone = MultiGolemConfig.defaults().tier(GolemVariant.REDSTONE);

        assertEquals(9, RedstoneAbility.deathPulseSlownessAmplifier(redstone));
    }

    @Test
    void deathPulseAffectsHostilesAndZombieGolemsOnly() {
        TargetFilter ignored = TargetFilter.fromIgnoredList(MultiGolemConfig.defaults()
            .tier(GolemVariant.REDSTONE)
            .ignoredTargetTypes());

        assertTrue(RedstoneAbility.shouldAffectDeathPulseTargetClass(Zombie.class, null, ignored));
        assertTrue(RedstoneAbility.shouldAffectDeathPulseTargetClass(IronGolem.class, GolemVariant.ZOMBIE, ignored));

        assertFalse(RedstoneAbility.shouldAffectDeathPulseTargetClass(Creeper.class, null, ignored));
        assertFalse(RedstoneAbility.shouldAffectDeathPulseTargetClass(Player.class, null, ignored));
        assertFalse(RedstoneAbility.shouldAffectDeathPulseTargetClass(Villager.class, null, ignored));
        assertFalse(RedstoneAbility.shouldAffectDeathPulseTargetClass(WanderingTrader.class, null, ignored));
        assertFalse(RedstoneAbility.shouldAffectDeathPulseTargetClass(IronGolem.class, GolemVariant.IRON, ignored));
        assertFalse(RedstoneAbility.shouldAffectDeathPulseTargetClass(IronGolem.class, GolemVariant.REDSTONE, ignored));
        assertFalse(RedstoneAbility.shouldAffectDeathPulseTargetClass(IronGolem.class, GolemVariant.DIAMOND, ignored));
    }
}
