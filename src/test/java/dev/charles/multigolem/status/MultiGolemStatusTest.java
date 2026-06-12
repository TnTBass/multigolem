package dev.charles.multigolem.status;

import dev.charles.multigolem.internal.modstatus.ModStatusClientState;
import dev.charles.multigolem.internal.modstatus.ModStatusConfig;
import dev.charles.multigolem.internal.modstatus.ModStatusDisplay;
import dev.charles.multigolem.internal.modstatus.ModStatusServerStatus;
import dev.charles.multigolem.internal.modstatus.ModStatusVersion;
import dev.charles.multigolem.internal.modstatus.ModStatusVersionPayload;
import dev.charles.multigolem.internal.modstatus.StatusTone;
import dev.charles.multigolem.internal.modstatus.VersionMismatchSeverity;
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
    void initializeVersionUpdatesCommonStatusConfig() {
        String originalVersion = MultiGolemStatus.config().clientVersion();
        String originalBuild = MultiGolemStatus.config().clientBuild();

        try {
            MultiGolemStatus.initializeVersion("9.9.9-test", "boundary-build");

            assertEquals("9.9.9-test", MultiGolemStatus.config().clientVersion());
            assertEquals("boundary-build", MultiGolemStatus.config().clientBuild());
        } finally {
            MultiGolemStatus.initializeVersion(originalVersion, originalBuild);
        }
    }

    @Test
    void displayCopyIsPassiveAndPlayerFacing() {
        ModStatusClientState state = ModStatusClientState.create(MultiGolemStatus.config());
        state.connected(ModStatusServerStatus.of("0.3.0", "mc26.1.2", VersionMismatchSeverity.WARN));

        ModStatusDisplay display = state.display();

        assertEquals(VersionStatus.DIFFERENT, state.snapshot().status());
        assertEquals(VersionMismatchSeverity.WARN, state.snapshot().versionMismatchSeverity());
        assertEquals(StatusTone.ORANGE, display.tone());
        assertEquals("Different versions", display.statusLabel());
        assertEquals(
            "Different versions may miss or hide new features. Gameplay remains compatible.",
            display.helpText()
        );
    }

    @Test
    void breakingSeverityCanRenderRedButMultiGolemConfigUsesWarn() {
        ModStatusClientState state = ModStatusClientState.create(MultiGolemStatus.config());

        state.connected(ModStatusServerStatus.of("0.3.0", null, VersionMismatchSeverity.BREAKING));

        assertEquals(VersionStatus.DIFFERENT, state.snapshot().status());
        assertEquals(VersionMismatchSeverity.BREAKING, state.snapshot().versionMismatchSeverity());
        assertEquals(StatusTone.RED, state.display().tone());
        assertEquals(VersionMismatchSeverity.WARN, MultiGolemStatus.versionMismatchSeverity());
    }

    @Test
    void matchingPublicVersionWithDifferentBuildRendersTeal() {
        ModStatusClientState state = ModStatusClientState.create(MultiGolemStatus.config());
        ModStatusConfig config = state.config();

        state.connected(ModStatusServerStatus.of(
            config.clientVersion(),
            "different-build",
            VersionMismatchSeverity.BREAKING
        ));

        assertEquals(VersionStatus.MATCHED, state.snapshot().status());
        assertEquals(StatusTone.TEAL, state.display().tone());
    }

    @Test
    void matchingPublicVersionWithMissingOrDevBuildRendersGreen() {
        ModStatusClientState state = ModStatusClientState.create(MultiGolemStatus.config());
        ModStatusConfig config = state.config();

        state.connected(ModStatusServerStatus.of(config.clientVersion(), null, VersionMismatchSeverity.BREAKING));

        assertEquals(VersionStatus.MATCHED, state.snapshot().status());
        assertEquals(StatusTone.GREEN, state.display().tone());

        ModStatusConfig devConfig = ModStatusConfig.builder()
            .modId("multigolem")
            .displayName("MultiGolem")
            .clientVersion(config.clientVersion())
            .clientBuild("dev")
            .payloadChannel(config.payloadNamespace(), config.payloadPath())
            .build();
        ModStatusClientState devState = ModStatusClientState.create(devConfig);

        devState.connected(ModStatusServerStatus.of(config.clientVersion(), "server-build", VersionMismatchSeverity.WARN));

        assertEquals(VersionStatus.MATCHED, devState.snapshot().status());
        assertEquals(StatusTone.GREEN, devState.display().tone());
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

        assertFalse(ModStatusVersionPayload.sendServerStatusIfSupported(
            config,
            MultiGolemStatus.versionMismatchSeverity(),
            ignored -> false,
            (sentChannel, sentPayload) -> {
                channel.set(sentChannel);
                payload.set(sentPayload);
            }
        ));
        assertNull(channel.get());
        assertNull(payload.get());

        assertTrue(ModStatusVersionPayload.sendServerStatusIfSupported(
            config,
            MultiGolemStatus.versionMismatchSeverity(),
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
        assertEquals(VersionMismatchSeverity.WARN, ModStatusVersionPayload.decodeServerStatus(payload.get()).versionMismatchSeverity());
        assertEquals(config.clientVersionInfo().toPayloadString(), ModStatusVersionPayload.decodeServerVersion(payload.get()));
    }

    private static String requiredProperty(String name) {
        String value = System.getProperty(name);
        assertNotNull(value, name + " must be supplied by Gradle test task");
        assertFalse(value.isBlank(), name + " must not be blank");
        return value;
    }
}
