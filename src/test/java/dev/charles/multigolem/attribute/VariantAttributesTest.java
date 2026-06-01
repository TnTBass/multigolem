package dev.charles.multigolem.attribute;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
            VariantAttributes.freshSpawnHealth(oxidizedCopper.variant(), config));
    }
}
