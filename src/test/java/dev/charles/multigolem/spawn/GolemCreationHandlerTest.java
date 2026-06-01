package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }
}
