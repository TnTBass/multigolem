package dev.charles.multigolem.attachment;

import com.mojang.serialization.JsonOps;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GolemAbilityStateTest {

    @BeforeAll
    static void bootstrap() { MinecraftBootstrap.ensure(); }

    @Test
    void empty_roundTrips() {
        GolemAbilityState empty = GolemAbilityState.fresh();
        var encoded = GolemAbilityState.CODEC.encodeStart(JsonOps.INSTANCE, empty).result().orElseThrow();
        GolemAbilityState decoded = GolemAbilityState.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertEquals(empty, decoded);
    }

    @Test
    void populated_roundTrips() {
        GolemAbilityState s = new GolemAbilityState(123456789L, 0L);
        var encoded = GolemAbilityState.CODEC.encodeStart(JsonOps.INSTANCE, s).result().orElseThrow();
        GolemAbilityState decoded = GolemAbilityState.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertEquals(s.nextDiamondAbilityGameTime(), decoded.nextDiamondAbilityGameTime());
    }

    @Test
    void fresh_hasZeroCooldown() {
        assertEquals(0L, GolemAbilityState.fresh().nextDiamondAbilityGameTime());
    }
}
