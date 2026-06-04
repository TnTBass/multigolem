package dev.charles.multigolem.internal.modstatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModStatusVersionTest {
    @Test
    void optionalBuildMetadataDoesNotChangeBaseVersionComparison() {
        ModStatusVersion version = ModStatusVersion.of("0.4.0+mc26.1.2", "abc1234");
        ModStatusVersion inlineBuild = ModStatusVersion.of("0.4.0+def5678");

        assertEquals("0.4.0", version.version());
        assertEquals("abc1234", version.build());
        assertEquals("0.4.0+abc1234", version.toPayloadString());
        assertEquals("0.4.0", inlineBuild.version());
        assertEquals("def5678", inlineBuild.build());
        assertEquals("0.4.0+def5678", inlineBuild.toPayloadString());
    }

    @Test
    void explicitBuildOverridesInlineBuild() {
        ModStatusVersion version = ModStatusVersion.of("0.4.0+mc26.1.2+inline", "gradle");

        assertEquals("0.4.0", version.version());
        assertEquals("gradle", version.build());
        assertEquals("0.4.0+gradle", version.toPayloadString());
    }

    @Test
    void plainVersionHasNoBuildMetadata() {
        ModStatusVersion version = ModStatusVersion.of("0.4.0");

        assertEquals("0.4.0", version.version());
        assertNull(version.build());
        assertEquals("0.4.0", version.toPayloadString());
    }

    @Test
    void payloadStringRoundTripsVersionAndBuild() {
        ModStatusVersion version = ModStatusVersion.of("0.4.0", "abc1234");
        ModStatusVersion roundTripped = ModStatusVersion.of(version.toPayloadString());

        assertEquals(version.version(), roundTripped.version());
        assertEquals(version.build(), roundTripped.build());
        assertEquals(version.toPayloadString(), roundTripped.toPayloadString());
    }
}
