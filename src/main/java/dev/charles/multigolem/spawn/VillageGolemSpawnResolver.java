package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;

import java.util.Optional;
import java.util.function.IntUnaryOperator;

public final class VillageGolemSpawnResolver {

    private final MultiGolemConfig config;

    public VillageGolemSpawnResolver(MultiGolemConfig config) {
        this.config = config;
    }

    public Optional<GolemVariant> rollVariant(IntUnaryOperator nextIntBounded) {
        Optional<GolemVariant> rolled = config.villageSpawnWeights().roll(nextIntBounded);
        if (rolled.isEmpty() || rolled.get() == GolemVariant.IRON) return Optional.empty();
        return rolled;
    }
}
