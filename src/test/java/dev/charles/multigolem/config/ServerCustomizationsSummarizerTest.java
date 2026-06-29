package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.GolemAvailability;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.customizations.ServerCustomizationsSnapshot;
import dev.charles.multigolem.customizations.ServerCustomizationsSummarizer;
import dev.charles.multigolem.customizations.ServerCustomizationsSummary;
import dev.charles.multigolem.customizations.VariantCustomizationSummary;
import dev.charles.multigolem.spawn.VillageSpawnWeights;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

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
        assertFalse(snapshot.golempediaStats().get(GolemVariant.COPPER).isEmpty());
        assertTrue(snapshot.golempediaStats().get(GolemVariant.COPPER).stream().anyMatch(line -> line.startsWith("Health:")));
        assertTrue(snapshot.golempediaStats().get(GolemVariant.REDSTONE).contains("Overcharge: at or below 25% health"));
        assertTrue(snapshot.golempediaStats().get(GolemVariant.REDSTONE).contains("Death pulse: Slowness X for 6s in 8 blocks"));
    }

    @Test
    void defaultSummaryStillReportsActiveServerValues() {
        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(MultiGolemConfig.defaults());

        assertTrue(summary.globalLines().stream().anyMatch(line -> line.contains("Global healing: enabled")));
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("Copper")));
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("roughly")));
        assertTrue(summary.zombieVillageLines().stream().anyMatch(line -> line.contains("Zombie village spawning: enabled")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Copper")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Redstone")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Overcharge: at or below 25% health")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Death pulse: Slowness X")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Health:")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Attack:")));
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
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("roughly")));
        assertFalse(summary.villageLines().stream().anyMatch(line -> line.contains("weight")));
    }

    @Test
    void summaryMarksDisabledVariantsWhenServerAvailabilityIsKnown() {
        MultiGolemConfig cfg = MultiGolemConfig.defaults()
            .withGolemAvailability(GolemAvailability.defaults()
                .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.DIAMOND, false));

        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(cfg);

        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Diamond") && line.contains("disabled")));
        assertFalse(summary.villageLines().stream().anyMatch(line -> line.startsWith("Diamond:") && line.contains("roughly")));
    }

    @Test
    void summaryMarksDisabledFamilyOnceWithoutListingEnabledVariantOverridesAsAvailable() {
        MultiGolemConfig cfg = MultiGolemConfig.defaults()
            .withGolemAvailability(GolemAvailability.defaults()
                .withFamily(GolemFamily.IRON_GOLEM, false)
                .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.COPPER, true));

        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(cfg);

        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Iron Golem family") && line.contains("disabled")));
        assertFalse(summary.variantLines().stream().anyMatch(line -> line.startsWith("Copper: Health:")));
    }

    @Test
    void villageSummaryAvailabilityUsesCatalogIdentity() throws IOException {
        String source = Files.readString(Path.of(
            "src/common/java/dev/charles/multigolem/customizations/ServerCustomizationsSummarizer.java"));

        assertTrue(source.contains("GolemVariantCatalog.require(variant).identity()"));
        assertFalse(source.contains("GolemIdentity.ofIronVariant(variant)"));
    }

    @Test
    void summaryOmitsPermissionProviderInternals() {
        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(MultiGolemConfig.defaults());

        assertFalse(summary.globalLines().stream().anyMatch(line -> line.contains("Permissions")));
        assertFalse(summary.globalLines().stream().anyMatch(line -> line.contains("permission provider")));
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
            List.of(new VariantCustomizationSummary(GolemVariant.GOLD, List.of("Gold speed multiplier differs from defaults"))),
            new EnumMap<>(GolemVariant.class)
        );

        ServerCustomizationsSummary summary = ServerCustomizationsSummarizer.summary(snapshot);

        assertTrue(summary.globalLines().stream().anyMatch(line -> line.contains("disabled")));
        assertTrue(summary.villageLines().stream().anyMatch(line -> line.contains("Gold")));
        assertTrue(summary.variantLines().stream().anyMatch(line -> line.contains("Gold speed multiplier")));
    }
}
