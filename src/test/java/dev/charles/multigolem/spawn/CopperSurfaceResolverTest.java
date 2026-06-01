package dev.charles.multigolem.spawn;

import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopperSurfaceResolverTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @ParameterizedTest
    @MethodSource("singleCopperBlocks")
    void singleCopperFamilyBlockMapsToExpectedSurface(Block block, GolemWeatheringStage stage, boolean waxed) {
        assertEquals(Optional.of(new GolemSurfaceState(stage, waxed)),
            CopperSurfaceResolver.surfaceFor(block.defaultBlockState()));
    }

    @Test
    void mixedWeatheringUsesMostOxidizedStage() {
        assertEquals(Optional.of(new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false)),
            CopperSurfaceResolver.resolveBody(List.of(
                state(Blocks.COPPER_BLOCK),
                state(Blocks.EXPOSED_COPPER),
                state(Blocks.WEATHERED_COPPER),
                state(Blocks.OXIDIZED_COPPER)
            )));
    }

    @Test
    void mixedWaxedAndUnwaxedUsesUnwaxedUnlessEveryBlockIsWaxed() {
        assertEquals(Optional.of(new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false)),
            CopperSurfaceResolver.resolveBody(List.of(
                state(Blocks.WAXED_EXPOSED_COPPER),
                state(Blocks.EXPOSED_COPPER),
                state(Blocks.WAXED_EXPOSED_COPPER),
                state(Blocks.EXPOSED_COPPER)
            )));
    }

    @Test
    void allWaxedBodyBlocksResolveAsWaxed() {
        assertEquals(Optional.of(new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true)),
            CopperSurfaceResolver.resolveBody(List.of(
                state(Blocks.WAXED_WEATHERED_COPPER),
                state(Blocks.WAXED_WEATHERED_COPPER),
                state(Blocks.WAXED_WEATHERED_COPPER),
                state(Blocks.WAXED_WEATHERED_COPPER)
            )));
    }

    @Test
    void nonCopperBodyBlocksResolveEmpty() {
        assertEquals(Optional.empty(), CopperSurfaceResolver.resolveBody(List.of(
            state(Blocks.COPPER_BLOCK),
            state(Blocks.IRON_BLOCK),
            state(Blocks.COPPER_BLOCK),
            state(Blocks.COPPER_BLOCK)
        )));
    }

    @Test
    void emptyOrMissingBodyBlocksResolveEmpty() {
        assertEquals(Optional.empty(), CopperSurfaceResolver.resolveBody(List.of()));
        assertEquals(Optional.empty(), CopperSurfaceResolver.resolveBody(null));
        assertEquals(Optional.empty(), CopperSurfaceResolver.surfaceFor(state(Blocks.IRON_BLOCK)));
    }

    private static Stream<Arguments> singleCopperBlocks() {
        return Stream.of(
            Arguments.of(Blocks.COPPER_BLOCK, GolemWeatheringStage.UNAFFECTED, false),
            Arguments.of(Blocks.EXPOSED_COPPER, GolemWeatheringStage.EXPOSED, false),
            Arguments.of(Blocks.WEATHERED_COPPER, GolemWeatheringStage.WEATHERED, false),
            Arguments.of(Blocks.OXIDIZED_COPPER, GolemWeatheringStage.OXIDIZED, false),
            Arguments.of(Blocks.WAXED_COPPER_BLOCK, GolemWeatheringStage.UNAFFECTED, true),
            Arguments.of(Blocks.WAXED_EXPOSED_COPPER, GolemWeatheringStage.EXPOSED, true),
            Arguments.of(Blocks.WAXED_WEATHERED_COPPER, GolemWeatheringStage.WEATHERED, true),
            Arguments.of(Blocks.WAXED_OXIDIZED_COPPER, GolemWeatheringStage.OXIDIZED, true)
        );
    }

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }
}
