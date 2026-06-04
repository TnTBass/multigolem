package dev.charles.multigolem.status;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemStatusIntegrationSourceTest {
    @Test
    void serverJoinSendIsCapabilityGatedAndSeparateFromGameplayPackets() throws IOException {
        String source = Files.readString(Path.of("src/main/java/dev/charles/multigolem/status/MultiGolemStatusNetworking.java"));
        String main = Files.readString(Path.of("src/main/java/dev/charles/multigolem/MultiGolem.java"));

        assertTrue(source.contains("ServerPlayConnectionEvents.JOIN"), "status must send from the server join lifecycle");
        assertTrue(source.contains("ServerPlayNetworking.canSend(player, MultiGolemStatusPayload.TYPE)"), "status send must be capability gated");
        assertTrue(source.contains("ServerPlayNetworking.send(player, MultiGolemStatusPayload.fromServerVersion"), "status payload must be sent independently");
        assertTrue(main.contains("MultiGolemStatusNetworking.registerServer();"), "main initializer must register status networking");
        assertFalse(source.contains("GolemVariant"), "status networking must not depend on gameplay variant packets/state");
    }

    @Test
    void clientJoinDisconnectAndTickTimeoutUpdateStatusState() throws IOException {
        String source = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/status/MultiGolemStatusClient.java"));
        String client = Files.readString(Path.of("src/client/java/dev/charles/multigolem/client/MultiGolemClient.java"));
        String status = Files.readString(Path.of("src/main/java/dev/charles/multigolem/status/MultiGolemStatus.java"));

        assertTrue(source.contains("ClientPlayConnectionEvents.JOIN"), "client join should enter unknown status");
        assertTrue(source.contains("ClientPlayConnectionEvents.DISCONNECT"), "client disconnect should enter disconnected status");
        assertTrue(source.contains("ClientTickEvents.END_CLIENT_TICK"), "client tick should resolve stale unknown status");
        assertTrue(source.contains("MultiGolemStatus.tickClientStatus()"), "client tick handler must call status timeout logic");
        assertTrue(status.contains("markServerNotDetectedIfUnknown"), "timeout should become passive server-not-detected");
        assertTrue(status.contains("private static volatile int ticksSinceJoin"), "join timeout counter must be visible across networking and client tick callbacks");
        assertTrue(status.contains("private static volatile boolean waitingForServerStatus"), "server-status wait flag must be visible across networking and client tick callbacks");
        assertTrue(source.contains("ClientPlayNetworking.registerGlobalReceiver"), "client must receive the status payload");
        assertTrue(client.contains("MultiGolemStatusClient.register();"), "client initializer must register status client");
    }
}
