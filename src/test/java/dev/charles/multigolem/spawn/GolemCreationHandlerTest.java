package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GolemCreationHandlerTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void copperBodyStatesDeriveSurfaceIdentity() {
        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER,
                new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false)),
            GolemCreationHandler.identityForBodyStates(GolemVariant.COPPER, List.of(
                state(Blocks.COPPER_BLOCK),
                state(Blocks.WEATHERED_COPPER),
                state(Blocks.OXIDIZED_COPPER),
                state(Blocks.EXPOSED_COPPER)
            )));
    }

    @Test
    void nonCopperBodyStatesKeepSurfaceEmpty() {
        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.GOLD),
            GolemCreationHandler.identityForBodyStates(GolemVariant.GOLD, List.of(
                state(Blocks.GOLD_BLOCK),
                state(Blocks.GOLD_BLOCK),
                state(Blocks.GOLD_BLOCK),
                state(Blocks.GOLD_BLOCK)
            )));
    }

    @Test
    void copperMatchBodyStatesSampleLowerBodyBlockNotPumpkin() {
        BlockPattern.BlockPatternMatch match = matchWithStates(Map.of(
            new BlockPos(1, 2, 0), state(Blocks.CARVED_PUMPKIN),
            new BlockPos(0, 1, 0), state(Blocks.OXIDIZED_COPPER),
            new BlockPos(1, 1, 0), state(Blocks.OXIDIZED_COPPER),
            new BlockPos(2, 1, 0), state(Blocks.OXIDIZED_COPPER),
            new BlockPos(1, 0, 0), state(Blocks.OXIDIZED_COPPER)
        ));

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER,
                new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false)),
            GolemCreationHandler.identityFromMatchBodyStates(GolemVariant.COPPER, match));
    }

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }

    private static BlockPattern.BlockPatternMatch matchWithStates(Map<BlockPos, BlockState> states) {
        LoadingCache<BlockPos, BlockInWorld> cache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
            @Override
            public BlockInWorld load(BlockPos pos) throws Exception {
                BlockInWorld block = new BlockInWorld(null, pos, false);
                Field state = BlockInWorld.class.getDeclaredField("state");
                state.setAccessible(true);
                state.set(block, states.getOrDefault(pos, Blocks.AIR.defaultBlockState()));
                return block;
            }
        });
        return new BlockPattern.BlockPatternMatch(new BlockPos(0, 2, 0), Direction.NORTH, Direction.UP, cache, 3, 3, 1);
    }
}
