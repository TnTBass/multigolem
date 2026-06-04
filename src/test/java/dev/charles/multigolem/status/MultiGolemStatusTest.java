package dev.charles.multigolem.status;

import dev.charles.multigolem.internal.modstatus.ModStatusClientState;
import dev.charles.multigolem.internal.modstatus.ModStatusConfig;
import dev.charles.multigolem.internal.modstatus.ModStatusDisplay;
import dev.charles.multigolem.internal.modstatus.ModStatusVersion;
import dev.charles.multigolem.internal.modstatus.ModStatusVersionPayload;
import dev.charles.multigolem.internal.modstatus.StatusTone;
import dev.charles.multigolem.internal.modstatus.VersionStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemStatusTest {
    @Test
    void configUsesMultiGolemIdentityAndDedicatedPayloadChannel() {
        ModStatusConfig config = MultiGolemStatus.config();

        assertEquals("multigolem", config.modId());
        assertEquals("MultiGolem", config.displayName());
        // ModStatusVersion keeps the display version before any +build metadata.
        assertEquals(requiredProperty("multigolem.version").split("\\+")[0], config.clientVersion());
        assertEquals(requiredProperty("multigolem.build"), config.clientBuild());
        assertEquals("https://modrinth.com/mod/multigolem", config.updateUrl());
        assertEquals("multigolem", config.payloadNamespace());
        assertEquals("mod_status/server_version", config.payloadPath());
        assertEquals("multigolem:mod_status/server_version", config.payloadChannel());
    }

    @Test
    void displayCopyIsPassiveAndPlayerFacing() {
        ModStatusClientState state = ModStatusClientState.create(MultiGolemStatus.config());
        state.connected("0.3.0+mc26.1.2");

        ModStatusDisplay display = state.display();

        assertEquals(VersionStatus.DIFFERENT, state.snapshot().status());
        assertEquals(StatusTone.ORANGE, display.tone());
        assertEquals("Different versions", display.statusLabel());
        assertEquals(
            "Different versions may miss or hide new features. Gameplay remains compatible.",
            display.helpText()
        );
    }

    @Test
    void unknownJoinCanTimeoutToServerNotDetectedWithoutDisconnecting() {
        ModStatusClientState state = ModStatusClientState.create(MultiGolemStatus.config());

        state.unknown();

        assertEquals(VersionStatus.UNKNOWN, state.snapshot().status());
        assertTrue(state.markServerNotDetectedIfUnknown());
        assertEquals(VersionStatus.SERVER_NOT_DETECTED, state.snapshot().status());
        assertFalse(state.markServerNotDetectedIfUnknown());
    }

    @Test
    void sendServerVersionIsCapabilityGated() {
        ModStatusConfig config = MultiGolemStatus.config();
        AtomicReference<String> channel = new AtomicReference<>();
        AtomicReference<byte[]> payload = new AtomicReference<>();

        assertFalse(ModStatusVersionPayload.sendServerVersionIfSupported(
            config,
            ignored -> false,
            (sentChannel, sentPayload) -> {
                channel.set(sentChannel);
                payload.set(sentPayload);
            }
        ));
        assertNull(channel.get());
        assertNull(payload.get());

        assertTrue(ModStatusVersionPayload.sendServerVersionIfSupported(
            config,
            config.payloadChannel()::equals,
            (sentChannel, sentPayload) -> {
                channel.set(sentChannel);
                payload.set(sentPayload);
            }
        ));
        assertEquals(config.payloadChannel(), channel.get());
        ModStatusVersion sentVersion = ModStatusVersionPayload.decodeServerVersionInfo(payload.get());
        assertEquals(config.clientVersion(), sentVersion.version());
        assertEquals(config.clientBuild(), sentVersion.build());
        assertEquals(config.clientVersionInfo().toPayloadString(), ModStatusVersionPayload.decodeServerVersion(payload.get()));
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, name + " must be supplied by Gradle test task");
        assertFalse(value.isBlank(), name + " must not be blank");
        return value;
    }
}
