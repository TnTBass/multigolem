package dev.charles.multigolem.spawn;

public final class ZombieVillageSpawnResolver {
    private final ZombieVillageSpawningConfig config;

    public ZombieVillageSpawnResolver(ZombieVillageSpawningConfig config) {
        this.config = config;
    }

    public int desiredCount(int zombieVillagers, int regularZombies, int currentZombieGolems) {
        return config.desiredCount(zombieVillagers, regularZombies, currentZombieGolems);
    }
}
