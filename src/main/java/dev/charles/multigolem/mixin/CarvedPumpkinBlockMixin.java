package dev.charles.multigolem.mixin;

import dev.charles.multigolem.spawn.GolemCreationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {

    @Inject(method = "trySpawnGolem(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$trySpawnVariant(Level level, BlockPos pos, CallbackInfo ci) {
        if (GolemCreationHandler.trySpawnVariant(level, pos)) {
            ci.cancel();
        }
    }
}
