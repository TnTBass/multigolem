package dev.charles.multigolem.fabric.customizations;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.customizations.ServerCustomizationsPayload;
import dev.charles.multigolem.customizations.ServerCustomizationsSummarizer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class FabricServerCustomizationsNetworking {
    private static boolean registered;

    private FabricServerCustomizationsNetworking() {
    }

    public static void registerServer() {
        if (registered) {
            return;
        }
        registered = true;

        PayloadTypeRegistry.clientboundPlay().register(ServerCustomizationsPayload.TYPE, ServerCustomizationsPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendServerCustomizations(handler.player));
    }

    static boolean sendServerCustomizations(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, ServerCustomizationsPayload.TYPE)) {
            MultiGolem.LOG.debug("Skipping MultiGolem customizations payload; client did not advertise {}", ServerCustomizationsPayload.ID);
            return false;
        }

        ServerPlayNetworking.send(player, new ServerCustomizationsPayload(
            ServerCustomizationsSummarizer.snapshot(MultiGolem.config())
        ));
        return true;
    }
}
