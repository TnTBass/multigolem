package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetheriteAbilityTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void fireImmunityPolicy_onlyAppliesToEnabledNetheriteTier() {
        assertTrue(NetheriteAbility.isFireImmuneVariant(GolemVariant.NETHERITE, true));
        assertFalse(NetheriteAbility.isFireImmuneVariant(GolemVariant.NETHERITE, false));
        assertFalse(NetheriteAbility.isFireImmuneVariant(GolemVariant.DIAMOND, true));
    }
}
