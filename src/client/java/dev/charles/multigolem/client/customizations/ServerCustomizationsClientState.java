package dev.charles.multigolem.client.customizations;

import dev.charles.multigolem.customizations.ServerCustomizationsSummary;

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
    }

    public void onServerSummary(ServerCustomizationsSummary summary) {
        this.summary = Objects.requireNonNull(summary, "summary");
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

    private void clear() {
        viewState = ViewState.UNAVAILABLE;
        pendingTicks = 0;
        summary = null;
    }
}
