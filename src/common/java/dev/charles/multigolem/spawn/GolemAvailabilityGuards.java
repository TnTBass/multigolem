package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;

public final class GolemAvailabilityGuards {
    private GolemAvailabilityGuards() {}

    public static boolean canCreate(MultiGolemConfig config, GolemIdentity identity) {
        return config.golemAvailability().isAvailable(identity);
    }

    public static boolean canCreate(MultiGolemConfig config, GolemFamily family, GolemVariant variant) {
        return config.golemAvailability().isAvailable(family, variant);
    }
}
