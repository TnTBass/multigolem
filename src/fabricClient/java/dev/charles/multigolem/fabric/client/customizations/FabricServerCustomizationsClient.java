package dev.charles.multigolem.fabric.client.customizations;

import dev.charles.multigolem.client.customizations.ServerCustomizationsClient;
import dev.charles.multigolem.customizations.ServerCustomizationsPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class FabricServerCustomizationsClient {
    private static volatile boolean registered;

    private FabricServerCustomizationsClient() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientPlayNetworking.registerGlobalReceiver(ServerCustomizationsPayload.TYPE, (payload, context) ->
            context.client().execute(() -> ServerCustomizationsClient.state().onServerSnapshot(payload.snapshot()))
        );
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ServerCustomizationsClient.state().onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ServerCustomizationsClient.state().onDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> ServerCustomizationsClient.state().tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ServerCustomizationsClient.state().onDisconnect());
    }
}
