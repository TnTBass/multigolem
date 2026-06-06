package dev.charles.multigolem.customizations;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClientState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerCustomizationsClientStateTest {
    @Test
    void disconnectedStartsUnavailable() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
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
    }

    @Test
    void pendingResolvesUnavailableOnDisconnect() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        state.onJoin();
        state.onDisconnect();

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }

    @Test
    void pendingResolvesUnavailableOnScreenClose() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);

        state.onJoin();
        state.onScreenClose();

        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }

    @Test
    void receivedSummaryBecomesAvailableAndDisconnectClearsIt() {
        ServerCustomizationsClientState state = new ServerCustomizationsClientState(100);
        ServerCustomizationsSummary summary = new ServerCustomizationsSummary(
            java.util.List.of("Global healing: enabled"),
            java.util.List.of(),
            java.util.List.of(),
            java.util.List.of()
        );

        state.onJoin();
        state.onServerSummary(summary);

        assertEquals(ServerCustomizationsClientState.ViewState.AVAILABLE, state.viewState());
        assertEquals(summary, state.summary().orElseThrow());

        state.onDisconnect();
        assertEquals(ServerCustomizationsClientState.ViewState.UNAVAILABLE, state.viewState());
        assertTrue(state.summary().isEmpty());
    }
}
