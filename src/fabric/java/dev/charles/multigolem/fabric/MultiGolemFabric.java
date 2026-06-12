package dev.charles.multigolem.fabric;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemStorage;
import dev.charles.multigolem.fabric.attachment.FabricGolemAttachments;
import dev.charles.multigolem.fabric.customizations.FabricServerCustomizationsNetworking;
import dev.charles.multigolem.fabric.event.FabricMultiGolemEvents;
import dev.charles.multigolem.fabric.permissions.FabricMultiGolemPermissions;
import dev.charles.multigolem.fabric.status.FabricMultiGolemStatus;
import dev.charles.multigolem.fabric.status.FabricMultiGolemStatusNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class MultiGolemFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        GolemStorage.register(FabricGolemAttachments.storageAdapter());
        FabricGolemAttachments.register();
        FabricMultiGolemStatus.initializeVersion();
        MultiGolem.initialize(FabricLoader.getInstance().getConfigDir().resolve(MultiGolem.MOD_ID + ".json"));
        FabricMultiGolemPermissions.register();
        FabricMultiGolemStatusNetworking.registerServer();
        FabricServerCustomizationsNetworking.registerServer();
        FabricMultiGolemEvents.register();
    }
}
