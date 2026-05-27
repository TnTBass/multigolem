package dev.charles.multigolem.spawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZombieVillageSpawnResolverTest {

    @Test
    void defaultsComputeDesiredZombieGolems() {
        ZombieVillageSpawnResolver resolver = new ZombieVillageSpawnResolver(ZombieVillageSpawningConfig.defaults());

        assertEquals(0, resolver.desiredCount(0, 0));
        assertEquals(1, resolver.desiredCount(1, 0));
        assertEquals(1, resolver.desiredCount(4, 0));
        assertEquals(2, resolver.desiredCount(5, 0));
        assertEquals(0, resolver.desiredCount(0, 4), "regular zombies alone never qualify");
        assertEquals(2, resolver.desiredCount(1, 3));
        assertEquals(2, resolver.desiredCount(20, 20), "default max_zombie_golems_per_village is 2");
    }
}
