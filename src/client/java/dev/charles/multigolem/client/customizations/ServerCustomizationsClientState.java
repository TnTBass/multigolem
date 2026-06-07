package dev.charles.multigolem.client.customizations;

import dev.charles.multigolem.customizations.ServerCustomizationsSummary;
import dev.charles.multigolem.customizations.ServerCustomizationsSnapshot;
import dev.charles.multigolem.customizations.ServerCustomizationsSummarizer;

import java.util.Objects;
import java.util.Optional;

public final class ServerCustomizationsClientState {
    public enum ViewState {
        UNAVAILABLE,
        PENDING,
        AVAILABLE
    }

    private final int timeoutTicks;
    private ViewState viewState = ViewState.UNAVAILABLE;
    private int pendingTicks;
    private ServerCustomizationsSummary summary;
    private ServerCustomizationsSnapshot snapshot;

    public ServerCustomizationsClientState(int timeoutTicks) {
        if (timeoutTicks < 1) {
            throw new IllegalArgumentException("timeoutTicks must be positive");
        }
        this.timeoutTicks = timeoutTicks;
    }

    public void onJoin() {
        viewState = ViewState.PENDING;
        pendingTicks = 0;
        summary = null;
        snapshot = null;
    }

    public void onServerSnapshot(ServerCustomizationsSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.summary = ServerCustomizationsSummarizer.summary(snapshot);
        viewState = ViewState.AVAILABLE;
        pendingTicks = 0;
    }

    public void tick() {
        if (viewState != ViewState.PENDING) {
            return;
        }
        pendingTicks++;
        if (pendingTicks >= timeoutTicks) {
            clear();
        }
    }

    public void onDisconnect() {
        clear();
    }

    public void onScreenClose() {
        if (viewState == ViewState.PENDING) {
            clear();
        }
    }

    public ViewState viewState() {
        return viewState;
    }

    public Optional<ServerCustomizationsSummary> summary() {
        return Optional.ofNullable(summary);
    }

    public Optional<ServerCustomizationsSnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    private void clear() {
        viewState = ViewState.UNAVAILABLE;
        pendingTicks = 0;
        summary = null;
        snapshot = null;
    }
}
