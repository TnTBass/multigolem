package dev.charles.multigolem.neoforge.client.network;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClient;
import dev.charles.multigolem.customizations.ServerCustomizationsPayload;
import dev.charles.multigolem.status.MultiGolemStatus;
import dev.charles.multigolem.status.MultiGolemStatusPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeMultiGolemClientNetworking {
    private NeoForgeMultiGolemClientNetworking() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeMultiGolemClientNetworking::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientJoin);
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientDisconnect);
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemClientNetworking::onClientStopping);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(MultiGolemStatusPayload.TYPE, (payload, context) ->
            context.enqueueWork(() -> MultiGolemStatus.onServerStatus(payload.serverStatus()))
        );
        event.register(ServerCustomizationsPayload.TYPE, (payload, context) ->
            context.enqueueWork(() -> ServerCustomizationsClient.state().onServerSnapshot(payload.snapshot()))
        );
    }

    private static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        MultiGolemStatus.onClientJoin();
        ServerCustomizationsClient.state().onJoin();
    }

    private static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        MultiGolemStatus.onClientDisconnect();
        ServerCustomizationsClient.state().onDisconnect();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        MultiGolemStatus.tickClientStatus();
        ServerCustomizationsClient.state().tick();
    }

    private static void onClientStopping(ClientStoppingEvent event) {
        MultiGolemStatus.onClientDisconnect();
        ServerCustomizationsClient.state().onDisconnect();
    }
}
