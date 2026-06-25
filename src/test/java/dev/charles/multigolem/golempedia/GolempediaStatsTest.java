package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolempediaStatsTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void redstoneStatsHandleMissingAbilityFields() {
        List<String> lines = GolempediaStats.linesFor(GolemVariant.REDSTONE, redstoneStatsWithoutRedstoneFields());

        assertTrue(lines.contains("Overcharge: at or below 25% health"));
        assertTrue(lines.contains("Overcharge attack: 1.5x"));
        assertTrue(lines.contains("Overcharge resistance: II"));
        assertTrue(lines.contains("Death pulse: Slowness X for 6s in 8 blocks"));
        assertFalse(lines.stream().anyMatch(line -> line.contains("null")));
    }

    private static TierStats redstoneStatsWithoutRedstoneFields() {
        // Use the legacy compatibility constructor intentionally: it predates Redstone-specific fields.
        return new TierStats(
            90,
            13.0,
            true,
            List.of("CREEPERS"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    }
}
