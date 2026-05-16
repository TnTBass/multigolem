package dev.charles.multigolem.stats;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;

public final class GolemStatsResolver {

    private final MultiGolemConfig config;

    public GolemStatsResolver(MultiGolemConfig config) {
        this.config = config;
    }

    public int maxHealth(GolemVariant variant) {
        return config.tier(variant).maxHealth();
    }

    public double attackDamage(GolemVariant variant) {
        return config.tier(variant).attackDamage();
    }

    public boolean angerOnHit(GolemVariant variant) {
        return config.tier(variant).angerOnHit();
    }

    public TierStats stats(GolemVariant variant) {
        return config.tier(variant);
    }
}
