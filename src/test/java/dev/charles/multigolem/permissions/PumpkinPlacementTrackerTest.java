package dev.charles.multigolem.permissions;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class PumpkinPlacementTrackerTest {
    @Test
    void currentPlayerFor_returnsPlayerOnlyInsideCurrentPlacementForExactPosition() {
        Object player = new Object();
        BlockPos pumpkinPos = new BlockPos(4, 65, -2);
        BlockPos otherPos = new BlockPos(4, 65, -1);

        assertTrue(PumpkinPlacementTracker.currentPlayerFor(pumpkinPos).isEmpty());

        PumpkinPlacementTracker.withCurrentPlacement(player, pumpkinPos, () -> {
            assertEquals(Optional.of(player), PumpkinPlacementTracker.currentPlayerFor(pumpkinPos));
            assertTrue(PumpkinPlacementTracker.currentPlayerFor(otherPos).isEmpty());
        });

        assertTrue(PumpkinPlacementTracker.currentPlayerFor(pumpkinPos).isEmpty());
    }

    @Test
    void currentPlacement_isClearedAfterActionCompletes() {
        Object player = new Object();
        BlockPos pumpkinPos = new BlockPos(0, 64, 0);

        PumpkinPlacementTracker.withCurrentPlacement(player, pumpkinPos,
            () -> assertEquals(Optional.of(player), PumpkinPlacementTracker.currentPlayerFor(pumpkinPos)));

        assertTrue(PumpkinPlacementTracker.currentPlayerFor(pumpkinPos).isEmpty());
    }

    @Test
    void currentPlacement_isClearedEvenWhenActionThrows() {
        Object player = new Object();
        BlockPos pumpkinPos = new BlockPos(1, 64, 1);
        RuntimeException thrown = new RuntimeException("placement failed");

        RuntimeException actual = assertThrows(RuntimeException.class, () ->
            PumpkinPlacementTracker.withCurrentPlacement(player, pumpkinPos, () -> {
                assertEquals(Optional.of(player), PumpkinPlacementTracker.currentPlayerFor(pumpkinPos));
                throw thrown;
            }));

        assertSame(thrown, actual);
        assertTrue(PumpkinPlacementTracker.currentPlayerFor(pumpkinPos).isEmpty());
    }

    @Test
    void mismatchedPositions_returnEmpty() {
        Object player = new Object();
        BlockPos pumpkinPos = new BlockPos(8, 70, 8);
        BlockPos mismatchedPos = new BlockPos(8, 70, 9);

        PumpkinPlacementTracker.withCurrentPlacement(player, pumpkinPos,
            () -> assertTrue(PumpkinPlacementTracker.currentPlayerFor(mismatchedPos).isEmpty()));
    }

    @Test
    void nestedPlacements_restoreOuterPlacementAfterInnerCompletes() {
        Object outerPlayer = new Object();
        Object innerPlayer = new Object();
        BlockPos outerPos = new BlockPos(10, 70, 10);
        BlockPos innerPos = new BlockPos(11, 71, 11);

        PumpkinPlacementTracker.withCurrentPlacement(outerPlayer, outerPos, () -> {
            assertEquals(Optional.of(outerPlayer), PumpkinPlacementTracker.currentPlayerFor(outerPos));

            PumpkinPlacementTracker.withCurrentPlacement(innerPlayer, innerPos, () -> {
                assertEquals(Optional.of(innerPlayer), PumpkinPlacementTracker.currentPlayerFor(innerPos));
                assertTrue(PumpkinPlacementTracker.currentPlayerFor(outerPos).isEmpty());
            });

            assertEquals(Optional.of(outerPlayer), PumpkinPlacementTracker.currentPlayerFor(outerPos));
            assertTrue(PumpkinPlacementTracker.currentPlayerFor(innerPos).isEmpty());
        });

        assertTrue(PumpkinPlacementTracker.currentPlayerFor(outerPos).isEmpty());
        assertTrue(PumpkinPlacementTracker.currentPlayerFor(innerPos).isEmpty());
    }

    @Test
    void publicCurrentServerPlayerFor_doesNotExposeNonServerObjects() {
        Object player = new Object();
        BlockPos pumpkinPos = new BlockPos(-3, 80, 12);
        AtomicBoolean checkedInsidePlacement = new AtomicBoolean(false);

        PumpkinPlacementTracker.withCurrentPlacement(player, pumpkinPos, () -> {
            assertTrue(PumpkinPlacementTracker.currentServerPlayerFor(pumpkinPos).isEmpty());
            checkedInsidePlacement.set(true);
        });

        assertTrue(checkedInsidePlacement.get());
        assertTrue(PumpkinPlacementTracker.currentServerPlayerFor(pumpkinPos).isEmpty());
    }
}
