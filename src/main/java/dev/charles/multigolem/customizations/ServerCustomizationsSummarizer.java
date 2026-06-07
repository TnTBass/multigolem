package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.golempedia.GolempediaStats;
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
            weights.put(variant, config.villageSpawnWeights().weight(variant));
        }
        EnumMap<GolemVariant, List<String>> stats = new EnumMap<>(GolemVariant.class);
        for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
            if (spec.variant() != GolemVariant.IRON) {
                stats.put(spec.variant(), GolempediaStats.linesFor(spec.variant(), config.tier(spec.variant())));
            }
        }
        List<VariantCustomizationSummary> overrides = variantOverrides(config, MultiGolemConfig.defaults());
        return new ServerCustomizationsSnapshot(
            config.allowGolemHealing(),
            config.villageSpawnWeights().enabled(),
            weights,
            config.zombieVillageSpawning().enabled(),
            PERMISSIONS_MODE,
            overrides,
            stats
        );
    }

    public static ServerCustomizationsSummary summary(MultiGolemConfig config) {
        return summary(snapshot(config));
    }

    public static ServerCustomizationsSummary summary(ServerCustomizationsSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        MultiGolemConfig defaults = MultiGolemConfig.defaults();
        List<String> global = new ArrayList<>();
        List<String> village = new ArrayList<>();
        List<String> zombieVillage = new ArrayList<>();
        List<String> variants = new ArrayList<>();

        global.add("Global healing: " + enabledText(snapshot.healingEnabled()));

        if (!snapshot.villageSpawnsEnabled()) {
            village.add("Village golem spawning: disabled");
        }
        for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
            int current = snapshot.villageSpawnWeights().getOrDefault(variant, 0);
            int bundled = defaults.villageSpawnWeights().weight(variant);
            if (current != bundled) {
                village.add(variant.displayName() + " village spawning is customized by this server.");
            }
        }

        if (!snapshot.zombieVillageSpawningEnabled()) {
            zombieVillage.add("Zombie village spawning: disabled");
        }

        for (VariantCustomizationSummary override : snapshot.variantOverrides()) {
            for (String line : override.lines()) {
                variants.add(line);
            }
        }

        return new ServerCustomizationsSummary(global, village, zombieVillage, variants);
    }

    private static List<VariantCustomizationSummary> variantOverrides(MultiGolemConfig config, MultiGolemConfig defaults) {
        List<VariantCustomizationSummary> overrides = new ArrayList<>();
        for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
            GolemVariant variant = spec.variant();
            TierStats current = config.tier(variant);
            TierStats bundled = defaults.tier(variant);
            if (!Objects.equals(current, bundled)) {
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
