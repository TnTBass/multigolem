package dev.charles.multigolem.internal.modstatus;

import java.util.Objects;

/**
 * Tiny public facade for consuming mods.
 */
public final class ModStatusKit {
    private static final String UNKNOWN_SERVER_VERSION = "Unknown";

    private ModStatusKit() {
    }

    public static ModStatusSnapshot disconnected() {
        return ModStatusSnapshot.disconnected();
    }

    public static ModStatusSnapshot unknown() {
        return ModStatusSnapshot.unknown();
    }

    public static ModStatusSnapshot serverNotDetected() {
        return ModStatusSnapshot.serverNotDetected();
    }

    public static ModStatusSnapshot connected(ModStatusConfig config, String serverVersion) {
        Objects.requireNonNull(config, "config");
        ModStatusVersion serverVersionInfo = ModStatusVersion.of(serverVersion);
        return connected(config, serverVersionInfo);
    }

    public static ModStatusSnapshot connected(ModStatusConfig config, String serverVersion, String serverBuild) {
        Objects.requireNonNull(config, "config");
        return connected(config, ModStatusVersion.of(serverVersion, serverBuild));
    }

    public static ModStatusSnapshot connected(ModStatusConfig config, ModStatusVersion serverVersionInfo) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(serverVersionInfo, "serverVersionInfo");
        VersionStatus status = config.clientVersionInfo().version().equals(serverVersionInfo.version())
            ? VersionStatus.MATCHED
            : VersionStatus.DIFFERENT;
        return ModStatusSnapshot.withServerVersion(serverVersionInfo, status);
    }

    public static ModStatusDisplay display(ModStatusConfig config, ModStatusSnapshot snapshot) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(snapshot, "snapshot");

        String serverVersion = snapshot.serverVersion() == null ? UNKNOWN_SERVER_VERSION : snapshot.serverVersion();
        VersionStatus status = snapshot.status();
        ModStatusMessages messages = config.messages();

        return new ModStatusDisplay(
            config.displayName(),
            config.clientVersion(),
            config.clientBuild(),
            serverVersion,
            snapshot.serverBuild(),
            messages.labelFor(status),
            messages.helpFor(status),
            status.tone(),
            config.updateUrl()
        );
    }
}
