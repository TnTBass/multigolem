package dev.charles.multigolem.fabric.client;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.fabric.client.customizations.FabricServerCustomizationsClient;
import dev.charles.multigolem.fabric.client.status.FabricMultiGolemStatusClient;
import net.fabricmc.api.ClientModInitializer;

public final class MultiGolemFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MultiGolem.LOG.info("MultiGolem client initializing");
        FabricMultiGolemStatusClient.register();
        FabricServerCustomizationsClient.register();
    }
}
