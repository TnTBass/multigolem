package dev.charles.multigolem.spawn;

public record ZombieVillageSpawningConfig(
    boolean enabled,
    int minZombieVillagers,
    int zombieVillagersPerGolem,
    boolean regularZombieBonusEnabled,
    int regularZombieBonusThreshold,
    int maxZombieGolemsPerVillage
) {
    public static ZombieVillageSpawningConfig defaults() {
        return new ZombieVillageSpawningConfig(true, 1, 5, true, 3, 2);
    }

    public int desiredCount(int zombieVillagers, int regularZombies, int currentZombieGolems) {
        if (!enabled || maxZombieGolemsPerVillage <= 0 || zombieVillagers < minZombieVillagers) {
            return 0;
        }
        int desired = 1 + (zombieVillagers / zombieVillagersPerGolem);
        if (regularZombieBonusEnabled && regularZombies >= regularZombieBonusThreshold) {
            desired++;
        }
        return Math.max(0, Math.min(maxZombieGolemsPerVillage, desired));
    }

    public ZombieVillageSpawningConfig withMaxZombieGolemsPerVillage(int max) {
        return new ZombieVillageSpawningConfig(enabled, minZombieVillagers, zombieVillagersPerGolem,
            regularZombieBonusEnabled, regularZombieBonusThreshold, max);
    }
}
