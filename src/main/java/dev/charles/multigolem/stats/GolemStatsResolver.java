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
}
