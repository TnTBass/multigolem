package dev.charles.multigolem.client.mixin;

import dev.charles.multigolem.attachment.GolemIdentityAttachment;
import dev.charles.multigolem.client.render.GolemRenderStateExtension;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolemRenderer.class)
public class IronGolemRenderStateMixin {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/animal/golem/IronGolem;Lnet/minecraft/client/renderer/entity/state/IronGolemRenderState;F)V",
        at = @At("TAIL")
    )
    private void multigolem$captureVariant(IronGolem entity, IronGolemRenderState state,
                                           float partialTick, CallbackInfo ci) {
        GolemIdentity identity = GolemIdentityAttachment.get(entity);
        ((GolemRenderStateExtension) state).multigolem$setIdentity(identity);
    }
}
