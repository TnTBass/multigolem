package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.config.TierStats;

public final class GolemCombatRules {
    private GolemCombatRules() {}

    public static float healAmount(GolemVariant variant, TierStats stats) {
        Double zombieHealAmount = stats.zombieRottenFleshHealAmount();
        if (variant == GolemVariant.ZOMBIE && zombieHealAmount != null) {
            return zombieHealAmount.floatValue();
        }
        return 25.0F;
    }

    public static int netheriteIgniteSeconds(TierStats stats, GolemSpawnOrigin origin) {
        Integer baseSeconds = stats.netheriteIgniteSeconds();
        if (origin == GolemSpawnOrigin.VILLAGE) {
            Integer villageSeconds = stats.netheriteVillageIgniteSeconds();
            return villageSeconds != null ? villageSeconds : baseSeconds != null ? baseSeconds : 0;
        }
        return baseSeconds != null ? baseSeconds : 0;
    }
}
