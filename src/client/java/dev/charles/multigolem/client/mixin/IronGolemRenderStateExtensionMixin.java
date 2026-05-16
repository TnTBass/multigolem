package dev.charles.multigolem.client.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.client.render.GolemRenderStateExtension;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(IronGolemRenderState.class)
public class IronGolemRenderStateExtensionMixin implements GolemRenderStateExtension {

    @Unique
    private GolemVariant multigolem$variant = GolemVariant.IRON;

    @Override
    public GolemVariant multigolem$getVariant() {
        return multigolem$variant;
    }

    @Override
    public void multigolem$setVariant(GolemVariant variant) {
        this.multigolem$variant = variant;
    }
}
