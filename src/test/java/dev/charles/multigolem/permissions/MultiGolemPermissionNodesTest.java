package dev.charles.multigolem.permissions;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiGolemPermissionNodesTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void nodeNamesAndDefaultsAreCommonOwned() {
        assertEquals("multigolem.admin.bypass", MultiGolemPermissionNodes.ADMIN_BYPASS);
        assertEquals("multigolem.create.diamond", MultiGolemPermissionNodes.create(GolemVariant.DIAMOND));
        assertEquals("multigolem.heal.iron", MultiGolemPermissionNodes.heal(GolemVariant.IRON));

        assertFalse(MultiGolemPermissionNodes.defaultAllowed(MultiGolemPermissionNodes.ADMIN_BYPASS));
        assertTrue(MultiGolemPermissionNodes.defaultAllowed(MultiGolemPermissionNodes.create(GolemVariant.NETHERITE)));
        assertTrue(MultiGolemPermissionNodes.defaultAllowed(MultiGolemPermissionNodes.heal(GolemVariant.ZOMBIE)));
    }
}
