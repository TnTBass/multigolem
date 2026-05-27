package dev.charles.multigolem.config;

import dev.charles.multigolem.spawn.ZombieVillageSpawningConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZombieVillageSpawningConfigTest {

    @Test
    void defaultsComputeDesiredZombieGolemCount() {
        ZombieVillageSpawningConfig cfg = ZombieVillageSpawningConfig.defaults();

        assertEquals(0, cfg.desiredCount(0, 10));
        assertEquals(1, cfg.desiredCount(1, 0));
        assertEquals(1, cfg.desiredCount(4, 0));
        assertEquals(2, cfg.desiredCount(5, 0));
        assertEquals(0, cfg.desiredCount(0, 3), "regular zombies alone never qualify");
        assertEquals(2, cfg.desiredCount(1, 3), "regular zombies can add bonus only after one zombie villager qualifies");
        assertEquals(0, cfg.withMaxZombieGolemsPerVillage(0).desiredCount(10, 10));
    }
}
