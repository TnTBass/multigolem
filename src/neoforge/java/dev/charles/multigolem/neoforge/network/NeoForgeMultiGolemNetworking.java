package dev.charles.multigolem.neoforge.network;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.customizations.ServerCustomizationsPayload;
import dev.charles.multigolem.customizations.ServerCustomizationsSummarizer;
import dev.charles.multigolem.internal.modstatus.VersionMismatchSeverity;
import dev.charles.multigolem.status.MultiGolemStatus;
import dev.charles.multigolem.status.MultiGolemStatusPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NeoForgeMultiGolemNetworking {
    private NeoForgeMultiGolemNetworking() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeMultiGolemNetworking::registerPayloads);
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemNetworking::onPlayerLoggedIn);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(MultiGolem.MOD_ID).optional()
            .playToClient(MultiGolemStatusPayload.TYPE, MultiGolemStatusPayload.CODEC)
            .playToClient(ServerCustomizationsPayload.TYPE, ServerCustomizationsPayload.CODEC);
    }

    static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendServerVersion(player);
            sendServerCustomizations(player);
        }
    }

    static boolean sendServerVersion(ServerPlayer player) {
        if (!player.connection.hasChannel(MultiGolemStatusPayload.TYPE)) {
            MultiGolem.LOG.debug("Skipping MultiGolem status payload; client did not advertise {}", MultiGolemStatusPayload.ID);
            return false;
        }

        PacketDistributor.sendToPlayer(player, MultiGolemStatusPayload.fromServerStatus(
            MultiGolemStatus.config().clientVersion(),
            MultiGolemStatus.config().clientBuild(),
            VersionMismatchSeverity.WARN
        ));
        return true;
    }

    static boolean sendServerCustomizations(ServerPlayer player) {
        if (!player.connection.hasChannel(ServerCustomizationsPayload.TYPE)) {
            MultiGolem.LOG.debug("Skipping MultiGolem customizations payload; client did not advertise {}", ServerCustomizationsPayload.ID);
            return false;
        }

        PacketDistributor.sendToPlayer(player, new ServerCustomizationsPayload(
            ServerCustomizationsSummarizer.snapshot(MultiGolem.config())
        ));
        return true;
    }
}
