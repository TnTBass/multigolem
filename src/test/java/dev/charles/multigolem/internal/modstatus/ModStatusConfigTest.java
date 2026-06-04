package dev.charles.multigolem.internal.modstatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModStatusConfigTest {
    @Test
    void builderRequiresOwningModIdentityVersionAndChannel() {
        ModStatusConfig config = ModStatusConfig.builder()
            .modId("multigolem")
            .displayName("MultiGolem")
            .clientVersion("0.4.0+mc26.1.2")
            .updateUrl("https://modrinth.com/mod/multigolem")
            .payloadChannel("multigolem", "mod_status/server_version")
            .build();

        assertEquals("multigolem", config.modId());
        assertEquals("MultiGolem", config.displayName());
        assertEquals("0.4.0", config.clientVersion());
        assertEquals("mc26.1.2", config.clientBuild());
        assertEquals("https://modrinth.com/mod/multigolem", config.updateUrl());
        assertEquals("multigolem:mod_status/server_version", config.payloadChannel());
    }

    @Test
    void builderRejectsBlankRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().modId(" ").build());
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().displayName(" ").build());
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().clientVersion(" ").build());
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().payloadChannel("multigolem", " ").build());
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().payloadChannel("multi:golem", "mod_status/server_version").build());
        assertThrows(IllegalArgumentException.class, () -> baseBuilder().payloadChannel("multigolem", "mod_status:server_version").build());
    }

    @Test
    void blankUpdateUrlIsTreatedAsAbsent() {
        assertNull(baseBuilder().updateUrl(" ").build().updateUrl());
    }

    @Test
    void plainClientVersionHasNoBuildMetadata() {
        ModStatusConfig config = baseBuilder().clientVersion("0.4.0").build();

        assertEquals("0.4.0", config.clientVersion());
        assertNull(config.clientBuild());
        assertEquals("0.4.0", config.clientVersionInfo().toPayloadString());
    }

    private static ModStatusConfig.Builder baseBuilder() {
        return ModStatusConfig.builder()
            .modId("multigolem")
            .displayName("MultiGolem")
            .clientVersion("0.4.0+mc26.1.2")
            .payloadChannel("multigolem", "mod_status/server_version");
    }
}
