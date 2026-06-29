package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.identity.GolemIdentity;

import java.util.Optional;
import java.util.function.IntUnaryOperator;

public final class VillageGolemSpawnResolver {

    private final MultiGolemConfig config;

    public VillageGolemSpawnResolver(MultiGolemConfig config) {
        this.config = config;
    }

    public Optional<GolemVariant> rollVariant(IntUnaryOperator nextIntBounded) {
        return rollIdentity(nextIntBounded).map(GolemIdentity::variant);
    }

    public Optional<GolemIdentity> rollIdentity(IntUnaryOperator nextIntBounded) {
        Optional<GolemIdentity> rolled = config.villageSpawnWeights()
            .rollAvailable(config.golemAvailability(), nextIntBounded);
        if (rolled.isEmpty() || rolled.get().variant() == GolemVariant.IRON) return Optional.empty();
        return rolled;
    }
}
