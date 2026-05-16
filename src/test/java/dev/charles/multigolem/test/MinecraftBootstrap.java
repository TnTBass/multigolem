package dev.charles.multigolem.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/** Shared helper to bootstrap Minecraft static state for unit tests. */
public final class MinecraftBootstrap {

    private static boolean done = false;

    private MinecraftBootstrap() {}

    /** Idempotent. Call from @BeforeAll in any test that touches Blocks, Items, or other registry-backed statics. */
    public static synchronized void ensure() {
        if (done) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        done = true;
    }
}
