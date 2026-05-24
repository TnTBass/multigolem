package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.NetheriteAbility;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "fireImmune()Z", at = @At("HEAD"), cancellable = true)
    private void multigolem$netheriteGolemsAreFireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (!(((Object) this) instanceof IronGolem golem)) return;

        GolemVariant variant = GolemVariantAttachment.get(golem);
        boolean enabled = MultiGolem.config().tier(GolemVariant.NETHERITE).netheriteFireImmune();
        if (NetheriteAbility.isFireImmuneVariant(variant, enabled)) {
            cir.setReturnValue(true);
        }
    }
}
