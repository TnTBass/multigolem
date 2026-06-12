package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CopperSurfaceResolver {
    private CopperSurfaceResolver() {}

    public static Optional<GolemSurfaceState> surfaceFor(BlockState state) {
        if (state == null || !GolemVariant.isCopperFamilyBlock(state.getBlock())) {
            return Optional.empty();
        }
        Block block = state.getBlock();
        Map<Block, Block> waxOffByBlock = HoneycombItem.WAX_OFF_BY_BLOCK.get();
        boolean waxed = waxOffByBlock.containsKey(block);
        Block unwaxed = waxed ? waxOffByBlock.get(block) : block;
        if (!(unwaxed instanceof WeatheringCopper copper)) {
            return Optional.empty();
        }
        return Optional.of(new GolemSurfaceState(stageFor(copper), waxed));
    }

    public static Optional<GolemSurfaceState> resolveBody(List<BlockState> bodyStates) {
        if (bodyStates == null || bodyStates.isEmpty()) return Optional.empty();

        GolemWeatheringStage latestStage = GolemWeatheringStage.UNAFFECTED;
        boolean allWaxed = true;
        for (BlockState state : bodyStates) {
            Optional<GolemSurfaceState> surface = surfaceFor(state);
            if (surface.isEmpty()) return Optional.empty();

            GolemSurfaceState value = surface.get();
            if (value.weatheringStage().isAfter(latestStage)) {
                latestStage = value.weatheringStage();
            }
            allWaxed &= value.waxed();
        }
        return Optional.of(new GolemSurfaceState(latestStage, allWaxed));
    }

    private static GolemWeatheringStage stageFor(WeatheringCopper copper) {
        return switch (copper.getAge()) {
            case UNAFFECTED -> GolemWeatheringStage.UNAFFECTED;
            case EXPOSED -> GolemWeatheringStage.EXPOSED;
            case WEATHERED -> GolemWeatheringStage.WEATHERED;
            case OXIDIZED -> GolemWeatheringStage.OXIDIZED;
        };
    }
}
