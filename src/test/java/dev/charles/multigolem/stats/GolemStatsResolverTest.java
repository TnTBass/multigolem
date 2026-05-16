package dev.charles.multigolem.stats;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
}
