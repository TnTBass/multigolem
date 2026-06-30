package dev.charles.multigolem.stats;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;

import java.util.List;

public final class GolemStatsResolver {

    private final MultiGolemConfig config;

    public GolemStatsResolver(MultiGolemConfig config) { this.config = config; }

    // V1
    public int maxHealth(GolemVariant v) { return config.tier(v).maxHealth(); }
    public double attackDamage(GolemVariant v) { return config.tier(v).attackDamage(); }
    public boolean angerOnHit(GolemVariant v) { return config.tier(v).angerOnHit(); }
    public TierStats stats(GolemVariant v) { return config.tier(v); }

    // V2 cross-tier
    public List<String> ignoredTargetTypes(GolemVariant v) { return config.tier(v).ignoredTargetTypes(); }

    // V2 copper-only
    public boolean copperLightningImmune() { return config.tier(GolemVariant.COPPER).copperLightningImmune(); }
    public Double copperLightningHealAmount() { return config.tier(GolemVariant.COPPER).copperLightningHealAmount(); }

    // V2 gold-only
    public double goldSpeedMultiplier() { return config.tier(GolemVariant.GOLD).goldSpeedMultiplier(); }
    public boolean goldSprintParticlesEnabled() { return config.tier(GolemVariant.GOLD).goldSprintParticlesEnabled(); }
    public boolean goldSunlightShineEnabled() { return config.tier(GolemVariant.GOLD).goldSunlightShineEnabled(); }

    // V2 emerald-only
    public int emeraldAuraRange() { return config.tier(GolemVariant.EMERALD).emeraldAuraRange(); }
    public double emeraldHealIntervalSeconds() { return config.tier(GolemVariant.EMERALD).emeraldHealIntervalSeconds(); }
    public double emeraldHealPerTick() { return config.tier(GolemVariant.EMERALD).emeraldHealPerTick(); }
    public boolean emeraldCountWanderingTraders() { return config.tier(GolemVariant.EMERALD).emeraldCountWanderingTraders(); }

    // V2 diamond-only
    public String diamondTargetMode() { return config.tier(GolemVariant.DIAMOND).diamondTargetMode(); }
    public int diamondCooldownMinSeconds() { return config.tier(GolemVariant.DIAMOND).diamondCooldownMinSeconds(); }
    public int diamondCooldownMaxSeconds() { return config.tier(GolemVariant.DIAMOND).diamondCooldownMaxSeconds(); }
    public int diamondAuraRange() { return config.tier(GolemVariant.DIAMOND).diamondAuraRange(); }
    public boolean diamondLightningProof() { return config.tier(GolemVariant.DIAMOND).diamondLightningProof(); }

    // V2 netherite-only
    public boolean netheriteFireImmune() { return config.tier(GolemVariant.NETHERITE).netheriteFireImmune(); }
    public int netheriteIgniteSeconds() { return config.tier(GolemVariant.NETHERITE).netheriteIgniteSeconds(); }
    public int netheriteVillageIgniteSeconds() { return config.tier(GolemVariant.NETHERITE).netheriteVillageIgniteSeconds(); }

    // V8 lapis-only
    public boolean lapisWardEnabled() { return config.tier(GolemVariant.LAPIS).lapisWardEnabled(); }
    public int lapisWardRange() { return config.tier(GolemVariant.LAPIS).lapisWardRange(); }
    public int lapisWardScanIntervalTicks() { return config.tier(GolemVariant.LAPIS).lapisWardScanIntervalTicks(); }
    public boolean lapisWardAffectsPlayers() { return config.tier(GolemVariant.LAPIS).lapisWardAffectsPlayers(); }
    public boolean lapisWardMagicDamageEnabled() { return config.tier(GolemVariant.LAPIS).lapisWardMagicDamageEnabled(); }
    public boolean lapisWardEffectCleanupEnabled() { return config.tier(GolemVariant.LAPIS).lapisWardEffectCleanupEnabled(); }
    public List<String> lapisWardEffectIds() { return config.tier(GolemVariant.LAPIS).lapisWardEffectIds(); }
    public boolean lapisParticlesEnabled() { return config.tier(GolemVariant.LAPIS).lapisParticlesEnabled(); }
}
