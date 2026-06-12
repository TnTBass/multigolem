package dev.charles.multigolem.client.customizations;

import dev.charles.multigolem.customizations.ServerCustomizationsPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ServerCustomizationsClient {
    private static final int TIMEOUT_TICKS = 100;
    private static final ServerCustomizationsClientState STATE = new ServerCustomizationsClientState(TIMEOUT_TICKS);
    private static volatile boolean registered;

    private ServerCustomizationsClient() {
    }

    public static ServerCustomizationsClientState state() {
        return STATE;
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        ClientPlayNetworking.registerGlobalReceiver(ServerCustomizationsPayload.TYPE, (payload, context) ->
            context.client().execute(() -> STATE.onServerSnapshot(payload.snapshot()))
        );
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> STATE.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.onDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> STATE.tick());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> STATE.onDisconnect());
    }
}
