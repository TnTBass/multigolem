package dev.charles.multigolem.permissions;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemPermissionsTest {
    private record Call(String node, boolean defaultValue) {}

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void createNodes_matchSpecExactly() {
        assertEquals("multigolem.create.copper", MultiGolemPermissions.createNode(GolemVariant.COPPER));
        assertEquals("multigolem.create.gold", MultiGolemPermissions.createNode(GolemVariant.GOLD));
        assertEquals("multigolem.create.emerald", MultiGolemPermissions.createNode(GolemVariant.EMERALD));
        assertEquals("multigolem.create.diamond", MultiGolemPermissions.createNode(GolemVariant.DIAMOND));
        assertEquals("multigolem.create.netherite", MultiGolemPermissions.createNode(GolemVariant.NETHERITE));
    }

    @Test
    void healNodes_matchSpecExactlyIncludingIron() {
        assertEquals("multigolem.heal.copper", MultiGolemPermissions.healNode(GolemVariant.COPPER));
        assertEquals("multigolem.heal.iron", MultiGolemPermissions.healNode(GolemVariant.IRON));
        assertEquals("multigolem.heal.gold", MultiGolemPermissions.healNode(GolemVariant.GOLD));
        assertEquals("multigolem.heal.emerald", MultiGolemPermissions.healNode(GolemVariant.EMERALD));
        assertEquals("multigolem.heal.diamond", MultiGolemPermissions.healNode(GolemVariant.DIAMOND));
        assertEquals("multigolem.heal.netherite", MultiGolemPermissions.healNode(GolemVariant.NETHERITE));
    }

    @Test
    void adminBypass_isCheckedBeforeTierNode() {
        List<Call> calls = new ArrayList<>();
        MultiGolemPermissions.PermissionLookup lookup = (node, defaultValue) -> {
            calls.add(new Call(node, defaultValue));
            return node.equals(MultiGolemPermissions.ADMIN_BYPASS_NODE);
        };

        assertTrue(MultiGolemPermissions.canCreate(GolemVariant.DIAMOND, lookup));
        assertEquals(List.of(new Call("multigolem.admin.bypass", false)), calls);
    }

    @Test
    void missingProviderOrMissingNode_defaultsToAllow() {
        List<Call> calls = new ArrayList<>();
        MultiGolemPermissions.PermissionLookup lookup = (node, defaultValue) -> {
            calls.add(new Call(node, defaultValue));
            return defaultValue;
        };

        assertTrue(MultiGolemPermissions.canCreate(GolemVariant.NETHERITE, lookup));
        assertTrue(MultiGolemPermissions.canHeal(GolemVariant.IRON, lookup));
        assertEquals(false, calls.get(0).defaultValue());
        assertEquals(true, calls.get(1).defaultValue());
        assertEquals(false, calls.get(2).defaultValue());
        assertEquals(true, calls.get(3).defaultValue());
    }

    @Test
    void explicitTierDenial_returnsFalseWhenBypassAbsent() {
        MultiGolemPermissions.PermissionLookup lookup = (node, defaultValue) ->
            node.equals(MultiGolemPermissions.ADMIN_BYPASS_NODE) ? false : !node.equals("multigolem.create.diamond");

        assertFalse(MultiGolemPermissions.canCreate(GolemVariant.DIAMOND, lookup));
    }

    @Test
    void clientSideHealPredictionDoesNotCallPermissionProvider() {
        MultiGolemPermissions.PermissionLookup throwingLookup = (node, defaultValue) -> {
            throw new AssertionError("Client-side prediction must not call Fabric Permissions");
        };

        assertTrue(MultiGolemPermissions.canHealForSidePrediction(false, GolemVariant.NETHERITE, throwingLookup));
    }

    @Test
    void denialMessages_useStableDisplayNames() {
        assertEquals("Iron", GolemVariant.IRON.displayName());
        assertEquals("Emerald", GolemVariant.EMERALD.displayName());
        assertEquals("Diamond", GolemVariant.DIAMOND.displayName());
        assertEquals("Netherite", GolemVariant.NETHERITE.displayName());
        assertEquals("You do not have permission to create a Copper golem.",
            MultiGolemPermissions.createDeniedMessage(GolemVariant.COPPER));
        assertEquals("You do not have permission to heal a Gold golem.",
            MultiGolemPermissions.healDeniedMessage(GolemVariant.GOLD));
        assertEquals("You do not have permission to heal an Iron golem.",
            MultiGolemPermissions.healDeniedMessage(GolemVariant.IRON));
        assertEquals("You do not have permission to heal an Emerald golem.",
            MultiGolemPermissions.healDeniedMessage(GolemVariant.EMERALD));
        assertEquals("You do not have permission to create a Diamond golem.",
            MultiGolemPermissions.createDeniedMessage(GolemVariant.DIAMOND));
        assertEquals("You do not have permission to heal a Netherite golem.",
            MultiGolemPermissions.healDeniedMessage(GolemVariant.NETHERITE));
    }
}
