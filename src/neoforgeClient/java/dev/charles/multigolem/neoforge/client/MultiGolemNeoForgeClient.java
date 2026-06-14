package dev.charles.multigolem.neoforge.client;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.client.modmenu.MultiGolemStatusScreen;
import dev.charles.multigolem.neoforge.client.network.NeoForgeMultiGolemClientNetworking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = MultiGolem.MOD_ID, dist = { Dist.CLIENT })
public final class MultiGolemNeoForgeClient {
    public MultiGolemNeoForgeClient(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
            (ignoredContainer, parent) -> new MultiGolemStatusScreen(parent));
        NeoForgeMultiGolemClientNetworking.register(modBus);
        MultiGolem.LOG.info("MultiGolem NeoForge client skeleton initializing");
    }
}
