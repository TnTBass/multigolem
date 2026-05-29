package dev.charles.multigolem.identity;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemIdentityTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void ironFamilyHasStableSavedId() {
        assertEquals("iron_golem", GolemFamily.IRON_GOLEM.id());
        assertEquals(GolemFamily.IRON_GOLEM, GolemFamily.fromId("iron_golem").orElseThrow());
        assertTrue(GolemFamily.fromId("copper_golem").isEmpty());
    }

    @Test
    void defaultIdentityIsIronFamilyIronVariant() {
        assertEquals(new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON), GolemIdentity.defaultIron());
        assertTrue(GolemIdentity.defaultIron().isDefaultIron());
    }

    @Test
    void phaseTwoAcceptsOnlyIronFamilyIdentitiesWithVariants() {
        assertTrue(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND).isValidForPhase2());
        assertFalse(new GolemIdentity(null, GolemVariant.DIAMOND).isValidForPhase2());
        assertFalse(new GolemIdentity(GolemFamily.IRON_GOLEM, null).isValidForPhase2());
    }
}
