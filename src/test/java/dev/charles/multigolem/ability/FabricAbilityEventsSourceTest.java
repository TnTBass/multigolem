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
            "LapisAbility.java",
            "NetheriteAbility.java",
            "RedstoneAbility.java"
        }) {
            String source = Files.readString(Path.of("src/common/java/dev/charles/multigolem/ability", file));

            assertFalse(source.contains("net.fabricmc"), file);
            assertFalse(source.contains("ServerTickEvents"), file);
            assertFalse(source.contains("ServerLivingEntityEvents"), file);
            assertFalse(source.contains("ServerMobEffectEvents"), file);
        }
    }

    @Test
    void fabricAbilityAdapterOwnsFabricEventRegistration() throws IOException {
        String source = Files.readString(Path.of("src/fabric/java/dev/charles/multigolem/fabric/ability/FabricAbilityEvents.java"));

        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(GoldAbility::onTick);"));
        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(EmeraldAbility::onTick);"));
        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(DiamondAbility::onTick);"));
        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(RedstoneAbility::onTick);"));
        assertTrue(source.contains("ServerTickEvents.START_LEVEL_TICK.register(LapisAbility::onTick);"));
        assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(CopperAbility::allowDamage);"));
        assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(NetheriteAbility::allowDamage);"));
        assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(DiamondAbility::allowDamage);"));
        assertTrue(source.contains("ServerLivingEntityEvents.ALLOW_DAMAGE.register(LapisAbility::allowDamage);"));
        assertTrue(source.contains("ServerMobEffectEvents.ALLOW_ADD.register"));
        assertTrue(source.contains("ServerMobEffectEvents.ALLOW_ADD.register((effect, entity, context) -> {"));
        assertTrue(source.contains("EffectEventContext is loader metadata; common Lapis ward logic only needs effect and target."));
        assertTrue(source.contains("LapisAbility.allowEffectApplication(effect, entity)"));
        assertFalse(source.contains("net.neoforged"));
    }

    @Test
    void fabricMixinConfigDeclaresRedstoneDeathPulseMixin() throws IOException {
        String mixins = Files.readString(Path.of("src/fabric/resources/multigolem.mixins.json"));

        assertTrue(mixins.contains("\"IronGolemDeathMixin\""));
    }
}
