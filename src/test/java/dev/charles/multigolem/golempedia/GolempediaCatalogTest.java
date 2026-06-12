package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.EnumMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GolempediaCatalogTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void includesEveryNonIronCatalogVariantRegardlessOfBuildableOrSpawnEggFlags() {
        EnumSet<GolemVariant> expected = GolemVariantCatalog.entries().stream()
            .map(GolemVariantSpec::variant)
            .filter(variant -> variant != GolemVariant.IRON)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(GolemVariant.class)));
        EnumSet<GolemVariant> actual = GolempediaCatalog.entries().stream()
            .map(GolempediaEntry::variant)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(GolemVariant.class)));

        assertEquals(expected, actual);
    }

    @Test
    void eachEntryHasRequiredPlayerFacingFields() {
        for (GolempediaEntry entry : GolempediaCatalog.entries()) {
            assertFalse(entry.displayName().isBlank(), entry.variant() + " display name");
            assertFalse(entry.creationSummary().isBlank(), entry.variant() + " creation");
            assertFalse(entry.healingItem().isBlank(), entry.variant() + " healing");
            assertFalse(entry.dropSummary().isBlank(), entry.variant() + " drops");
            assertFalse(entry.statLines().isEmpty(), entry.variant() + " stats");
            assertFalse(entry.spawnEggSummary().isBlank(), entry.variant() + " spawn egg");
            assertFalse(entry.villageSpawnSummary().isBlank(), entry.variant() + " village spawn");
            assertFalse(entry.coreAbility().isBlank(), entry.variant() + " ability");
            assertFalse(entry.caveats().isEmpty(), entry.variant() + " caveats");
        }
    }

    @Test
    void playerFacingCopyExplainsAbilitiesAndAvoidsInternalSpawnWeights() {
        assertEquals("Lightning does not hurt Copper golems; it heals them instead.",
            entry(GolemVariant.COPPER).coreAbility());
        assertEquals("Emerald golems heal themselves when villagers are nearby.",
            entry(GolemVariant.EMERALD).coreAbility());
        assertEquals("Diamond golems call lightning onto nearby hostile mobs after a cooldown.",
            entry(GolemVariant.DIAMOND).coreAbility());
        assertEquals("Zombie golems attack players, villagers, wandering traders, and non-zombie golems. "
                + "When they hit villagers or wandering traders, they can turn them into zombie villagers.",
            entry(GolemVariant.ZOMBIE).coreAbility());

        for (GolempediaEntry entry : GolempediaCatalog.entries()) {
            assertFalse(entry.villageSpawnSummary().contains("weight"), entry.variant() + " village spawn copy");
            assertFalse(entry.villageSpawnSummary().contains("default"), entry.variant() + " village spawn copy");
            assertFalse(entry.villageSpawnSummary().contains("bundled"), entry.variant() + " village spawn copy");
        }
    }

    @Test
    void defaultEntriesExposePlayerFacingStats() {
        GolempediaEntry copper = entry(GolemVariant.COPPER);

        assertTrue(copper.statLines().stream().anyMatch(line -> line.startsWith("Health:")));
        assertTrue(copper.statLines().stream().anyMatch(line -> line.startsWith("Attack:")));
        assertFalse(copper.statLines().stream().anyMatch(line -> line.contains("null")));
    }

    @Test
    void villageSpawnCopyDescribesFrequencyFromConfiguredWeights() {
        assertTrue(entry(GolemVariant.GOLD).villageSpawnSummary().contains("About 23%"));
        assertTrue(entry(GolemVariant.GOLD).villageSpawnSummary().contains("roughly 1 in 4"));
        assertTrue(entry(GolemVariant.DIAMOND).villageSpawnSummary().contains("About 6%"));
        assertTrue(entry(GolemVariant.DIAMOND).villageSpawnSummary().contains("roughly 1 in 16"));
        assertTrue(entry(GolemVariant.NETHERITE).villageSpawnSummary().contains("Does not spawn"));

        EnumMap<GolemVariant, Integer> custom = new EnumMap<>(GolemVariant.class);
        custom.put(GolemVariant.IRON, 0);
        custom.put(GolemVariant.COPPER, 0);
        custom.put(GolemVariant.GOLD, 1);
        custom.put(GolemVariant.EMERALD, 0);
        custom.put(GolemVariant.DIAMOND, 1);
        custom.put(GolemVariant.NETHERITE, 0);

        assertEquals("About 50% of village-spawned golems (roughly 1 in 2).",
            GolempediaVillageSpawns.summary(GolemVariant.GOLD, true, custom, true));
    }

    @Test
    void golempediaDoesNotDependOnServerCustomizationsState() throws IOException {
        String source = Files.readString(Path.of("src/common/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java"));

        assertTrue(source.contains("GolemVariantCatalog.entries()"));
        assertTrue(source.contains("MultiGolemConfig.defaults()"));
        assertFalse(source.contains("ServerCustomizations"));
        assertFalse(source.contains("MultiGolemStatus"));
    }

    private static GolempediaEntry entry(GolemVariant variant) {
        return GolempediaCatalog.entries().stream()
            .filter(entry -> entry.variant() == variant)
            .findFirst()
            .orElseThrow();
    }
}
