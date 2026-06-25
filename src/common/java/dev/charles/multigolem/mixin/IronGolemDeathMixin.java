package dev.charles.multigolem.mixin;

import dev.charles.multigolem.ability.RedstoneAbility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolem.class)
public abstract class IronGolemDeathMixin {

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void multigolem$redstoneDeathPulse(DamageSource source, CallbackInfo ci) {
        IronGolem self = (IronGolem) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;

        RedstoneAbility.emitDeathPulse(level, self);
    }
}
