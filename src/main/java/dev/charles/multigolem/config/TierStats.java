package dev.charles.multigolem.config;

public record TierStats(int maxHealth, double attackDamage, boolean angerOnHit) {

    public static final int MIN_HEALTH = 1;
    public static final int MAX_HEALTH = 2048;
    public static final double MIN_DAMAGE = 0.0;
    public static final double MAX_DAMAGE = 2048.0;

    public TierStats clamped() {
        int h = Math.max(MIN_HEALTH, Math.min(MAX_HEALTH, maxHealth));
        double d = Math.max(MIN_DAMAGE, Math.min(MAX_DAMAGE, attackDamage));
        if (h == maxHealth && d == attackDamage) return this;
        return new TierStats(h, d, angerOnHit);
    }

    public boolean isClamped() {
        return maxHealth < MIN_HEALTH || maxHealth > MAX_HEALTH
            || attackDamage < MIN_DAMAGE || attackDamage > MAX_DAMAGE;
    }
}
