package dev.charles.multigolem.identity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
        assertEquals("copper_golem", GolemFamily.COPPER_GOLEM.id());
        assertEquals(GolemFamily.COPPER_GOLEM, GolemFamily.fromId("copper_golem").orElseThrow());
    }

    @Test
    void defaultIdentityIsIronFamilyIronVariant() {
        assertEquals(new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON, Optional.empty()),
            GolemIdentity.defaultIron());
        assertTrue(GolemIdentity.defaultIron().isDefaultIron());
    }

    @Test
    void phaseTwoAcceptsOnlyIronFamilyIdentitiesWithVariants() {
        assertTrue(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND).isValidForPhase2());
        assertFalse(new GolemIdentity(null, GolemVariant.DIAMOND, Optional.empty()).isValidForPhase2());
        assertFalse(new GolemIdentity(GolemFamily.IRON_GOLEM, null, Optional.empty()).isValidForPhase2());
    }

    @Test
    void copperIronIdentityMayCarrySurfaceButOtherIronVariantsMayNot() {
        GolemSurfaceState surface = new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true);

        assertTrue(GolemIdentity.ofIronVariant(GolemVariant.COPPER, surface).isValidForPhase3());
        assertFalse(new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.GOLD, Optional.of(surface))
            .isValidForPhase3());
    }

    @Test
    void copperGolemFamilyIsReservedButNotValidForFirstSliceRuntime() {
        assertFalse(new GolemIdentity(GolemFamily.COPPER_GOLEM, GolemVariant.COPPER, Optional.empty())
            .isValidForPhase3());
    }

    @Test
    void existingHelpersKeepSurfaceEmpty() {
        assertEquals(Optional.empty(), GolemIdentity.defaultIron().surfaceState());
        assertEquals(Optional.empty(), GolemIdentity.ofIronVariant(GolemVariant.DIAMOND).surfaceState());
    }

    @Test
    void phaseTwoValidityStillMeansNoSurfaceState() {
        GolemIdentity surfaceCopper = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false));

        assertFalse(surfaceCopper.isValidForPhase2());
        assertTrue(surfaceCopper.isValidForPhase3());
    }

    @Test
    void surfaceStateDoesNotChangeIdentityVariant() {
        assertEquals(GolemVariant.COPPER, GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true)).variant());
    }

    @Test
    void streamCodecRoundTripsAbsentSurfaceAsAbsent() {
        GolemIdentity decoded = roundTrip(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND));

        assertEquals(Optional.empty(), decoded.surfaceState());
    }

    @Test
    void streamCodecRoundTripsPresentCopperSurface() {
        GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true));

        assertEquals(identity, roundTrip(identity));
    }

    @Test
    void streamCodecRoundTripsUnwaxedPresentCopperSurface() {
        GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false));

        assertEquals(identity, roundTrip(identity));
    }

    @Test
    void codecPreservesAbsentSurfaceAsAbsent() {
        JsonElement encoded = GolemIdentity.CODEC.encodeStart(JsonOps.INSTANCE,
            GolemIdentity.ofIronVariant(GolemVariant.COPPER)).getOrThrow();

        assertFalse(encoded.getAsJsonObject().has("surface_state"));
        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER),
            GolemIdentity.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    @Test
    void codecPreservesDefaultSurfaceAsPresent() {
        GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER, GolemSurfaceState.DEFAULT);
        JsonObject encoded = GolemIdentity.CODEC.encodeStart(JsonOps.INSTANCE, identity).getOrThrow().getAsJsonObject();

        assertTrue(encoded.has("surface_state"));
        assertEquals(identity, GolemIdentity.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
    }

    private static GolemIdentity roundTrip(GolemIdentity identity) {
        ByteBuf buffer = Unpooled.buffer();
        GolemIdentity.STREAM_CODEC.encode(buffer, identity);
        return GolemIdentity.STREAM_CODEC.decode(buffer);
    }
}
