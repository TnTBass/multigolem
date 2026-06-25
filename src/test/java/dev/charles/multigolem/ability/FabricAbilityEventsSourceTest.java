package dev.charles.multigolem.ability;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricAbilityEventsSourceTest {
    @Test
    void commonAbilityClassesDoNotOwnFabricEventRegistration() throws IOException {
        for (String file : new String[] {
            "CopperAbility.java",
            "DiamondAbility.java",
            "EmeraldAbility.java",
            "GoldAbility.java",
            "NetheriteAbility.java",
            "RedstoneAbility.java"
        }) {
            String source = Files.readString(Path.of("src/common/java/dev/charles/multigolem/ability", file));

            assertFalse(source.contains("net.fabricmc"), file);
            assertFalse(source.contains("ServerTickEvents"), file);
            assertFalse(source.contains("ServerLivingEntityEvents"), file);
        }
    }

    @Test
    void fabricAbilityAdapterOwnsFabricEventRegistration() throws IOException {
        String source = Files.readString(Path.of("src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java"));

        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(GoldAbility::onTick);"));
        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(EmeraldAbility::onTick);"));
        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(DiamondAbility::onTick);"));
        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(RedstoneAbility::onTick);"));
        assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(CopperAbility::allowDamage);"));
        assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(NetheriteAbility::allowDamage);"));
        assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(DiamondAbility::allowDamage);"));
    }

    @Test
    void fabricMixinConfigDeclaresRedstoneDeathPulseMixin() throws IOException {
        String mixins = Files.readString(Path.of("src/fabric/resources/multigolem.mixins.json"));

        assertTrue(mixins.contains("\"IronGolemDeathMixin\""));
    }
}
