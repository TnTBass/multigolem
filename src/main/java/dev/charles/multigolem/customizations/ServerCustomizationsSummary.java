package dev.charles.multigolem.customizations;

import java.util.List;
import java.util.Objects;

public record ServerCustomizationsSummary(
    List<String> globalLines,
    List<String> villageLines,
    List<String> zombieVillageLines,
    List<String> variantLines
) {
    public ServerCustomizationsSummary {
        Objects.requireNonNull(globalLines, "globalLines");
        Objects.requireNonNull(villageLines, "villageLines");
        Objects.requireNonNull(zombieVillageLines, "zombieVillageLines");
        Objects.requireNonNull(variantLines, "variantLines");
        globalLines = List.copyOf(globalLines);
        villageLines = List.copyOf(villageLines);
        zombieVillageLines = List.copyOf(zombieVillageLines);
        variantLines = List.copyOf(variantLines);
    }

    public boolean isEmpty() {
        return globalLines.isEmpty()
            && villageLines.isEmpty()
            && zombieVillageLines.isEmpty()
            && variantLines.isEmpty();
    }
}
