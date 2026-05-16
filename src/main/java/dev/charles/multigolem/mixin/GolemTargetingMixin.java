package dev.charles.multigolem.mixin;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.TargetFilter;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class GolemTargetingMixin {

    @Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$filterExcludedTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!(self instanceof IronGolem golem)) return;
        if (target == null) return;
        var variant = GolemVariantAttachment.get(golem);
        var filter = TargetFilter.fromIgnoredList(MultiGolem.config().tier(variant).ignoredTargetTypes());
        if (filter.isExcluded(target)) {
            ci.cancel();
        }
    }
}
