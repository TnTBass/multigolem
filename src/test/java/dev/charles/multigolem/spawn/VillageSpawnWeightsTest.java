package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.GolemAvailability;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VillageSpawnWeightsTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void defaults_matchV8Spec() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults();
        assertTrue(weights.enabled());
        assertEquals(19, weights.weight(GolemVariant.IRON));
        assertEquals(19, weights.weight(GolemVariant.COPPER));
        assertEquals(19, weights.weight(GolemVariant.REDSTONE));
        assertEquals(19, weights.weight(GolemVariant.GOLD));
        assertEquals(5, weights.weight(GolemVariant.LAPIS));
        assertEquals(19, weights.weight(GolemVariant.EMERALD));
        assertEquals(5, weights.weight(GolemVariant.DIAMOND));
        assertEquals(0, weights.weight(GolemVariant.NETHERITE));
        assertEquals(105, weights.totalWeight());
        assertEquals(List.of(
            GolemVariant.IRON,
            GolemVariant.COPPER,
            GolemVariant.REDSTONE,
            GolemVariant.GOLD,
            GolemVariant.LAPIS,
            GolemVariant.EMERALD,
            GolemVariant.DIAMOND,
            GolemVariant.NETHERITE
        ), VillageSpawnWeights.rollOrder());
    }

    @Test
    void deterministicRollCanReachEachVariant() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults();
        assertEquals(Optional.of(GolemVariant.IRON), weights.roll(bound -> 0));
        assertEquals(Optional.of(GolemVariant.COPPER), weights.roll(bound -> 19));
        assertEquals(Optional.of(GolemVariant.REDSTONE), weights.roll(bound -> 38));
        assertEquals(Optional.of(GolemVariant.GOLD), weights.roll(bound -> 57));
        assertEquals(Optional.of(GolemVariant.LAPIS), weights.roll(bound -> 76));
        assertEquals(Optional.of(GolemVariant.EMERALD), weights.roll(bound -> 81));
        assertEquals(Optional.of(GolemVariant.DIAMOND), weights.roll(bound -> 100));
        assertEquals(Optional.of(GolemVariant.DIAMOND), weights.roll(bound -> 104));
        assertEquals(Optional.empty(), weights.roll(bound -> 105));
    }

    @Test
    void eachSuccessfulCallRollsIndependently() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults();
        AtomicInteger calls = new AtomicInteger();
        assertEquals(Optional.of(GolemVariant.IRON), weights.roll(bound -> calls.getAndIncrement() == 0 ? 0 : 104));
        assertEquals(Optional.of(GolemVariant.DIAMOND), weights.roll(bound -> calls.getAndIncrement() == 0 ? 0 : 104));
        assertEquals(2, calls.get());
    }

    @Test
    void explicitNetheriteWeightCanRollNetherite() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : GolemVariant.values()) {
            map.put(variant, 0);
        }
        map.put(GolemVariant.NETHERITE, 1);

        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);

        assertEquals(Optional.of(GolemVariant.NETHERITE), weights.roll(bound -> 0));
    }

    @Test
    void rollAvailableSkipsDisabledVariants() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : GolemVariant.values()) {
            map.put(variant, 0);
        }
        map.put(GolemVariant.COPPER, 10);
        map.put(GolemVariant.DIAMOND, 10);
        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
        GolemAvailability availability = GolemAvailability.defaults()
            .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.COPPER, false);

        assertEquals(Optional.of(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)),
            weights.rollAvailable(availability, bound -> 0));
    }

    @Test
    void rollAvailableReturnsEmptyWhenAllPositiveWeightsAreDisabled() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : GolemVariant.values()) {
            map.put(variant, 0);
        }
        map.put(GolemVariant.COPPER, 10);
        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
        GolemAvailability availability = GolemAvailability.defaults()
            .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.COPPER, false);

        assertEquals(Optional.empty(), weights.rollAvailable(availability, bound -> fail("no available weight should remain")));
    }

    @Test
    void enabledFalseLeavesIronWithoutRolling() {
        VillageSpawnWeights weights = VillageSpawnWeights.defaults().withEnabled(false);
        assertEquals(Optional.empty(), weights.roll(bound -> fail("disabled weights must not call random")));
    }

    @Test
    void explicitAllZeroRecognizedWeightsDisableRolling() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : GolemVariant.values()) {
            map.put(variant, 0);
        }

        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
        assertTrue(weights.isAllZero());
        assertEquals(Optional.empty(), weights.roll(bound -> fail("all-zero weights must not call random")));
    }

    @Test
    void negativeWeightsClampToZero() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        map.put(GolemVariant.IRON, -10);
        map.put(GolemVariant.COPPER, 1);
        map.put(GolemVariant.REDSTONE, 0);
        map.put(GolemVariant.GOLD, 0);
        map.put(GolemVariant.LAPIS, 0);
        map.put(GolemVariant.EMERALD, 0);
        map.put(GolemVariant.DIAMOND, 0);
        map.put(GolemVariant.NETHERITE, 0);

        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);
        assertEquals(0, weights.weight(GolemVariant.IRON));
        assertEquals(Optional.of(GolemVariant.COPPER), weights.roll(bound -> 0));
    }

    @Test
    void constructorAndAccessorDefensivelyCopyWeights() {
        EnumMap<GolemVariant, Integer> map = new EnumMap<>(GolemVariant.class);
        map.put(GolemVariant.IRON, 1);
        VillageSpawnWeights weights = new VillageSpawnWeights(true, map);

        map.put(GolemVariant.IRON, 99);
        assertEquals(1, weights.weight(GolemVariant.IRON));

        EnumMap<GolemVariant, Integer> copy = weights.weights();
        copy.put(GolemVariant.IRON, 42);
        assertEquals(1, weights.weight(GolemVariant.IRON));
    }
}
