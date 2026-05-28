package dev.charles.multigolem.mixin;

import dev.charles.multigolem.ability.GolemCombatRules;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.config.TierStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronGolemAttackMixinTest {

    @Test
    void netheriteIgniteSeconds_usesVillageOverrideOnlyForVillageOrigin() {
        TierStats stats = new TierStats(600, 85.0, true, List.of(),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            true, 7, 3);

        assertEquals(3,
            GolemCombatRules.netheriteIgniteSeconds(stats, GolemSpawnOrigin.VILLAGE));
        assertEquals(7,
            GolemCombatRules.netheriteIgniteSeconds(stats, GolemSpawnOrigin.UNKNOWN));
    }

    @Test
    void netheriteIgniteSeconds_fallsBackToBaseWhenVillageOverrideIsAbsent() {
        TierStats stats = new TierStats(600, 85.0, true, List.of(),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            true, 7, null);

        assertEquals(7,
            GolemCombatRules.netheriteIgniteSeconds(stats, GolemSpawnOrigin.VILLAGE));
    }
}
