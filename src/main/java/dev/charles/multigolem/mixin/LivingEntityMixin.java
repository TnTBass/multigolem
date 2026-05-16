package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// setLastHurtByMob is declared on LivingEntity, not IronGolem. Mixin @Inject does not walk
// the class hierarchy, so we mix into LivingEntity and filter to IronGolem at runtime.
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "setLastHurtByMob(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$suppressPlayerAnger(LivingEntity hurtBy, CallbackInfo ci) {
        if (!(hurtBy instanceof Player)) return;
        if (!(((Object) this) instanceof IronGolem self)) return;
        GolemVariant variant = GolemVariantAttachment.get(self);
        if (!MultiGolem.config().tier(variant).angerOnHit()) {
            ci.cancel();
        }
    }
}
