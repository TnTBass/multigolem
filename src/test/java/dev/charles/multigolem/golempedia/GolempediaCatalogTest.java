package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
            assertFalse(entry.spawnEggSummary().isBlank(), entry.variant() + " spawn egg");
            assertFalse(entry.villageSpawnSummary().isBlank(), entry.variant() + " village spawn");
            assertFalse(entry.coreAbility().isBlank(), entry.variant() + " ability");
            assertFalse(entry.caveats().isEmpty(), entry.variant() + " caveats");
        }
    }

    @Test
    void golempediaDoesNotDependOnServerCustomizationsState() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/charles/multigolem/golempedia/GolempediaCatalog.java"));

        assertTrue(source.contains("GolemVariantCatalog.entries()"));
        assertTrue(source.contains("MultiGolemConfig.defaults()"));
        assertFalse(source.contains("ServerCustomizations"));
        assertFalse(source.contains("MultiGolemStatus"));
    }
}
