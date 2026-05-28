package dev.charles.multigolem.mixin;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.TargetFilter;
import dev.charles.multigolem.ability.ZombieGolemFaction;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class GolemTargetingMixin {

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$preventZombieFamilyAttackingZombieGolems(
        LivingEntity target,
        CallbackInfoReturnable<Boolean> cir
    ) {
        Mob self = (Mob) (Object) this;
        if (target instanceof IronGolem targetGolem
                && !ZombieGolemFaction.zombieFamilyCanTargetGolem(self.getClass(), GolemVariantAttachment.get(targetGolem))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setTarget(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$filterExcludedTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (target == null) return;
        if (target instanceof IronGolem targetGolem
                && !ZombieGolemFaction.zombieFamilyCanTargetGolem(self.getClass(), GolemVariantAttachment.get(targetGolem))) {
            ci.cancel();
            return;
        }
        if (!(self instanceof IronGolem golem)) return;
        if (ZombieGolemFaction.shouldCancelTarget(golem, target)) {
            ci.cancel();
            return;
        }
        var variant = GolemVariantAttachment.get(golem);
        var filter = TargetFilter.fromIgnoredList(MultiGolem.config().tier(variant).ignoredTargetTypes());
        if (filter.isExcluded(target)) {
            ci.cancel();
        }
    }
}
