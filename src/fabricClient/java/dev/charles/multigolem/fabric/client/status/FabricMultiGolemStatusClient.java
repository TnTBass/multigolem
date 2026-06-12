package dev.charles.multigolem.fabric.client.status;

import dev.charles.multigolem.status.MultiGolemStatus;
import dev.charles.multigolem.status.MultiGolemStatusPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricMultiGolemStatusClient {
    private static volatile boolean registered;

    private FabricMultiGolemStatusClient() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientPlayNetworking.registerGlobalReceiver(MultiGolemStatusPayload.TYPE, (payload, context) ->
            context.client().execute(() -> MultiGolemStatus.onServerStatus(payload.serverStatus()))
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> MultiGolemStatus.onClientJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MultiGolemStatus.onClientDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> MultiGolemStatus.tickClientStatus());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> MultiGolemStatus.onClientDisconnect());
    }
}
