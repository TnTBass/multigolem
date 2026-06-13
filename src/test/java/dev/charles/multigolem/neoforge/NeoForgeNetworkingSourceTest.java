package dev.charles.multigolem.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeNetworkingSourceTest {
    @Test
    void neoforgeNetworkingRegistersOptionalClientboundPlayPayloads() throws IOException {
        String source = readSource(
            "src/neoforge/java/dev/charles/multigolem/neoforge/network/NeoForgeMultiGolemNetworking.java");
        String main = readSource("src/neoforge/java/dev/charles/multigolem/neoforge/MultiGolemNeoForge.java");

        assertTrue(source.contains("RegisterPayloadHandlersEvent"));
        assertTrue(source.contains("event.registrar(MultiGolem.MOD_ID).optional()"));
        assertTrue(source.contains("playToClient(MultiGolemStatusPayload.TYPE, MultiGolemStatusPayload.CODEC)"));
        assertTrue(source.contains("playToClient(ServerCustomizationsPayload.TYPE, ServerCustomizationsPayload.CODEC)"));
        assertTrue(main.contains("NeoForgeMultiGolemNetworking.register(modBus);"));
        assertFalse(source.contains("net.fabricmc"));
    }

    @Test
    void neoforgeNetworkingCapabilityGatesJoinSends() throws IOException {
        String source = readSource(
            "src/neoforge/java/dev/charles/multigolem/neoforge/network/NeoForgeMultiGolemNetworking.java");

        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemNetworking::onPlayerLoggedIn);"));
        assertTrue(source.contains("event.getEntity() instanceof ServerPlayer player"));
        assertTrue(source.contains("player.connection.hasChannel(MultiGolemStatusPayload.TYPE)"));
        assertTrue(source.contains("player.connection.hasChannel(ServerCustomizationsPayload.TYPE)"));
        assertTrue(source.contains("PacketDistributor.sendToPlayer(player, MultiGolemStatusPayload.fromServerStatus"));
        assertTrue(source.contains("PacketDistributor.sendToPlayer(player, new ServerCustomizationsPayload"));
        assertTrue(source.contains("VersionMismatchSeverity.WARN"));
        assertTrue(source.contains("ServerCustomizationsSummarizer.snapshot(MultiGolem.config())"));
    }

    @Test
    void neoforgeClientRegistersPayloadReceiversAndLifecycleHooks() throws IOException {
        String source = readSource(
            "src/neoforgeClient/java/dev/charles/multigolem/neoforge/client/network/NeoForgeMultiGolemClientNetworking.java");
        String client = readSource(
            "src/neoforgeClient/java/dev/charles/multigolem/neoforge/client/MultiGolemNeoForgeClient.java");

        assertTrue(source.contains("RegisterClientPayloadHandlersEvent"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientJoin);"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientDisconnect);"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientTick);"));
        assertTrue(source.contains("NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientStopping);"));
        assertTrue(source.contains("event.register(MultiGolemStatusPayload.TYPE"));
        assertTrue(source.contains("event.register(ServerCustomizationsPayload.TYPE"));
        assertTrue(source.contains("context.enqueueWork(() -> MultiGolemStatus.onServerStatus(payload.serverStatus()))"));
        assertTrue(source.contains("context.enqueueWork(() -> ServerCustomizationsClient.state().onServerSnapshot(payload.snapshot()))"));
        assertTrue(source.contains("MultiGolemStatus.onServerStatus(payload.serverStatus())"));
        assertTrue(source.contains("ServerCustomizationsClient.state().onServerSnapshot(payload.snapshot())"));
        assertTrue(source.contains("MultiGolemStatus.onClientJoin()"));
        assertTrue(source.contains("MultiGolemStatus.onClientDisconnect()"));
        assertTrue(source.contains("MultiGolemStatus.tickClientStatus()"));
        assertTrue(source.contains("ServerCustomizationsClient.state().onJoin()"));
        assertTrue(source.contains("ServerCustomizationsClient.state().onDisconnect()"));
        assertTrue(source.contains("ServerCustomizationsClient.state().tick()"));
        assertTrue(client.contains("NeoForgeMultiGolemClientNetworking.register(modBus);"));
        assertFalse(source.contains("net.fabricmc"));
    }

    @Test
    void neoforgeClientRegistersRenderStateMixins() throws IOException {
        String metadata = readSource("src/neoforge/resources/META-INF/neoforge.mods.toml");
        String clientMixins = readSource("src/neoforgeClient/resources/multigolem.neoforge.client.mixins.json");

        assertTrue(metadata.contains("mixins=[\"multigolem.neoforge.mixins.json\", \"multigolem.neoforge.client.mixins.json\"]"));
        assertTrue(clientMixins.contains("\"package\": \"dev.charles.multigolem.client.mixin\""));
        assertTrue(clientMixins.contains("\"environment\": \"client\""));
        assertTrue(clientMixins.contains("\"IronGolemRenderStateExtensionMixin\""));
        assertTrue(clientMixins.contains("\"IronGolemRenderStateMixin\""));
        assertTrue(clientMixins.contains("\"IronGolemRendererMixin\""));
    }

    private static String readSource(String path) throws IOException {
        Path source = Path.of(path);
        assertTrue(Files.exists(source), "Expected source file missing: " + source);
        return Files.readString(source);
    }
}
