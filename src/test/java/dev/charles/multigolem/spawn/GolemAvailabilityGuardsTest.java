package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.GolemAvailability;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemAvailabilityGuardsTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void canCreateRejectsDisabledVariantAndAllowsAvailableVariant() {
        MultiGolemConfig config = MultiGolemConfig.defaults().withGolemAvailability(
            GolemAvailability.defaults()
                .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.DIAMOND, false));

        assertFalse(GolemAvailabilityGuards.canCreate(config, GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
        assertTrue(GolemAvailabilityGuards.canCreate(config, GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
    }
}
