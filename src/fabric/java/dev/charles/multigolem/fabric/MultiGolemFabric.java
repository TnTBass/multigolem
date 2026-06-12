package dev.charles.multigolem.fabric;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.AbilityRegistry;
import dev.charles.multigolem.attachment.GolemStorage;
import dev.charles.multigolem.customizations.ServerCustomizationsNetworking;
import dev.charles.multigolem.fabric.attachment.FabricGolemAttachments;
import dev.charles.multigolem.fabric.event.FabricMultiGolemEvents;
import dev.charles.multigolem.status.MultiGolemStatusNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MultiGolemFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GolemStorage.register(FabricGolemAttachments.storageAdapter());
        FabricGolemAttachments.register();
        MultiGolem.initialize(FabricLoader.getInstance().getConfigDir().resolve(MultiGolem.MOD_ID + ".json"));
        AbilityRegistry.register();
        MultiGolemStatusNetworking.registerServer();
        ServerCustomizationsNetworking.registerServer();
        FabricMultiGolemEvents.register();
    }
}
