package dev.charles.multigolem.customizations;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClientState;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.customizations.ServerCustomizationsSnapshot;
import dev.charles.multigolem.customizations.VariantCustomizationSummary;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsClientStateTest {
    @Test
    void disconnectedStartsUnavailable() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
        assertTrue(state.snapshot().isEmpty());
    }

    @Test
    void pendingFallsBackToUnavailableAfterTimeout() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(3);

        state.onJoin();
        assertEquals(ServerCustomizationsClientState.ViewState.PENDING, state.viewState());

        state.tick();
        state.tick();
        assertEquals(ServerCustomizationsClientState.ViewState.PENDING, state.viewState());

        state.tick();
        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
        assertTrue(state.snapshot().isEmpty());
    }

    @Test
    void pendingResolvesUnavailableOnDisconnect() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        state.onJoin();
        state.onDisconnect();

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
        assertTrue(state.snapshot().isEmpty());
    }

    @Test
    void pendingResolvesUnavailableOnScreenClose() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        state.onJoin();
        state.onScreenClose();

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
        assertTrue(state.snapshot().isEmpty());
    }

    @Test
    void receivedSummaryBecomesAvailableAndDisconnectClearsIt() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);
        ServerCustomizationsSnapshot snapshot = snapshotWithCopperStats("Health: 222");
        ServerCustomizationsSummary summary = dev.charles.multigolem.customizations.ServerCustomizationsSummarizer.summary(snapshot);

        state.onJoin();
        state.onServerSnapshot(snapshot);

        assertEquals(ServerCustomizationsClientState.ViewState.AVAILABLE, state.viewState());
        assertEquals(summary, state.summary().orElseThrow());
        assertEquals(List.of("Health: 222"), state.snapshot().orElseThrow().golempediaStats().get(GolemVariant.COPPER));

        state.onDisconnect();
        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
        assertTrue(state.snapshot().isEmpty());
    }

    private static ServerCustomizationsSnapshot snapshotWithCopperStats(String line) {
        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        weights.put(GolemVariant.COPPER, 1);
        EnumMap<GolemVariant, List<String>> stats = new EnumMap<>(GolemVariant.class);
        stats.put(GolemVariant.COPPER, List.of(line));
        return new ServerCustomizationsSnapshot(
            true,
            true,
            weights,
            true,
            "permissions unavailable",
            List.of(new VariantCustomizationSummary(GolemVariant.COPPER, List.of())),
            stats
        );
    }
}
