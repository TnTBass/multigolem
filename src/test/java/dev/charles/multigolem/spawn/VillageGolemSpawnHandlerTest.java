package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VillageGolemSpawnHandlerTest {

    @BeforeAll
    static void bootstrap() {
        System.setProperty("net.bytebuddy.experimental", "true");
        MinecraftBootstrap.ensure();
    }

    @Test
    void applyVariantMarksVillageOriginForNonIronVariant() {
        IronGolem golem = mock(IronGolem.class);
        CapturingApplier applier = new CapturingApplier();
        when(golem.getMaxHealth()).thenReturn(100.0F);

        VillageGolemSpawnHandler.applyVariant(golem, GolemVariant.NETHERITE, applier);

        assertSame(golem, applier.golem);
        assertEquals(GolemVariant.NETHERITE, applier.variant);
        assertEquals(GolemSpawnOrigin.VILLAGE, applier.origin);
        verify(golem).setHealth(100.0F);
    }

    @Test
    void applyVariantDoesNotMarkIronOrigin() {
        IronGolem golem = mock(IronGolem.class);
        CapturingApplier applier = new CapturingApplier();

        VillageGolemSpawnHandler.applyVariant(golem, GolemVariant.IRON, applier);

        assertEquals(0, applier.calls);
        verifyNoInteractions(golem);
    }

    @Test
    void villageCopperRollIdentityHasEmptySurfaceState() {
        IronGolem golem = mock(IronGolem.class);
        CapturingApplier applier = new CapturingApplier();
        when(golem.getMaxHealth()).thenReturn(100.0F);

        VillageGolemSpawnHandler.applyVariant(golem, GolemVariant.COPPER, applier);

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER),
            GolemIdentity.ofIronVariant(applier.variant));
    }

    private static final class CapturingApplier implements VillageGolemSpawnHandler.VillageVariantApplier {
        private int calls;
        private IronGolem golem;
        private GolemVariant variant;
        private GolemSpawnOrigin origin;

        @Override
        public void apply(IronGolem golem, GolemVariant variant, GolemSpawnOrigin origin) {
            this.calls++;
            this.golem = golem;
            this.variant = variant;
            this.origin = origin;
        }
    }
}
