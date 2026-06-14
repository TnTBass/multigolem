package dev.charles.multigolem.neoforge;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemStorage;
import dev.charles.multigolem.neoforge.ability.NeoForgeAbilityEvents;
import dev.charles.multigolem.neoforge.attachment.NeoForgeGolemAttachments;
import dev.charles.multigolem.neoforge.event.NeoForgeMultiGolemEvents;
import dev.charles.multigolem.neoforge.network.NeoForgeMultiGolemNetworking;
import dev.charles.multigolem.neoforge.permissions.NeoForgeMultiGolemPermissions;
import dev.charles.multigolem.status.MultiGolemStatus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MultiGolem.MOD_ID)
public final class MultiGolemNeoForge {
    public MultiGolemNeoForge(IEventBus modBus, ModContainer container) {
        NeoForgeGolemAttachments.register(modBus);
        GolemStorage.register(NeoForgeGolemAttachments.storageAdapter());
        MultiGolemStatus.initializeVersion(container.getModInfo().getVersion().toString());
        MultiGolem.initialize(FMLPaths.CONFIGDIR.get().resolve(MultiGolem.MOD_ID + ".json"));
        NeoForgeMultiGolemPermissions.register();
        NeoForgeMultiGolemNetworking.register(modBus);
        NeoForgeMultiGolemEvents.register(modBus);
        NeoForgeAbilityEvents.register();
        MultiGolem.LOG.info("MultiGolem NeoForge initializing");
    }
}
