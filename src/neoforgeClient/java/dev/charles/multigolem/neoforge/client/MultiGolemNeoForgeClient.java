package dev.charles.multigolem.neoforge.client;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.neoforge.client.network.NeoForgeMultiGolemClientNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = MultiGolem.MOD_ID, dist = { Dist.CLIENT })
public final class MultiGolemNeoForgeClient {
    public MultiGolemNeoForgeClient(IEventBus modBus) {
        NeoForgeMultiGolemClientNetworking.register(modBus);
        MultiGolem.LOG.info("MultiGolem NeoForge client skeleton initializing");
    }
}
