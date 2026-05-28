package dev.charles.multigolem.ability;

import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.effect.MobEffects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZombieGolemEffectsTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void defaultSicknessEffectsUseConfiguredDurations() {
        var effects = ZombieGolemEffects.effects(MultiGolemConfig.defaults().tier(dev.charles.multigolem.GolemVariant.ZOMBIE));

        assertEquals(3, effects.size());
        assertEquals(MobEffects.HUNGER, effects.get(0).getEffect());
        assertEquals(12 * 20, effects.get(0).getDuration());
        assertEquals(MobEffects.NAUSEA, effects.get(1).getEffect());
        assertEquals(4 * 20, effects.get(1).getDuration());
        assertEquals(MobEffects.POISON, effects.get(2).getEffect());
        assertEquals(4 * 20, effects.get(2).getDuration());
    }
}
