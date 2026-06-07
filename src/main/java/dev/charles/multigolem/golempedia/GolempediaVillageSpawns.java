package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.spawn.VillageSpawnWeights;

import java.util.Map;

public final class GolempediaVillageSpawns {
    private GolempediaVillageSpawns() {
    }

    public static String summary(
        GolemVariant variant,
        boolean villageSpawnsEnabled,
        Map<GolemVariant, Integer> weights,
        boolean zombieVillageSpawningEnabled
    ) {
        if (variant == GolemVariant.ZOMBIE) {
            return zombieVillageSpawningEnabled
                ? "Special village rule: may appear near villages with zombie villagers."
                : "Does not appear near villages.";
        }
        if (!VillageSpawnWeights.rollOrder().contains(variant)) {
            return "Does not spawn from normal village golem creation.";
        }
        if (!villageSpawnsEnabled) {
            return "Village golem spawning is disabled.";
        }

        int weight = Math.max(0, weights.getOrDefault(variant, 0));
        int total = 0;
        for (GolemVariant rollVariant : VillageSpawnWeights.rollOrder()) {
            total += Math.max(0, weights.getOrDefault(rollVariant, 0));
        }
        if (weight == 0 || total == 0) {
            return "Does not spawn from normal village golem creation.";
        }

        int percent = Math.round(weight * 100.0f / total);
        int oneIn = Math.max(1, Math.round(total * 1.0f / weight));
        return "About " + percent + "% of village-spawned golems (roughly 1 in " + oneIn + ").";
    }
}
