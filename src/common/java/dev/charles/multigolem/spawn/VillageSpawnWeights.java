package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

public record VillageSpawnWeights(boolean enabled, EnumMap<GolemVariant, Integer> weights) {

    static final List<GolemVariant> ROLL_ORDER = List.of(
        GolemVariant.IRON,
        GolemVariant.COPPER,
        GolemVariant.REDSTONE,
        GolemVariant.GOLD,
        GolemVariant.EMERALD,
        GolemVariant.DIAMOND,
        GolemVariant.NETHERITE
    );

    public VillageSpawnWeights {
        weights = sanitize(weights);
    }

    public static List<GolemVariant> rollOrder() {
        return ROLL_ORDER;
    }

    @Override
    public EnumMap<GolemVariant, Integer> weights() {
        return new EnumMap<>(weights);
    }

    public static VillageSpawnWeights defaults() {
        EnumMap<GolemVariant, Integer> defaults = new EnumMap<>(GolemVariant.class);
        defaults.put(GolemVariant.IRON, 19);
        defaults.put(GolemVariant.COPPER, 19);
        defaults.put(GolemVariant.REDSTONE, 19);
        defaults.put(GolemVariant.GOLD, 19);
        defaults.put(GolemVariant.EMERALD, 19);
        defaults.put(GolemVariant.DIAMOND, 5);
        defaults.put(GolemVariant.NETHERITE, 0);
        return new VillageSpawnWeights(true, defaults);
    }

    public VillageSpawnWeights withEnabled(boolean enabled) {
        return new VillageSpawnWeights(enabled, weights);
    }

    public int weight(GolemVariant variant) {
        return weights.getOrDefault(variant, 0);
    }

    public int totalWeight() {
        int total = 0;
        for (GolemVariant variant : ROLL_ORDER) {
            total += weight(variant);
        }
        return total;
    }

    public boolean isAllZero() {
        return totalWeight() == 0;
    }

    public Optional<GolemVariant> roll(IntUnaryOperator nextIntBounded) {
        if (!enabled || isAllZero()) return Optional.empty();

        int ticket = nextIntBounded.applyAsInt(totalWeight());
        int cursor = 0;
        for (GolemVariant variant : ROLL_ORDER) {
            cursor += weight(variant);
            if (ticket < cursor) return Optional.of(variant);
        }
        return Optional.empty();
    }

    private static EnumMap<GolemVariant, Integer> sanitize(Map<GolemVariant, Integer> input) {
        EnumMap<GolemVariant, Integer> result = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : ROLL_ORDER) {
            int raw = input == null ? 0 : input.getOrDefault(variant, 0);
            result.put(variant, Math.max(0, raw));
        }
        return result;
    }
}
