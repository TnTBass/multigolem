package dev.charles.multigolem.customizations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsNetworkingSourceTest {
    @Test
    void serverCustomizationsNetworkingUsesOwnPayloadAndCapabilityGate() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/charles/multigolem/customizations/ServerCustomizationsNetworking.java"));
        String main = Files.readString(Path.of("src/main/java/dev/charles/multigolem/MultiGolem.java"));

        assertTrue(source.contains("PayloadTypeRegistry.clientboundPlay().register(ServerCustomizationsPayload.TYPE"));
        assertTrue(source.contains("ServerPlayConnectionEvents.JOIN"));
        assertTrue(source.contains("ServerPlayNetworking.canSend(player, ServerCustomizationsPayload.TYPE"));
        assertTrue(source.contains("ServerPlayNetworking.send(player, new ServerCustomizationsPayload"));
        assertTrue(source.contains("ServerCustomizationsSummarizer.snapshot(MultiGolem.config())"));
        assertFalse(source.contains("MultiGolemStatusPayload"));
        assertFalse(source.contains("MultiGolemStatus.PAYLOAD_PATH"));
        assertTrue(main.contains("ServerCustomizationsNetworking.registerServer();"));
    }

    @Test
    void clientCustomizationsReceiverClearsLifecycleAndStoresPayloadSnapshots() throws IOException {
        String source = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/customizations/ServerCustomizationsClient.java"));
        String client = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/MultiGolemClient.java"));

        assertTrue(source.contains("ClientPlayNetworking.registerGlobalReceiver(ServerCustomizationsPayload.TYPE"));
        assertTrue(source.contains("STATE.onServerSnapshot(payload.snapshot())"));
        assertTrue(source.contains("ClientPlayConnectionEvents.JOIN"));
        assertTrue(source.contains("ClientPlayConnectionEvents.DISCONNECT"));
        assertTrue(source.contains("ClientTickEvents.END_CLIENT_TICK"));
        assertTrue(source.contains("ClientLifecycleEvents.CLIENT_STOPPING"));
        assertTrue(source.contains("STATE.onDisconnect()"));
        assertTrue(source.contains("STATE.tick()"));
        assertTrue(client.contains("ServerCustomizationsClient.register();"));
    }
}
