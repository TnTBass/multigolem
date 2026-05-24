package dev.charles.multigolem.attribute;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VariantAttributesTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void freshSpawnHealthUsesConfiguredVariantMaximum() {
        MultiGolemConfig config = MultiGolemConfig.defaults();

        assertEquals(350.0F, VariantAttributes.freshSpawnHealth(GolemVariant.DIAMOND, config));
        assertEquals(600.0F, VariantAttributes.freshSpawnHealth(GolemVariant.NETHERITE, config));
    }
}
