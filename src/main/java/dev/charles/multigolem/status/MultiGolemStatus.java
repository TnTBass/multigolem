package dev.charles.multigolem.status;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.internal.modstatus.ModStatusClientState;
import dev.charles.multigolem.internal.modstatus.ModStatusConfig;
import dev.charles.multigolem.internal.modstatus.ModStatusDisplay;
import dev.charles.multigolem.internal.modstatus.ModStatusMessages;
import dev.charles.multigolem.internal.modstatus.VersionStatus;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public final class MultiGolemStatus {
    public static final String PAYLOAD_PATH = "mod_status/server_version";
    public static final String UPDATE_URL = "https://modrinth.com/mod/multigolem";
    private static final String BUILD_INFO_RESOURCE = "/multigolem-build.properties";
    private static final int SERVER_DETECTION_TICKS = 100;

    private static final ModStatusConfig CONFIG = ModStatusConfig.builder()
        .modId(MultiGolem.MOD_ID)
        .displayName("MultiGolem")
        .clientVersion(version())
        .clientBuild(build())
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
    private static final ModStatusClientState CLIENT_STATE = ModStatusClientState.create(CONFIG);
    private static volatile int ticksSinceJoin;
    private static volatile boolean waitingForServerStatus;

    private MultiGolemStatus() {
    }

    public static ModStatusConfig config() {
        return CONFIG;
    }

    public static ModStatusDisplay display() {
        return CLIENT_STATE.display();
    }

    public static void onClientJoin() {
        CLIENT_STATE.unknown();
        ticksSinceJoin = 0;
        waitingForServerStatus = true;
    }

    public static void onClientDisconnect() {
        CLIENT_STATE.disconnected();
        ticksSinceJoin = 0;
        waitingForServerStatus = false;
    }

    public static void onServerVersion(String serverVersion) {
        onServerVersion(serverVersion, null);
    }

    public static void onServerVersion(String serverVersion, String serverBuild) {
        CLIENT_STATE.connected(serverVersion, serverBuild);
        waitingForServerStatus = false;
    }

    public static void tickClientStatus() {
        if (!waitingForServerStatus) {
            return;
        }

        ticksSinceJoin++;
        if (ticksSinceJoin >= SERVER_DETECTION_TICKS) {
            CLIENT_STATE.markServerNotDetectedIfUnknown();
            waitingForServerStatus = false;
        }
    }

    private static String version() {
        return versionFromFabric()
            .or(() -> Optional.ofNullable(System.getProperty("multigolem.version")))
            .orElseThrow(() -> new IllegalStateException("Missing version metadata for " + MultiGolem.MOD_ID));
    }

    private static Optional<String> versionFromFabric() {
        try {
            return FabricLoader.getInstance()
                .getModContainer(MultiGolem.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
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
}
