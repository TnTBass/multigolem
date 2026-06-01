package dev.charles.multigolem.client.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.client.render.GolemRenderStateExtension;
import dev.charles.multigolem.client.render.GolemTextureSelector;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolemRenderer.class)
public class IronGolemRendererMixin {

    @Inject(
        method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/IronGolemRenderState;)Lnet/minecraft/resources/Identifier;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void multigolem$variantTexture(IronGolemRenderState state,
                                           CallbackInfoReturnable<Identifier> cir) {
        GolemIdentity identity = ((GolemRenderStateExtension) state).multigolem$getIdentity();
        if (identity.variant() != GolemVariant.IRON) {
            cir.setReturnValue(GolemTextureSelector.get(identity));
        }
    }
}
