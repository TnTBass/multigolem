package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.customizations.ServerCustomizationsSnapshot;
import dev.charles.multigolem.customizations.ServerCustomizationsSummarizer;
import dev.charles.multigolem.customizations.ServerCustomizationsSummary;
import dev.charles.multigolem.customizations.VariantCustomizationSummary;
import dev.charles.multigolem.spawn.VillageSpawnWeights;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsSummarizerTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void defaultConfigSnapshotReportsNoVariantOverrides() {
        ServerCustomizationsSnapshot snapshot = ServerCustomizationsSummarizer.snapshot(MultiGolemConfig.defaults());

        assertTrue(snapshot.healingEnabled());
        assertTrue(snapshot.villageSpawnsEnabled());
        assertTrue(snapshot.zombieVillageSpawningEnabled());
        assertTrue(snapshot.variantOverrides().isEmpty());
    }

    @Test
    void nonDefaultVillageWeightIsReported() {
        EnumMap<GolemVariant, Integer> weights = VillageSpawnWeights.defaults().weights();
        weights.put(GolemVariant.COPPER, VillageSpawnWeights.defaults().weight(GolemVariant.COPPER) + 5);
        MultiGolemConfig customized = MultiGolemConfig.forTesting(
            MultiGolemConfig.defaults().allowGolemHealing(),
            MultiGolemConfig.defaults().tiersForTesting(),
            new VillageSpawnWeights(true, weights),
            MultiGolemConfig.defaults().zombieVillageSpawning()
        );

        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(customized);

        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("Copper")));
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("24")));
    }

    @Test
    void snapshotSummaryOverloadSupportsClientPayloadSnapshots() {
        EnumMap<GolemVariant, Integer> weights = VillageSpawnWeights.defaults().weights();
        weights.put(GolemVariant.GOLD, 7);
        ServerCustomizationsSnapshot snapshot = new ServerCustomizationsSnapshot(
            false,
            true,
            weights,
            true,
            "permission checks use the server's configured permission provider when present",
            List.of(new VariantCustomizationSummary(GolemVariant.GOLD, List.of("Gold speed multiplier differs from defaults")))
        );

        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(snapshot);

        assertTrue(summary.globalLines().stream().anyMatch(line -> line.contains("disabled")));
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("Gold")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Gold speed multiplier")));
    }
}
