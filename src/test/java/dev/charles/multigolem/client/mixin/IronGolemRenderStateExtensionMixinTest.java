package dev.charles.multigolem.client.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.client.render.GolemRenderStateExtension;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IronGolemRenderStateExtensionMixinTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void invalidRenderIdentityFallsBackToDefaultIron() {
        GolemRenderStateExtension state = new IronGolemRenderStateExtensionMixin();

        state.multigolem$setIdentity(new GolemIdentity(GolemFamily.COPPER_GOLEM, GolemVariant.COPPER, Optional.empty()));

        assertEquals(GolemIdentity.defaultIron(), state.multigolem$getIdentity());
    }
}
