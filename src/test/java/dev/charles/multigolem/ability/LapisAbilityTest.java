package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
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

    @Test
    void wardRangeUsesBlockCubeLikeEffectCleanup() {
        TierStats lapis = MultiGolemConfig.defaults().tier(GolemVariant.LAPIS);

        assertTrue(LapisAbility.isWithinWardRange(15.0, 0.0, 15.0, lapis));
        assertFalse(LapisAbility.isWithinWardRange(16.0, 0.0, 0.0, lapis));
    }

    @Test
    void invalidEffectInputsAreIgnoredWithoutThrowing() {
        TierStats lapis = MultiGolemConfig.defaults().tier(GolemVariant.LAPIS);

        assertFalse(LapisAbility.isConfiguredEffect(null, lapis));
        assertFalse(LapisAbility.isConfiguredEffect(new MobEffectInstance(MobEffects.POISON, 100), null));
    }

    @Test
    void magicDamageUsesOnlyConfiguredMinecraftDamageTypes() {
        assertTrue(LapisAbility.isMagicDamage(new KeyedDamageSource(DamageTypes.MAGIC)));
        assertTrue(LapisAbility.isMagicDamage(new KeyedDamageSource(DamageTypes.INDIRECT_MAGIC)));
        assertFalse(LapisAbility.isMagicDamage(new KeyedDamageSource(DamageTypes.LIGHTNING_BOLT)));
        assertFalse(LapisAbility.isMagicDamage(null));
    }

    @Test
    void wardFlagsGateDamageAndEffectProtection() {
        TierStats lapis = MultiGolemConfig.defaults().tier(GolemVariant.LAPIS);
        TierStats disabled = new TierStats(
            lapis.maxHealth(), lapis.attackDamage(), lapis.angerOnHit(), lapis.ignoredTargetTypes(),
            lapis.copperLightningImmune(), lapis.copperLightningHealAmount(),
            lapis.goldSpeedMultiplier(), lapis.goldSprintParticlesEnabled(), lapis.goldSunlightShineEnabled(),
            lapis.emeraldAuraRange(), lapis.emeraldHealIntervalSeconds(), lapis.emeraldHealPerTick(),
            lapis.emeraldCountWanderingTraders(),
            lapis.diamondTargetMode(), lapis.diamondCooldownMinSeconds(), lapis.diamondCooldownMaxSeconds(),
            lapis.diamondAuraRange(), lapis.diamondLightningProof(),
            lapis.netheriteFireImmune(), lapis.netheriteIgniteSeconds(), lapis.netheriteVillageIgniteSeconds(),
            lapis.zombieRottenFleshHealAmount(),
            lapis.zombieHungerEnabled(), lapis.zombieHungerSeconds(), lapis.zombieHungerAmplifier(),
            lapis.zombieNauseaEnabled(), lapis.zombieNauseaSeconds(), lapis.zombieNauseaAmplifier(),
            lapis.zombiePoisonEnabled(), lapis.zombiePoisonSeconds(), lapis.zombiePoisonAmplifier(),
            lapis.zombieConvertVillagersEnabled(), lapis.zombieVillagerConversionChance(),
            lapis.zombieConvertWanderingTradersEnabled(), lapis.zombieWanderingTraderConversionChance(),
            lapis.redstoneOverchargeEnabled(), lapis.redstoneOverchargeHealthThresholdPercent(),
            lapis.redstoneOverchargeDurationSeconds(), lapis.redstoneOverchargeCooldownSeconds(),
            lapis.redstoneOverchargeAttackMultiplier(), lapis.redstoneOverchargeResistanceAmplifier(),
            lapis.redstoneOverchargeResistanceRefreshSeconds(), lapis.redstoneDeathPulseEnabled(),
            lapis.redstoneDeathPulseRadius(),
            lapis.redstoneDeathPulseSlownessSeconds(), lapis.redstoneDeathPulseSlownessAmplifier(),
            lapis.redstoneParticlesEnabled(), lapis.redstoneDeathPulseParticlesEnabled(),
            false, lapis.lapisWardRange(),
            lapis.lapisWardScanIntervalTicks(), lapis.lapisWardAffectsPlayers(),
            false, false, lapis.lapisWardEffectIds(), lapis.lapisParticlesEnabled());

        assertTrue(LapisAbility.blocksMagicDamage(lapis));
        assertTrue(LapisAbility.blocksConfiguredEffects(lapis));
        assertFalse(LapisAbility.blocksMagicDamage(disabled));
        assertFalse(LapisAbility.blocksConfiguredEffects(disabled));
    }

    private static final class KeyedDamageSource extends DamageSource {
        private final ResourceKey<DamageType> key;

        private KeyedDamageSource(ResourceKey<DamageType> key) {
            super(Holder.direct(new DamageType("test", 0.1F)));
            this.key = key;
        }

        @Override
        public boolean is(ResourceKey<DamageType> key) {
            return this.key.equals(key);
        }
    }
}
