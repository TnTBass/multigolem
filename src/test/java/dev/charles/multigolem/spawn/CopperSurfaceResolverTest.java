package dev.charles.multigolem.spawn;

import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
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
                state(copper(WeatheringCopper.WeatherState.UNAFFECTED)),
                state(copper(WeatheringCopper.WeatherState.EXPOSED)),
                state(copper(WeatheringCopper.WeatherState.WEATHERED)),
                state(copper(WeatheringCopper.WeatherState.OXIDIZED))
            )));
    }

    @Test
    void mixedWaxedAndUnwaxedUsesUnwaxedUnlessEveryBlockIsWaxed() {
        assertEquals(Optional.of(new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false)),
            CopperSurfaceResolver.resolveBody(List.of(
                state(waxedCopper(WeatheringCopper.WeatherState.EXPOSED)),
                state(copper(WeatheringCopper.WeatherState.EXPOSED)),
                state(waxedCopper(WeatheringCopper.WeatherState.EXPOSED)),
                state(copper(WeatheringCopper.WeatherState.EXPOSED))
            )));
    }

    @Test
    void allWaxedBodyBlocksResolveAsWaxed() {
        assertEquals(Optional.of(new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true)),
            CopperSurfaceResolver.resolveBody(List.of(
                state(waxedCopper(WeatheringCopper.WeatherState.WEATHERED)),
                state(waxedCopper(WeatheringCopper.WeatherState.WEATHERED)),
                state(waxedCopper(WeatheringCopper.WeatherState.WEATHERED)),
                state(waxedCopper(WeatheringCopper.WeatherState.WEATHERED))
            )));
    }

    @Test
    void nonCopperBodyBlocksResolveEmpty() {
        assertEquals(Optional.empty(), CopperSurfaceResolver.resolveBody(List.of(
            state(copper(WeatheringCopper.WeatherState.UNAFFECTED)),
            state(Blocks.IRON_BLOCK),
            state(copper(WeatheringCopper.WeatherState.UNAFFECTED)),
            state(copper(WeatheringCopper.WeatherState.UNAFFECTED))
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
            Arguments.of(copper(WeatheringCopper.WeatherState.UNAFFECTED), GolemWeatheringStage.UNAFFECTED, false),
            Arguments.of(copper(WeatheringCopper.WeatherState.EXPOSED), GolemWeatheringStage.EXPOSED, false),
            Arguments.of(copper(WeatheringCopper.WeatherState.WEATHERED), GolemWeatheringStage.WEATHERED, false),
            Arguments.of(copper(WeatheringCopper.WeatherState.OXIDIZED), GolemWeatheringStage.OXIDIZED, false),
            Arguments.of(waxedCopper(WeatheringCopper.WeatherState.UNAFFECTED), GolemWeatheringStage.UNAFFECTED, true),
            Arguments.of(waxedCopper(WeatheringCopper.WeatherState.EXPOSED), GolemWeatheringStage.EXPOSED, true),
            Arguments.of(waxedCopper(WeatheringCopper.WeatherState.WEATHERED), GolemWeatheringStage.WEATHERED, true),
            Arguments.of(waxedCopper(WeatheringCopper.WeatherState.OXIDIZED), GolemWeatheringStage.OXIDIZED, true)
        );
    }

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }

    private static Block copper(WeatheringCopper.WeatherState state) {
        return Blocks.COPPER_BLOCK.weathering().pick(state);
    }

    private static Block waxedCopper(WeatheringCopper.WeatherState state) {
        return Blocks.COPPER_BLOCK.waxed().pick(state);
    }
}
