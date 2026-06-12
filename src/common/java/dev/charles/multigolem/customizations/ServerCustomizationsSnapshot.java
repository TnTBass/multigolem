package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ServerCustomizationsSnapshot(
    boolean healingEnabled,
    boolean villageSpawnsEnabled,
    Map<GolemVariant, Integer> villageSpawnWeights,
    boolean zombieVillageSpawningEnabled,
    String permissionsMode,
    List<VariantCustomizationSummary> variantOverrides,
    Map<GolemVariant, List<String>> golempediaStats
) {
    public ServerCustomizationsSnapshot {
        Objects.requireNonNull(villageSpawnWeights, "villageSpawnWeights");
        permissionsMode = permissionsMode == null || permissionsMode.isBlank() ? "permissions unavailable" : permissionsMode;
        Objects.requireNonNull(variantOverrides, "variantOverrides");
        villageSpawnWeights = Collections.unmodifiableMap(new EnumMap<>(villageSpawnWeights));
        variantOverrides = List.copyOf(variantOverrides);
        Objects.requireNonNull(golempediaStats, "golempediaStats");
        EnumMap<GolemVariant, List<String>> statsCopy = new EnumMap<>(GolemVariant.class);
        for (Map.Entry<GolemVariant, List<String>> entry : golempediaStats.entrySet()) {
            statsCopy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        golempediaStats = Collections.unmodifiableMap(statsCopy);
    }
}
