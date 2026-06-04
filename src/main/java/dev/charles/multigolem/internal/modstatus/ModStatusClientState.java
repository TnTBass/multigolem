package dev.charles.multigolem.internal.modstatus;

import java.util.Objects;

/**
 * Reusable client-side status state for consuming mods to call from their own
 * lifecycle and networking callbacks.
 */
public final class ModStatusClientState {
    private final ModStatusConfig config;
    private volatile ModStatusSnapshot snapshot;

    private ModStatusClientState(ModStatusConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.snapshot = ModStatusKit.disconnected();
    }

    public static ModStatusClientState create(ModStatusConfig config) {
        return new ModStatusClientState(config);
    }

    public ModStatusConfig config() {
        return config;
    }

    public ModStatusSnapshot snapshot() {
        return snapshot;
    }

    public ModStatusDisplay display() {
        return ModStatusKit.display(config, snapshot);
    }

    public synchronized void disconnected() {
        snapshot = ModStatusKit.disconnected();
    }

    public synchronized void unknown() {
        snapshot = ModStatusKit.unknown();
    }

    public synchronized void serverNotDetected() {
        snapshot = ModStatusKit.serverNotDetected();
    }

    public synchronized void connected(String serverVersion) {
        snapshot = ModStatusKit.connected(config, serverVersion);
    }

    public synchronized void connected(String serverVersion, String serverBuild) {
        snapshot = ModStatusKit.connected(config, serverVersion, serverBuild);
    }

    public synchronized boolean markServerNotDetectedIfUnknown() {
        if (snapshot.status() != VersionStatus.UNKNOWN) {
            return false;
        }
        serverNotDetected();
        return true;
    }
}
