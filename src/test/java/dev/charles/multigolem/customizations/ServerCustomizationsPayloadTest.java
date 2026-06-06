package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.status.MultiGolemStatus;
import dev.charles.multigolem.test.MinecraftBootstrap;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsPayloadTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void payloadUsesSeparateMultiGolemChannelFromStatus() {
        assertEquals(Identifier.fromNamespaceAndPath("multigolem", "server_customizations"), ServerCustomizationsPayload.ID);
        assertEquals("server_customizations", ServerCustomizationsPayload.PAYLOAD_PATH);
        assertNotEquals(MultiGolemStatus.PAYLOAD_PATH, ServerCustomizationsPayload.PAYLOAD_PATH);
    }

    @Test
    void payloadRoundTripsSemanticSnapshot() {
        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        weights.put(GolemVariant.COPPER, 24);
        weights.put(GolemVariant.GOLD, 7);
        ServerCustomizationsSnapshot snapshot = new ServerCustomizationsSnapshot(
            false,
            true,
            weights,
            false,
            "permission checks use the server's configured permission provider when present",
            List.of(new VariantCustomizationSummary(GolemVariant.GOLD, List.of("Gold speed multiplier differs from defaults")))
        );
        ServerCustomizationsPayload payload = new ServerCustomizationsPayload(snapshot);
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        ServerCustomizationsPayload.CODEC.encode(buf, payload);
        ServerCustomizationsPayload roundTripped = ServerCustomizationsPayload.CODEC.decode(buf);

        assertEquals(payload, roundTripped);
        assertEquals(snapshot, roundTripped.snapshot());
        assertEquals(ServerCustomizationsPayload.TYPE, roundTripped.type());
    }

    @Test
    void snapshotCopiesMutableCollections() {
        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        weights.put(GolemVariant.COPPER, 24);
        ServerCustomizationsSnapshot snapshot = new ServerCustomizationsSnapshot(
            true,
            true,
            weights,
            true,
            "permissions unavailable",
            List.of()
        );

        weights.put(GolemVariant.COPPER, 99);

        assertEquals(24, snapshot.villageSpawnWeights().get(GolemVariant.COPPER));
        assertThrows(UnsupportedOperationException.class, () ->
            snapshot.villageSpawnWeights().put(GolemVariant.GOLD, 1)
        );
    }
}
