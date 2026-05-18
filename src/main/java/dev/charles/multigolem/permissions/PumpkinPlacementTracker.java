package dev.charles.multigolem.permissions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class PumpkinPlacementTracker {
    private static final ThreadLocal<Placement> CURRENT = new ThreadLocal<>();

    private PumpkinPlacementTracker() {}

    public static void withCurrentPlacement(ServerPlayer player, BlockPos pos, Runnable action) {
        withCurrentPlacement((Object) player, pos, action);
    }

    static void withCurrentPlacement(Object player, BlockPos pos, Runnable action) {
        CURRENT.set(new Placement(player, pos.immutable()));
        try {
            action.run();
        } finally {
            CURRENT.remove();
        }
    }

    public static Optional<ServerPlayer> currentServerPlayerFor(BlockPos pos) {
        return currentPlayerFor(pos)
            .filter(ServerPlayer.class::isInstance)
            .map(ServerPlayer.class::cast);
    }

    static Optional<Object> currentPlayerFor(BlockPos pos) {
        Placement placement = CURRENT.get();
        if (placement == null || !placement.pos.equals(pos)) {
            return Optional.empty();
        }
        return Optional.of(placement.player);
    }

    private record Placement(Object player, BlockPos pos) {}
}
