package dev.charles.multigolem.attachment;

import com.mojang.serialization.JsonOps;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void scanBackoff_blocksUntilExpiry() {
        long now = 1000L;
        GolemAbilityState s = GolemAbilityState.fresh().withDiamondScanBackoff(now + 40L);

        // Blocked during backoff window
        assertFalse(s.diamondScanReady(now));
        assertFalse(s.diamondScanReady(now + 39L));

        // Ready exactly at expiry and after
        assertTrue(s.diamondScanReady(now + 40L));
        assertTrue(s.diamondScanReady(now + 100L));
    }

    @Test
    void scanBackoff_doesNotAffectAbilityCooldown() {
        long now = 1000L;
        // Ability cooldown is independent of scan backoff
        GolemAbilityState s = GolemAbilityState.fresh()
            .withDiamondCooldown(now + 600L)
            .withDiamondScanBackoff(now + 40L);

        assertFalse(s.diamondCooldownReady(now));
        assertFalse(s.diamondScanReady(now));

        // Scan backoff expires, but ability cooldown still blocks
        assertTrue(s.diamondScanReady(now + 40L));
        assertFalse(s.diamondCooldownReady(now + 40L));
    }

    @Test
    void scanBackoff_roundTrips() {
        GolemAbilityState s = new GolemAbilityState(500L, 999L);
        var encoded = GolemAbilityState.CODEC.encodeStart(JsonOps.INSTANCE, s).result().orElseThrow();
        GolemAbilityState decoded = GolemAbilityState.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertEquals(s.nextDiamondScanGameTime(), decoded.nextDiamondScanGameTime());
    }
}
