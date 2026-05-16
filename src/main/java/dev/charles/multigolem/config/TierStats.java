package dev.charles.multigolem.config;

import java.util.List;
import java.util.Objects;

/**
 * Per-tier configuration. V1 fields plus all V2 ability fields.
 * Optional V2 fields are null when the field doesn't apply to this tier
 * (e.g., gold-specific fields are null on the copper tier).
 */
public record TierStats(
    int maxHealth,
    double attackDamage,
    boolean angerOnHit,
    List<String> ignoredTargetTypes,
    // Copper
    Boolean copperLightningImmune,
    Double copperLightningHealAmount,  // null = full heal
    // Gold
    Double goldSpeedMultiplier,
    Boolean goldSprintParticlesEnabled,
    Boolean goldSunlightShineEnabled,
    // Emerald
    Integer emeraldAuraRange,
    Double emeraldHealIntervalSeconds,
    Double emeraldHealPerTick,
    Boolean emeraldCountWanderingTraders,
    // Diamond
    String diamondTargetMode,
    Integer diamondCooldownMinSeconds,
    Integer diamondCooldownMaxSeconds,
    Integer diamondAuraRange,
    Boolean diamondLightningProof,
    // Netherite
    Boolean netheriteFireImmune,
    Integer netheriteIgniteSeconds
) {

    public static final int MIN_HEALTH = 1;
    public static final int MAX_HEALTH = 2048;
    public static final double MIN_DAMAGE = 0.0;
    public static final double MAX_DAMAGE = 2048.0;

    public TierStats {
        Objects.requireNonNull(ignoredTargetTypes, "ignoredTargetTypes");
        ignoredTargetTypes = List.copyOf(ignoredTargetTypes);
    }

    /** Clamp V1 fields. V2 field clamping happens in MultiGolemConfig.parse. */
    public TierStats clampedHealthDamage() {
        int h = Math.max(MIN_HEALTH, Math.min(MAX_HEALTH, maxHealth));
        double d = Math.max(MIN_DAMAGE, Math.min(MAX_DAMAGE, attackDamage));
        if (h == maxHealth && d == attackDamage) return this;
        return new TierStats(h, d, angerOnHit, ignoredTargetTypes,
            copperLightningImmune, copperLightningHealAmount,
            goldSpeedMultiplier, goldSprintParticlesEnabled, goldSunlightShineEnabled,
            emeraldAuraRange, emeraldHealIntervalSeconds, emeraldHealPerTick, emeraldCountWanderingTraders,
            diamondTargetMode, diamondCooldownMinSeconds, diamondCooldownMaxSeconds, diamondAuraRange, diamondLightningProof,
            netheriteFireImmune, netheriteIgniteSeconds);
    }

    public boolean isHealthDamageClamped() {
        return maxHealth < MIN_HEALTH || maxHealth > MAX_HEALTH
            || attackDamage < MIN_DAMAGE || attackDamage > MAX_DAMAGE;
    }
}
