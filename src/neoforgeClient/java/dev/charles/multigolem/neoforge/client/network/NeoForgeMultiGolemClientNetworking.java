package dev.charles.multigolem.neoforge.client.network;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClient;
import dev.charles.multigolem.customizations.ServerCustomizationsPayload;
import dev.charles.multigolem.status.MultiGolemStatus;
import dev.charles.multigolem.status.MultiGolemStatusPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class NeoForgeMultiGolemClientNetworking {
    private NeoForgeMultiGolemClientNetworking() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(NeoForgeMultiGolemClientNetworking::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(MultiGolemStatusPayload.TYPE, (payload, context) ->
            MultiGolemStatus.onServerStatus(payload.serverStatus())
        );
        event.register(ServerCustomizationsPayload.TYPE, (payload, context) ->
            ServerCustomizationsClient.state().onServerSnapshot(payload.snapshot())
        );
    }
}
