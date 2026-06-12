package dev.charles.multigolem.status;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.internal.modstatus.ModStatusClientState;
import dev.charles.multigolem.internal.modstatus.ModStatusConfig;
import dev.charles.multigolem.internal.modstatus.ModStatusDisplay;
import dev.charles.multigolem.internal.modstatus.ModStatusMessages;
import dev.charles.multigolem.internal.modstatus.ModStatusServerStatus;
import dev.charles.multigolem.internal.modstatus.VersionMismatchSeverity;
import dev.charles.multigolem.internal.modstatus.VersionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public final class MultiGolemStatus {
    public static final String PAYLOAD_PATH = "mod_status/server_version";
    public static final String UPDATE_URL = "https://modrinth.com/mod/multigolem";
    private static final String BUILD_INFO_RESOURCE = "/multigolem-build.properties";
    private static final int SERVER_DETECTION_TICKS = 100;

    private static volatile StatusState state = StatusState.create(version(), build());
    private static volatile int ticksSinceJoin;
    private static volatile boolean waitingForServerStatus;

    private MultiGolemStatus() {
    }

    public static ModStatusConfig config() {
        return state.config();
    }

    public static void initializeVersion(String version, String build) {
        if (version == null || version.isBlank()) {
            throw new IllegalStateException("Missing version metadata for " + MultiGolem.MOD_ID);
        }
        state = StatusState.create(version, build);
        ticksSinceJoin = 0;
        waitingForServerStatus = false;
    }

    public static ModStatusDisplay display() {
        return state.clientState().display();
    }

    public static VersionMismatchSeverity versionMismatchSeverity() {
        return VersionMismatchSeverity.WARN;
    }

    public static void onClientJoin() {
        state.clientState().unknown();
        ticksSinceJoin = 0;
        waitingForServerStatus = true;
    }

    public static void onClientDisconnect() {
        state.clientState().disconnected();
        ticksSinceJoin = 0;
        waitingForServerStatus = false;
    }

    public static void onServerVersion(String serverVersion) {
        onServerVersion(serverVersion, null);
    }

    public static void onServerVersion(String serverVersion, String serverBuild) {
        onServerStatus(ModStatusServerStatus.of(serverVersion, serverBuild, VersionMismatchSeverity.WARN));
    }

    public static void onServerStatus(ModStatusServerStatus serverStatus) {
        state.clientState().connected(serverStatus);
        waitingForServerStatus = false;
    }

    public static void tickClientStatus() {
        if (!waitingForServerStatus) {
            return;
        }

        ticksSinceJoin++;
        if (ticksSinceJoin >= SERVER_DETECTION_TICKS) {
            state.clientState().markServerNotDetectedIfUnknown();
            waitingForServerStatus = false;
        }
    }

    private static ModStatusConfig createConfig(String version, String build) {
        return ModStatusConfig.builder()
            .modId(MultiGolem.MOD_ID)
            .displayName("MultiGolem")
            .clientVersion(version)
            .clientBuild(build)
            .updateUrl(UPDATE_URL)
            .payloadChannel(MultiGolem.MOD_ID, PAYLOAD_PATH)
            .messages(ModStatusMessages.builder()
                .label(VersionStatus.MATCHED, "Matched")
                .label(VersionStatus.DIFFERENT, "Different versions")
                .label(VersionStatus.DISCONNECTED, "Disconnected")
                .label(VersionStatus.SERVER_NOT_DETECTED, "Server not detected")
                .label(VersionStatus.UNKNOWN, "Unknown")
                .help(VersionStatus.MATCHED, "Client and server MultiGolem versions match.")
                .help(VersionStatus.DIFFERENT, "Different versions may miss or hide new features. Gameplay remains compatible.")
                .help(VersionStatus.DISCONNECTED, "Not connected to a server or world.")
                .help(VersionStatus.SERVER_NOT_DETECTED, "This server does not appear to have MultiGolem installed.")
                .help(VersionStatus.UNKNOWN, "Waiting for the server MultiGolem version.")
                .build())
            .build();
    }

    private static String version() {
        return Optional.ofNullable(System.getProperty("multigolem.version"))
            .orElse("unknown");
    }

    private static String build() {
        return Optional.ofNullable(System.getProperty("multigolem.build"))
            .or(MultiGolemStatus::buildFromResource)
            .orElse(null);
    }

    private static Optional<String> buildFromResource() {
        try (InputStream input = MultiGolemStatus.class.getResourceAsStream(BUILD_INFO_RESOURCE)) {
            if (input == null) {
                return Optional.empty();
            }
            Properties properties = new Properties();
            properties.load(input);
            return Optional.ofNullable(properties.getProperty("build"));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private record StatusState(ModStatusConfig config, ModStatusClientState clientState) {
        static StatusState create(String version, String build) {
            ModStatusConfig config = createConfig(version, build);
            return new StatusState(config, ModStatusClientState.create(config));
        }
    }
}
