package dev.charles.multigolem.attribute;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariantAttributesTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void freshSpawnHealthUsesConfiguredVariantMaximum() {
        MultiGolemConfig config = MultiGolemConfig.defaults();

        for (GolemVariant variant : List.of(
            GolemVariant.COPPER,
            GolemVariant.GOLD,
            GolemVariant.LAPIS,
            GolemVariant.EMERALD,
            GolemVariant.DIAMOND,
            GolemVariant.NETHERITE
        )) {
            assertEquals(config.tier(variant).maxHealth(), VariantAttributes.freshSpawnHealth(variant, config));
        }
    }

    @Test
    void surfaceCopperUsesCopperVariantStats() {
        MultiGolemConfig config = MultiGolemConfig.defaults();
        GolemIdentity oxidizedCopper = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true));

        assertEquals(config.tier(GolemVariant.COPPER).maxHealth(),
            VariantAttributes.freshSpawnHealth(GolemIdentity.ofIronVariant(GolemVariant.COPPER).variant(), config));
        assertEquals(config.tier(GolemVariant.COPPER).maxHealth(),
            VariantAttributes.freshSpawnHealth(oxidizedCopper.variant(), config));
    }

    @Test
    void lapisFreshSpawnHealthUsesConfiguredMaximum() {
        MultiGolemConfig config = MultiGolemConfig.defaults();

        assertEquals(50.0F, VariantAttributes.freshSpawnHealth(GolemVariant.LAPIS, config));
        assertEquals(config.tier(GolemVariant.LAPIS).maxHealth(),
            VariantAttributes.freshSpawnHealth(GolemVariant.LAPIS, config));
    }

    @Test
    void speedModifierRemainsGoldOnly() throws IOException {
        String source = Files.readString(Path.of("src/common/java/dev/charles/multigolem/attribute/VariantAttributes.java"));

        assertTrue(source.contains("double speedDelta = (variant == GolemVariant.GOLD)"));
        // Plan guard: Lapis must stay on the existing config-backed attack delta path.
        assertTrue(source.contains("stats.attackDamage() - IRON_BASE_ATTACK"));
        assertFalse(source.contains("GolemVariant.LAPIS) ?"), "Lapis must not add a special attack or speed branch");
        assertFalse(source.toLowerCase(Locale.ROOT).contains("redstone"));
    }
}
