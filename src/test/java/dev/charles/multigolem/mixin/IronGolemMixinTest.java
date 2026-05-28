package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.ability.GolemCombatRules;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronGolemMixinTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void healAmountUsesZombieRottenFleshConfig() {
        TierStats zombie = new TierStats(100, 15.0, true, List.of(),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null,
            40.0, true, 12, 0,
            true, 4, 0,
            true, 4, 0,
            true, 1.0, true, 1.0);

        assertEquals(40.0F, GolemCombatRules.healAmount(GolemVariant.ZOMBIE, zombie), 0.0001F);
    }

    @Test
    void healAmountDefaultsOtherVariantsToTwentyFive() {
        TierStats gold = new TierStats(130, 22.5, true, List.of("CREEPERS"),
            null, null,
            1.75, true, true,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null);

        assertEquals(25.0F, GolemCombatRules.healAmount(GolemVariant.GOLD, gold), 0.0001F);
    }
}
