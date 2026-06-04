package dev.charles.multigolem.status;

import dev.charles.multigolem.MultiGolem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class MultiGolemStatusNetworking {
    private static boolean registered;

    private MultiGolemStatusNetworking() {
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

        ServerPlayNetworking.send(player, MultiGolemStatusPayload.fromServerVersion(
            MultiGolemStatus.config().clientVersion(),
            MultiGolemStatus.config().clientBuild()
        ));
        return true;
    }
}
