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
    Integer netheriteIgniteSeconds,
    Integer netheriteVillageIgniteSeconds,
    // Zombie
    Double zombieRottenFleshHealAmount,
    Boolean zombieHungerEnabled,
    Integer zombieHungerSeconds,
    Integer zombieHungerAmplifier,
    Boolean zombieNauseaEnabled,
    Integer zombieNauseaSeconds,
    Integer zombieNauseaAmplifier,
    Boolean zombiePoisonEnabled,
    Integer zombiePoisonSeconds,
    Integer zombiePoisonAmplifier,
    Boolean zombieConvertVillagersEnabled,
    Double zombieVillagerConversionChance,
    Boolean zombieConvertWanderingTradersEnabled,
    Double zombieWanderingTraderConversionChance,
    // Redstone
    Boolean redstoneOverchargeEnabled,
    Double redstoneOverchargeHealthThresholdPercent,
    Double redstoneOverchargeDurationSeconds,
    Double redstoneOverchargeCooldownSeconds,
    Double redstoneOverchargeAttackMultiplier,
    Integer redstoneOverchargeResistanceAmplifier,
    Double redstoneOverchargeResistanceRefreshSeconds,
    Boolean redstoneDeathPulseEnabled,
    Integer redstoneDeathPulseRadius,
    Double redstoneDeathPulseSlownessSeconds,
    Integer redstoneDeathPulseSlownessAmplifier,
    Boolean redstoneParticlesEnabled,
    Boolean redstoneDeathPulseParticlesEnabled,
    // Lapis
    Boolean lapisWardEnabled,
    Integer lapisWardRange,
    Integer lapisWardScanIntervalTicks,
    Boolean lapisWardAffectsPlayers,
    Boolean lapisWardMagicDamageEnabled,
    Boolean lapisWardEffectCleanupEnabled,
    List<String> lapisWardEffectIds,
    Boolean lapisParticlesEnabled
) {

    public static final int MIN_HEALTH = 1;
    public static final int MAX_HEALTH = 2048;
    public static final double MIN_DAMAGE = 0.0;
    public static final double MAX_DAMAGE = 2048.0;

    public TierStats {
        Objects.requireNonNull(ignoredTargetTypes, "ignoredTargetTypes");
        ignoredTargetTypes = List.copyOf(ignoredTargetTypes);
        if (lapisWardEffectIds != null) {
            lapisWardEffectIds = List.copyOf(lapisWardEffectIds);
        }
    }

    public TierStats(
        int maxHealth,
        double attackDamage,
        boolean angerOnHit,
        List<String> ignoredTargetTypes,
        Boolean copperLightningImmune,
        Double copperLightningHealAmount,
        Double goldSpeedMultiplier,
        Boolean goldSprintParticlesEnabled,
        Boolean goldSunlightShineEnabled,
        Integer emeraldAuraRange,
        Double emeraldHealIntervalSeconds,
        Double emeraldHealPerTick,
        Boolean emeraldCountWanderingTraders,
        String diamondTargetMode,
        Integer diamondCooldownMinSeconds,
        Integer diamondCooldownMaxSeconds,
        Integer diamondAuraRange,
        Boolean diamondLightningProof,
        Boolean netheriteFireImmune,
        Integer netheriteIgniteSeconds,
        Integer netheriteVillageIgniteSeconds
    ) {
        this(maxHealth, attackDamage, angerOnHit, ignoredTargetTypes,
            copperLightningImmune, copperLightningHealAmount,
            goldSpeedMultiplier, goldSprintParticlesEnabled, goldSunlightShineEnabled,
            emeraldAuraRange, emeraldHealIntervalSeconds, emeraldHealPerTick, emeraldCountWanderingTraders,
            diamondTargetMode, diamondCooldownMinSeconds, diamondCooldownMaxSeconds, diamondAuraRange, diamondLightningProof,
            netheriteFireImmune, netheriteIgniteSeconds, netheriteVillageIgniteSeconds,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);
    }

    public TierStats(
        int maxHealth,
        double attackDamage,
        boolean angerOnHit,
        List<String> ignoredTargetTypes,
        Boolean copperLightningImmune,
        Double copperLightningHealAmount,
        Double goldSpeedMultiplier,
        Boolean goldSprintParticlesEnabled,
        Boolean goldSunlightShineEnabled,
        Integer emeraldAuraRange,
        Double emeraldHealIntervalSeconds,
        Double emeraldHealPerTick,
        Boolean emeraldCountWanderingTraders,
        String diamondTargetMode,
        Integer diamondCooldownMinSeconds,
        Integer diamondCooldownMaxSeconds,
        Integer diamondAuraRange,
        Boolean diamondLightningProof,
        Boolean netheriteFireImmune,
        Integer netheriteIgniteSeconds,
        Integer netheriteVillageIgniteSeconds,
        Double zombieRottenFleshHealAmount,
        Boolean zombieHungerEnabled,
        Integer zombieHungerSeconds,
        Integer zombieHungerAmplifier,
        Boolean zombieNauseaEnabled,
        Integer zombieNauseaSeconds,
        Integer zombieNauseaAmplifier,
        Boolean zombiePoisonEnabled,
        Integer zombiePoisonSeconds,
        Integer zombiePoisonAmplifier,
        Boolean zombieConvertVillagersEnabled,
        Double zombieVillagerConversionChance,
        Boolean zombieConvertWanderingTradersEnabled,
        Double zombieWanderingTraderConversionChance
    ) {
        this(maxHealth, attackDamage, angerOnHit, ignoredTargetTypes,
            copperLightningImmune, copperLightningHealAmount,
            goldSpeedMultiplier, goldSprintParticlesEnabled, goldSunlightShineEnabled,
            emeraldAuraRange, emeraldHealIntervalSeconds, emeraldHealPerTick, emeraldCountWanderingTraders,
            diamondTargetMode, diamondCooldownMinSeconds, diamondCooldownMaxSeconds, diamondAuraRange, diamondLightningProof,
            netheriteFireImmune, netheriteIgniteSeconds, netheriteVillageIgniteSeconds,
            zombieRottenFleshHealAmount, zombieHungerEnabled, zombieHungerSeconds, zombieHungerAmplifier,
            zombieNauseaEnabled, zombieNauseaSeconds, zombieNauseaAmplifier,
            zombiePoisonEnabled, zombiePoisonSeconds, zombiePoisonAmplifier,
            zombieConvertVillagersEnabled, zombieVillagerConversionChance,
            zombieConvertWanderingTradersEnabled, zombieWanderingTraderConversionChance,
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);
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
            netheriteFireImmune, netheriteIgniteSeconds, netheriteVillageIgniteSeconds,
            zombieRottenFleshHealAmount, zombieHungerEnabled, zombieHungerSeconds, zombieHungerAmplifier,
            zombieNauseaEnabled, zombieNauseaSeconds, zombieNauseaAmplifier,
            zombiePoisonEnabled, zombiePoisonSeconds, zombiePoisonAmplifier,
            zombieConvertVillagersEnabled, zombieVillagerConversionChance,
            zombieConvertWanderingTradersEnabled, zombieWanderingTraderConversionChance,
            redstoneOverchargeEnabled, redstoneOverchargeHealthThresholdPercent,
            redstoneOverchargeDurationSeconds, redstoneOverchargeCooldownSeconds,
            redstoneOverchargeAttackMultiplier, redstoneOverchargeResistanceAmplifier,
            redstoneOverchargeResistanceRefreshSeconds, redstoneDeathPulseEnabled,
            redstoneDeathPulseRadius, redstoneDeathPulseSlownessSeconds,
            redstoneDeathPulseSlownessAmplifier, redstoneParticlesEnabled,
            redstoneDeathPulseParticlesEnabled, lapisWardEnabled, lapisWardRange,
            lapisWardScanIntervalTicks, lapisWardAffectsPlayers,
            lapisWardMagicDamageEnabled, lapisWardEffectCleanupEnabled,
            lapisWardEffectIds, lapisParticlesEnabled);
    }

    public boolean isHealthDamageClamped() {
        return maxHealth < MIN_HEALTH || maxHealth > MAX_HEALTH
            || attackDamage < MIN_DAMAGE || attackDamage > MAX_DAMAGE;
    }
}
