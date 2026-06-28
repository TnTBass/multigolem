package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LapisAbilityTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void scansOnConfiguredCadence() {
        assertTrue(LapisAbility.shouldScan(100L, 95L, 5));
        assertFalse(LapisAbility.shouldScan(100L, 96L, 5));
        assertTrue(LapisAbility.shouldScan(100L, 99L, 1));
    }

    @Test
    void protectsVillageAlliesAndFriendlyGolems() {
        assertTrue(LapisAbility.shouldProtectTargetClass(Villager.class, null, false));
        assertTrue(LapisAbility.shouldProtectTargetClass(WanderingTrader.class, null, false));
        assertTrue(LapisAbility.shouldProtectTargetClass(IronGolem.class, GolemVariant.IRON, false));
        assertTrue(LapisAbility.shouldProtectTargetClass(IronGolem.class, GolemVariant.LAPIS, false));

        assertFalse(LapisAbility.shouldProtectTargetClass(IronGolem.class, GolemVariant.ZOMBIE, false));
        assertFalse(LapisAbility.shouldProtectTargetClass(Zombie.class, null, false));
        assertFalse(LapisAbility.shouldProtectTargetClass(Player.class, null, false));
        assertTrue(LapisAbility.shouldProtectTargetClass(Player.class, null, true));
    }

    @Test
    void configuredEffectsUseLapisDefaultListAndIgnoreOthers() {
        TierStats lapis = MultiGolemConfig.defaults().tier(GolemVariant.LAPIS);

        assertTrue(LapisAbility.isConfiguredEffect(new MobEffectInstance(MobEffects.POISON, 100), lapis));
        assertFalse(LapisAbility.isConfiguredEffect(new MobEffectInstance(MobEffects.STRENGTH, 100), lapis));
    }
}
