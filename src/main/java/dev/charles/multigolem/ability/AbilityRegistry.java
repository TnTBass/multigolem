package dev.charles.multigolem.ability;

import dev.charles.multigolem.MultiGolem;

/**
 * Single entry point called from MultiGolem.onInitialize to wire all V2 ability event listeners.
 * Each ability class's register() method adds its own Fabric event listeners.
 * No interfaces, no polymorphic framework — just a fan-out to keep onInitialize clean.
 */
public final class AbilityRegistry {

    private AbilityRegistry() {}

    public static void register() {
        GoldAbility.register();          // Task 10
        // EmeraldAbility.register();   (Task 11)
        // CopperAbility.register();    (Task 12)
        // NetheriteAbility.register(); (Task 13)
        // DiamondAbility.register();   (Tasks 16–19)
        MultiGolem.LOG.debug("AbilityRegistry: wired all V2 abilities");
    }
}
