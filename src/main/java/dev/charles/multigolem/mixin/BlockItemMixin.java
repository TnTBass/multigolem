package dev.charles.multigolem.mixin;

import dev.charles.multigolem.permissions.PumpkinPlacementTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Shadow
    protected abstract boolean placeBlock(BlockPlaceContext context, BlockState placementState);

    @Redirect(
        method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        )
    )
    private boolean multigolem$trackPumpkinPlacement(BlockItem instance, BlockPlaceContext context, BlockState placementState) {
        boolean isPumpkinHead = placementState.is(Blocks.CARVED_PUMPKIN) || placementState.is(Blocks.JACK_O_LANTERN);
        if (isPumpkinHead && context.getLevel() instanceof ServerLevel && context.getPlayer() instanceof ServerPlayer player) {
            final boolean[] placed = new boolean[1];
            PumpkinPlacementTracker.withCurrentPlacement(player, context.getClickedPos(), () ->
                placed[0] = this.placeBlock(context, placementState));
            return placed[0];
        }
        return this.placeBlock(context, placementState);
    }
}
