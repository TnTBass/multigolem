package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;

import java.util.List;
import java.util.Objects;

public record GolempediaEntry(
    GolemVariant variant,
    String displayName,
    String creationSummary,
    String healingItem,
    String dropSummary,
    List<String> statLines,
    String spawnEggSummary,
    String villageSpawnSummary,
    String coreAbility,
    List<String> caveats
) {
    public GolempediaEntry {
        Objects.requireNonNull(variant, "variant");
        displayName = requireText(displayName, "displayName");
        creationSummary = requireText(creationSummary, "creationSummary");
        healingItem = requireText(healingItem, "healingItem");
        dropSummary = requireText(dropSummary, "dropSummary");
        Objects.requireNonNull(statLines, "statLines");
        statLines = List.copyOf(statLines);
        if (statLines.isEmpty()) {
            throw new IllegalArgumentException("statLines must not be empty");
        }
        spawnEggSummary = requireText(spawnEggSummary, "spawnEggSummary");
        villageSpawnSummary = requireText(villageSpawnSummary, "villageSpawnSummary");
        coreAbility = requireText(coreAbility, "coreAbility");
        Objects.requireNonNull(caveats, "caveats");
        caveats = List.copyOf(caveats);
        if (caveats.isEmpty()) {
            throw new IllegalArgumentException("caveats must not be empty");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
