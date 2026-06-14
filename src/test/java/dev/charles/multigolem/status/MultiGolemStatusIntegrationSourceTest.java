package dev.charles.multigolem.status;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemStatusIntegrationSourceTest {
    @Test
    void serverJoinSendIsCapabilityGatedAndSeparateFromGameplayPackets() throws IOException {
        String source = readSource("src/fabric/java/dev/charles/multigolem/fabric/status/FabricMultiGolemStatusNetworking.java");
        String fabricMain = readSource("src/fabric/java/dev/charles/multigolem/fabric/MultiGolemFabric.java");

        assertTrue(source.contains("ServerPlayConnectionEvents.JOIN"), "status must send from the server join lifecycle");
        assertTrue(source.contains("ServerPlayNetworking.canSend(player, MultiGolemStatusPayload.TYPE)"), "status send must be capability gated");
        assertTrue(source.contains("ServerPlayNetworking.send(player, MultiGolemStatusPayload.fromServerStatus"), "status payload must be sent independently");
        assertTrue(source.contains("VersionMismatchSeverity.WARN"), "MultiGolem should declare passive WARN mismatch severity");
        assertTrue(fabricMain.contains("FabricMultiGolemStatusNetworking.registerServer();"), "Fabric initializer must register status networking");
        assertFalse(source.contains("GolemVariant"), "status networking must not depend on gameplay variant packets/state");
    }

    @Test
    void clientJoinDisconnectAndTickTimeoutUpdateStatusState() throws IOException {
        String source = readSource("src/fabricClient/java/dev/charles/multigolem/fabric/client/status/FabricMultiGolemStatusClient.java");
        String client = readSource("src/fabricClient/java/dev/charles/multigolem/fabric/client/MultiGolemFabricClient.java");
        String status = readSource("src/common/java/dev/charles/multigolem/status/MultiGolemStatus.java");

        assertTrue(source.contains("ClientPlayConnectionEvents.JOIN"), "client join should enter unknown status");
        assertTrue(source.contains("ClientPlayConnectionEvents.DISCONNECT"), "client disconnect should enter disconnected status");
        assertTrue(source.contains("ClientTickEvents.END_CLIENT_TICK"), "client tick should resolve stale unknown status");
        assertTrue(source.contains("MultiGolemStatus.tickClientStatus()"), "client tick handler must call status timeout logic");
        assertTrue(status.contains("markServerNotDetectedIfUnknown"), "timeout should become passive server-not-detected");
        assertTrue(status.contains("private static volatile int ticksSinceJoin"), "join timeout counter must be visible across networking and client tick callbacks");
        assertTrue(status.contains("private static volatile boolean waitingForServerStatus"), "server-status wait flag must be visible across networking and client tick callbacks");
        assertTrue(source.contains("ClientPlayNetworking.registerGlobalReceiver"), "client must receive the status payload");
        assertTrue(source.contains("MultiGolemStatus.onServerStatus(payload.serverStatus())"), "client should preserve structured server status severity");
        assertTrue(client.contains("FabricMultiGolemStatusClient.register();"), "client initializer must register status client");
    }

    @Test
    void statusPayloadStaysVersionStatusOnlyAndDoesNotCarryServerCustomizations() throws IOException {
        String statusPayload = readSource("src/common/java/dev/charles/multigolem/status/MultiGolemStatusPayload.java");
        String status = readSource("src/common/java/dev/charles/multigolem/status/MultiGolemStatus.java");
        String statusNetworking = readSource("src/fabric/java/dev/charles/multigolem/fabric/status/FabricMultiGolemStatusNetworking.java");

        assertTrue(statusPayload.contains("encodedVersion"));
        assertTrue(statusPayload.contains("ModStatusVersionPayload"));
        assertFalse(statusPayload.contains("healingEnabled"));
        assertFalse(statusPayload.contains("villageSpawnWeights"));
        assertFalse(statusPayload.contains("variantOverrides"));
        assertFalse(status.contains("server_customizations"));
        assertFalse(statusNetworking.contains("ServerCustomizationsPayload"));
    }

    @Test
    void statusNetworkingRegistrationCanBeResetForIsolatedTests() throws IOException {
        String source = readSource("src/fabric/java/dev/charles/multigolem/fabric/status/FabricMultiGolemStatusNetworking.java");

        assertTrue(source.contains("static void resetForTesting()"), "tests need a package-private registration reset hook");
        assertTrue(source.contains("registered = false;"), "reset hook must clear the registration guard");
    }

    private static String readSource(String path) throws IOException {
        Path source = Path.of(path);
        assertTrue(Files.exists(source), "Expected source file missing: " + source);
        return Files.readString(source);
    }
}
