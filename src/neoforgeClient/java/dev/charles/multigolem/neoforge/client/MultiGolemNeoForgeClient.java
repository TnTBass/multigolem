package dev.charles.multigolem.neoforge.client;

import dev.charles.multigolem.MultiGolem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = MultiGolem.MOD_ID, dist = Dist.CLIENT)
public final class MultiGolemNeoForgeClient {
    public MultiGolemNeoForgeClient() {
        MultiGolem.LOG.info("MultiGolem NeoForge client skeleton initializing");
    }
}
