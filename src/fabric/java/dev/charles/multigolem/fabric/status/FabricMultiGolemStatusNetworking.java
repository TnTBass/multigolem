package dev.charles.multigolem.fabric.status;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.internal.modstatus.VersionMismatchSeverity;
import dev.charles.multigolem.status.MultiGolemStatus;
import dev.charles.multigolem.status.MultiGolemStatusPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class FabricMultiGolemStatusNetworking {
    private static boolean registered;

    private FabricMultiGolemStatusNetworking() {
    }

    public static void registerServer() {
        if (registered) {
            return;
        }
        registered = true;

        PayloadTypeRegistry.clientboundPlay().register(MultiGolemStatusPayload.TYPE, MultiGolemStatusPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendServerVersion(handler.player));
    }

    static boolean sendServerVersion(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, MultiGolemStatusPayload.TYPE)) {
            MultiGolem.LOG.debug("Skipping MultiGolem status payload; client did not advertise {}", MultiGolemStatusPayload.ID);
            return false;
        }

        ServerPlayNetworking.send(player, MultiGolemStatusPayload.fromServerStatus(
            MultiGolemStatus.config().clientVersion(),
            MultiGolemStatus.config().clientBuild(),
            VersionMismatchSeverity.WARN
        ));
        return true;
    }
}
