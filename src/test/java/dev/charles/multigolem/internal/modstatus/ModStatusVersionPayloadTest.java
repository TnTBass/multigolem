package dev.charles.multigolem.internal.modstatus;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ModStatusVersionPayloadTest {
    @Test
    void structuredStatusPayloadCarriesVersionBuildAndSeverity() {
        byte[] payload = ModStatusVersionPayload.encodeServerStatus(
            "0.5.1",
            "server123",
            VersionMismatchSeverity.WARN
        );

        ModStatusServerStatus status = ModStatusVersionPayload.decodeServerStatus(payload);

        assertEquals("0.5.1", status.serverVersion());
        assertEquals("server123", status.serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, status.versionMismatchSeverity());
        assertEquals("0.5.1+server123", ModStatusVersionPayload.decodeServerVersion(payload));
        assertEquals("server123", ModStatusVersionPayload.decodeServerVersionInfo(payload).build());
    }

    @Test
    void structuredStatusPayloadAllowsOmittedBuild() {
        byte[] payload = ModStatusVersionPayload.encodeServerStatus(
            "0.5.1",
            null,
            VersionMismatchSeverity.BREAKING
        );

        ModStatusServerStatus status = ModStatusVersionPayload.decodeServerStatus(payload);

        assertEquals("0.5.1", status.serverVersion());
        assertNull(status.serverBuild());
        assertEquals(VersionMismatchSeverity.BREAKING, status.versionMismatchSeverity());
        assertEquals("0.5.1", ModStatusVersionPayload.decodeServerVersion(payload));
    }

    @Test
    void structuredStatusPayloadTreatsBlankBuildAsAbsent() {
        byte[] payload = "MSK2\nversion=0.5.1\nbuild=\nversionMismatchSeverity=WARN\n".getBytes(StandardCharsets.UTF_8);

        assertNull(ModStatusVersionPayload.decodeServerStatus(payload).serverBuild());
    }

    @Test
    void legacyPayloadsDecodeAsWarnSeverity() {
        ModStatusServerStatus status = ModStatusVersionPayload.decodeServerStatus(
            ModStatusVersionPayload.encodeServerVersion("0.5.1+legacy123")
        );

        assertEquals("0.5.1", status.serverVersion());
        assertEquals("legacy123", status.serverBuild());
        assertEquals(VersionMismatchSeverity.WARN, status.versionMismatchSeverity());
    }

    @Test
    void unknownStructuredSeverityDefaultsToWarn() {
        byte[] payload = "MSK2\nversion=0.5.1\nversionMismatchSeverity=LOUD\n".getBytes(StandardCharsets.UTF_8);

        assertEquals(
            VersionMismatchSeverity.WARN,
            ModStatusVersionPayload.decodeServerStatus(payload).versionMismatchSeverity()
        );
    }

    @Test
    void structuredPayloadRejectsLineBreaksInFields() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ModStatusVersionPayload.encodeServerStatus("0.5.1\nbad", null, VersionMismatchSeverity.WARN)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> ModStatusVersionPayload.encodeServerStatus("0.5.1", "bad\nbuild", VersionMismatchSeverity.WARN)
        );
    }
}
