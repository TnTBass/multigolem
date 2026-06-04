package dev.charles.multigolem.client;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.client.status.MultiGolemStatusClient;
import net.fabricmc.api.ClientModInitializer;

public class MultiGolemClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MultiGolem.LOG.info("MultiGolem client initializing");
        MultiGolemStatusClient.register();
    }
}
