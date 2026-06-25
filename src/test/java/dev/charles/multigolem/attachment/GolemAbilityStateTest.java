package dev.charles.multigolem.attachment;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
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

    @Test
    void clampDiamondCooldown_capsFarFutureCooldownsToCurrentConfigWindow() {
        long now = 1000L;
        GolemAbilityState s = GolemAbilityState.fresh()
            .withDiamondCooldown(now + 100_000L)
            .withDiamondScanBackoff(now + 40L);

        GolemAbilityState clamped = s.clampDiamondCooldown(now, 1200L);

        assertEquals(now + 1200L, clamped.nextDiamondAbilityGameTime());
        assertEquals(now + 40L, clamped.nextDiamondScanGameTime());
    }

    @Test
    void clampDiamondCooldown_preservesNormalCooldowns() {
        long now = 1000L;
        GolemAbilityState s = GolemAbilityState.fresh().withDiamondCooldown(now + 600L);

        assertEquals(s, s.clampDiamondCooldown(now, 1200L));
    }

    @Test
    void redstoneStateHelpers_roundTrip() {
        GolemAbilityState state = GolemAbilityState.fresh()
            .withRedstoneOvercharge(1200L)
            .withRedstoneCooldown(2400L)
            .withRedstoneWasBelowThreshold(true);

        assertEquals(1200L, state.redstoneOverchargeActiveUntilGameTime());
        assertEquals(2400L, state.redstoneOverchargeCooldownUntilGameTime());
        assertTrue(state.redstoneWasBelowThreshold());

        var encoded = GolemAbilityState.CODEC.encodeStart(JsonOps.INSTANCE, state).result().orElseThrow();
        GolemAbilityState decoded = GolemAbilityState.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        assertEquals(state, decoded);
    }

    @Test
    void oldDiamondOnlyJsonDefaultsRedstoneStateInactive() {
        var oldJson = JsonParser.parseString("""
            {
              "next_diamond_ability_game_time": 500,
              "next_diamond_scan_game_time": 999
            }
            """);

        GolemAbilityState decoded = GolemAbilityState.CODEC.parse(JsonOps.INSTANCE, oldJson).result().orElseThrow();
        assertEquals(500L, decoded.nextDiamondAbilityGameTime());
        assertEquals(999L, decoded.nextDiamondScanGameTime());
        assertEquals(0L, decoded.redstoneOverchargeActiveUntilGameTime());
        assertEquals(0L, decoded.redstoneOverchargeCooldownUntilGameTime());
        assertFalse(decoded.redstoneWasBelowThreshold());
    }
}
