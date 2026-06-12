package dev.charles.multigolem.ability;

import dev.charles.multigolem.MultiGolem;

/**
 * Common ability marker retained for source compatibility while loader adapters own event wiring.
 */
public final class AbilityRegistry {

    private AbilityRegistry() {}

    public static void register() {
        MultiGolem.LOG.debug("AbilityRegistry: common ability hooks are loader-wired");
    }
}
