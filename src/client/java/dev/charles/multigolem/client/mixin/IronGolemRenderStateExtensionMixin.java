package dev.charles.multigolem.client.mixin;

import dev.charles.multigolem.client.render.GolemRenderStateExtension;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(IronGolemRenderState.class)
public class IronGolemRenderStateExtensionMixin implements GolemRenderStateExtension {

    @Unique
    private GolemIdentity multigolem$identity = GolemIdentity.defaultIron();

    @Override
    public GolemIdentity multigolem$getIdentity() {
        return multigolem$identity;
    }

    @Override
    public void multigolem$setIdentity(GolemIdentity identity) {
        this.multigolem$identity = identity == null || !identity.isValidForPhase3()
            ? GolemIdentity.defaultIron()
            : identity;
    }
}
