package dev.charles.multigolem.status;

import dev.charles.multigolem.internal.modstatus.ModStatusServerStatus;
import dev.charles.multigolem.internal.modstatus.ModStatusVersionPayload;
import dev.charles.multigolem.internal.modstatus.VersionMismatchSeverity;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemStatusPayloadTest {
    @Test
    void payloadIdUsesDedicatedStatusChannel() {
        assertEquals(Identifier.fromNamespaceAndPath("multigolem", "mod_status/server_version"), MultiGolemStatusPayload.ID);
        assertEquals("multigolem:mod_status/server_version", MultiGolemStatusPayload.ID.toString());
    }

    @Test
    void payloadCarriesOnlyEncodedServerVersion() {
        MultiGolemStatusPayload payload = MultiGolemStatusPayload.fromServerVersion("0.4.0+mc26.1.2", "abc1234");
        MultiGolemStatusPayload matchingPayload = MultiGolemStatusPayload.fromServerVersion("0.4.0+mc26.1.2", "abc1234");

        assertEquals("0.4.0", payload.serverVersion());
        assertEquals("abc1234", payload.serverBuild());
        assertEquals("0.4.0+abc1234", ModStatusVersionPayload.decodeServerVersion(payload.encodedVersion()));
        assertEquals("abc1234", ModStatusVersionPayload.decodeServerVersionInfo(payload.encodedVersion()).build());
        assertEquals(payload, matchingPayload);
        assertEquals(payload.hashCode(), matchingPayload.hashCode());
        assertEquals(MultiGolemStatusPayload.TYPE, payload.type());
    }

    @Test
    void payloadCarriesVersionWithoutBuildWhenAbsent() {
        MultiGolemStatusPayload payload = MultiGolemStatusPayload.fromServerVersion("0.4.0");

        assertEquals("0.4.0", payload.serverVersion());
        assertNull(payload.serverBuild());
        assertEquals("0.4.0", ModStatusVersionPayload.decodeServerVersion(payload.encodedVersion()));
        assertNull(ModStatusVersionPayload.decodeServerVersionInfo(payload.encodedVersion()).build());
    }

    @Test
    void payloadCarriesStructuredWarnServerStatus() {
        MultiGolemStatusPayload payload = MultiGolemStatusPayload.fromServerStatus(
            "0.5.1",
            "server456",
            VersionMismatchSeverity.WARN
        );

        ModStatusServerStatus status = payload.serverStatus();

        assertEquals("0.5.1", payload.serverVersion());
        assertEquals("server456", payload.serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, status.versionMismatchSeverity());
        assertEquals("0.5.1+server456", ModStatusVersionPayload.decodeServerVersion(payload.encodedVersion()));
    }

    @Test
    void payloadRejectsBlankVersions() {
        assertThrows(IllegalArgumentException.class, () -> MultiGolemStatusPayload.fromServerVersion(null));
        assertThrows(IllegalArgumentException.class, () -> MultiGolemStatusPayload.fromServerVersion(" "));
        assertThrows(IllegalArgumentException.class, () -> new MultiGolemStatusPayload(new byte[0]));
    }
}
