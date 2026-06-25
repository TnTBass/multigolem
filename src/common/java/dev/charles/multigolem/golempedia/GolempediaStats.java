package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.TierStats;

import java.util.ArrayList;
import java.util.List;

public final class GolempediaStats {
    private GolempediaStats() {
    }

    public static List<String> linesFor(GolemVariant variant, TierStats stats) {
        List<String> lines = new ArrayList<>();
        lines.add("Health: " + stats.maxHealth());
        lines.add("Attack: " + number(stats.attackDamage()));
        addAbilityStats(variant, stats, lines);
        return List.copyOf(lines);
    }

    private static void addAbilityStats(GolemVariant variant, TierStats stats, List<String> lines) {
        switch (variant) {
            case COPPER -> {
                if (Boolean.TRUE.equals(stats.copperLightningImmune())) {
                    lines.add("Lightning: heals instead of hurting");
                    lines.add("Lightning heal: " + (stats.copperLightningHealAmount() == null
                        ? "full missing health"
                        : number(stats.copperLightningHealAmount())));
                }
            }
            case GOLD -> {
                if (stats.goldSpeedMultiplier() != null) {
                    lines.add("Speed: " + number(stats.goldSpeedMultiplier()) + "x");
                }
            }
            case REDSTONE -> {
                lines.add("Overcharge: at or below " + percent(stats.redstoneOverchargeHealthThresholdPercent()) + " health");
                lines.add("Overcharge attack: " + number(stats.redstoneOverchargeAttackMultiplier()) + "x");
                lines.add("Overcharge resistance: " + romanEffectLevel(stats.redstoneOverchargeResistanceAmplifier()));
                lines.add("Speed: no bonus");
                lines.add("Death pulse: Slowness " + romanEffectLevel(stats.redstoneDeathPulseSlownessAmplifier())
                    + " for " + number(stats.redstoneDeathPulseSlownessSeconds()) + "s in "
                    + stats.redstoneDeathPulseRadius() + " blocks");
            }
            case EMERALD -> {
                lines.add("Aura range: " + stats.emeraldAuraRange() + " blocks");
                lines.add("Heal: " + number(stats.emeraldHealPerTick()) + " every "
                    + number(stats.emeraldHealIntervalSeconds()) + "s");
            }
            case DIAMOND -> {
                lines.add("Lightning range: " + stats.diamondAuraRange() + " blocks");
                lines.add("Cooldown: " + stats.diamondCooldownMinSeconds() + "-"
                    + stats.diamondCooldownMaxSeconds() + "s");
            }
            case NETHERITE -> {
                if (Boolean.TRUE.equals(stats.netheriteFireImmune())) {
                    lines.add("Fire: immune");
                }
                lines.add("Ignites attackers: " + stats.netheriteIgniteSeconds() + "s");
            }
            case ZOMBIE -> {
                lines.add("Rotten flesh heal: " + number(stats.zombieRottenFleshHealAmount()));
                lines.add("Villager conversion chance: " + percent(stats.zombieVillagerConversionChance()));
                lines.add("Trader conversion chance: " + percent(stats.zombieWanderingTraderConversionChance()));
            }
            case IRON -> {
            }
        }
    }

    private static String number(double value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String percent(double value) {
        return number(value * 100.0) + "%";
    }

    private static String romanEffectLevel(int zeroBasedAmplifier) {
        return switch (Math.max(0, zeroBasedAmplifier) + 1) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(Math.max(0, zeroBasedAmplifier) + 1);
        };
    }
}
