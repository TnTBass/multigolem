package dev.charles.multigolem.stats;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GolemStatsResolverTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void resolvesAllVariantsFromDefaults() {
        MultiGolemConfig cfg = MultiGolemConfig.defaults();
        GolemStatsResolver r = new GolemStatsResolver(cfg);

        assertEquals(60,   r.maxHealth(GolemVariant.COPPER));
        assertEquals(8.5,  r.attackDamage(GolemVariant.COPPER), 0.0001);
        assertTrue(r.angerOnHit(GolemVariant.COPPER));

        assertEquals(600,  r.maxHealth(GolemVariant.NETHERITE));
        assertEquals(85.0, r.attackDamage(GolemVariant.NETHERITE), 0.0001);
    }

    @Test
    void ironMatchesVanillaDefaults() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertEquals(100,  r.maxHealth(GolemVariant.IRON));
        assertEquals(15.0, r.attackDamage(GolemVariant.IRON), 0.0001);
    }

    @Test
    void resolvesV2_copperFields() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertTrue(r.copperLightningImmune());
        assertNull(r.copperLightningHealAmount());
    }

    @Test
    void resolvesV2_goldFields() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertEquals(1.25, r.goldSpeedMultiplier(), 0.0001);
        assertTrue(r.goldSprintParticlesEnabled());
        assertTrue(r.goldSunlightShineEnabled());
    }

    @Test
    void resolvesV2_emeraldFields() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertEquals(8, r.emeraldAuraRange());
        assertEquals(2.0, r.emeraldHealIntervalSeconds(), 0.0001);
        assertEquals(1.0, r.emeraldHealPerTick(), 0.0001);
        assertTrue(r.emeraldCountWanderingTraders());
    }

    @Test
    void resolvesV2_diamondFields() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertEquals("ALL_HOSTILE_MOBS", r.diamondTargetMode());
        assertEquals(30, r.diamondCooldownMinSeconds());
        assertEquals(60, r.diamondCooldownMaxSeconds());
        assertEquals(12, r.diamondAuraRange());
        assertTrue(r.diamondLightningProof());
    }

    @Test
    void resolvesV2_netheriteFields() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertTrue(r.netheriteFireImmune());
        assertEquals(5, r.netheriteIgniteSeconds());
    }

    @Test
    void resolvesV2_ignoredTargetTypes() {
        GolemStatsResolver r = new GolemStatsResolver(MultiGolemConfig.defaults());
        assertEquals(List.of(), r.ignoredTargetTypes(GolemVariant.IRON));
        assertEquals(List.of("CREEPERS"), r.ignoredTargetTypes(GolemVariant.COPPER));
    }
}
