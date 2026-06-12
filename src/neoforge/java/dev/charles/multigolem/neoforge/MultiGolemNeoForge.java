package dev.charles.multigolem.neoforge;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.status.MultiGolemStatus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MultiGolem.MOD_ID)
public final class MultiGolemNeoForge {
    public MultiGolemNeoForge(ModContainer container) {
        MultiGolemStatus.initializeVersion(container.getModInfo().getVersion().toString(), System.getProperty("multigolem.build"));
        MultiGolem.LOG.info("MultiGolem NeoForge skeleton initializing");
    }
}
