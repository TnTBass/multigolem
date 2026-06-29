package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.config.GolemAvailability;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.golempedia.GolempediaStats;
import dev.charles.multigolem.golempedia.GolempediaVillageSpawns;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.spawn.VillageSpawnWeights;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public final class ServerCustomizationsSummarizer {
    private static final String PERMISSIONS_MODE =
        "permission checks use the server's configured permission provider when present";

    private ServerCustomizationsSummarizer() {
    }

    public static ServerCustomizationsSnapshot snapshot(MultiGolemConfig config) {
        Objects.requireNonNull(config, "config");
        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
            var identity = GolemVariantCatalog.require(variant).identity();
            weights.put(variant, config.golemAvailability().isAvailable(identity)
                ? config.villageSpawnWeights().weight(variant)
                : 0);
        }
        EnumMap<GolemVariant, List<String>> stats = new EnumMap<>(GolemVariant.class);
        for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
            if (spec.variant() != GolemVariant.IRON && config.golemAvailability().isAvailable(spec.identity())) {
                stats.put(spec.variant(), GolempediaStats.linesFor(spec.variant(), config.tier(spec.variant())));
            }
        }
        List<String> disabledAvailabilityLines = disabledAvailabilityLines(config.golemAvailability());
        List<VariantCustomizationSummary> overrides = variantOverrides(config, MultiGolemConfig.defaults());
        return new ServerCustomizationsSnapshot(
            config.allowGolemHealing(),
            config.villageSpawnWeights().enabled(),
            weights,
            config.zombieVillageSpawning().enabled(),
            PERMISSIONS_MODE,
            overrides,
            stats,
            disabledAvailabilityLines
        );
    }

    public static ServerCustomizationsSummary summary(MultiGolemConfig config) {
        return summary(snapshot(config));
    }

    public static ServerCustomizationsSummary summary(ServerCustomizationsSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<String> global = new ArrayList<>();
        List<String> village = new ArrayList<>();
        List<String> zombieVillage = new ArrayList<>();
        List<String> variants = new ArrayList<>();

        global.add("Global healing: " + enabledText(snapshot.healingEnabled()));

        for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
            village.add(variant.displayName() + ": " + GolempediaVillageSpawns.summary(
                variant,
                snapshot.villageSpawnsEnabled(),
                snapshot.villageSpawnWeights(),
                snapshot.zombieVillageSpawningEnabled()
            ));
        }

        zombieVillage.add("Zombie village spawning: " + enabledText(snapshot.zombieVillageSpawningEnabled()));

        variants.addAll(snapshot.disabledAvailabilityLines());
        for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
            GolemVariant variant = spec.variant();
            if (variant == GolemVariant.IRON) {
                continue;
            }
            List<String> statLines = snapshot.golempediaStats().getOrDefault(variant, List.of());
            if (!statLines.isEmpty()) {
                variants.add(variant.displayName() + ": " + String.join(", ", statLines));
            }
        }
        for (VariantCustomizationSummary override : snapshot.variantOverrides()) {
            for (String line : override.lines()) {
                if (!variants.contains(line)) {
                    variants.add(line);
                }
            }
        }

        return new ServerCustomizationsSummary(global, village, zombieVillage, variants);
    }

    private static List<String> disabledAvailabilityLines(GolemAvailability availability) {
        List<String> lines = new ArrayList<>();
        for (GolemFamily family : GolemFamily.values()) {
            if (!availability.isFamilyAvailable(family)) {
                lines.add(familyDisplayName(family) + " family: disabled by server availability");
                continue;
            }
            for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
                if (spec.family() == family && spec.variant() != GolemVariant.IRON && !availability.isAvailable(spec.identity())) {
                    lines.add(spec.variant().displayName() + ": disabled by server availability");
                }
            }
        }
        return lines;
    }

    private static String familyDisplayName(GolemFamily family) {
        return switch (family) {
            case IRON_GOLEM -> "Iron Golem";
            case COPPER_GOLEM -> "Copper Golem";
        };
    }

    private static List<VariantCustomizationSummary> variantOverrides(MultiGolemConfig config, MultiGolemConfig defaults) {
        List<VariantCustomizationSummary> overrides = new ArrayList<>();
        for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
            GolemVariant variant = spec.variant();
            TierStats current = config.tier(variant);
            TierStats bundled = defaults.tier(variant);
            if (config.golemAvailability().isAvailable(spec.identity()) && !Objects.equals(current, bundled)) {
                overrides.add(new VariantCustomizationSummary(
                    variant,
                    List.of(variant.displayName() + " settings differ from bundled defaults")
                ));
            }
        }
        return overrides;
    }

    private static String enabledText(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }
}
