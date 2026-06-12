package dev.charles.multigolem.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgePermissionsSourceTest {
    private static final String[] VARIANTS = {
        "COPPER",
        "IRON",
        "GOLD",
        "EMERALD",
        "DIAMOND",
        "NETHERITE",
        "ZOMBIE"
    };

    @Test
    void neoforgePermissionAdapterOwnsNeoForgePermissionApi() throws IOException {
        String source = Files.readString(Path.of(
            "src/neoforge/java/dev/charles/multigolem/neoforge/permissions/NeoForgeMultiGolemPermissions.java"));

        assertTrue(source.contains("net.neoforged.neoforge.server.permission.PermissionAPI"));
        assertTrue(source.contains("net.neoforged.neoforge.server.permission.events.PermissionGatherEvent"));
        assertTrue(source.contains("net.neoforged.neoforge.server.permission.nodes.PermissionNode"));
        assertTrue(source.contains("net.neoforged.neoforge.server.permission.nodes.PermissionTypes"));
        assertTrue(source.contains("event.addNodes(NODES);"));
        assertTrue(source.contains("PermissionAPI.getPermission(player, permissionNode)"));
        assertTrue(source.contains("MultiGolemPermissions.registerLookup"));
        assertFalse(source.contains("net.fabricmc"));
        assertFalse(source.contains("me.lucko.fabric"));
    }

    @Test
    void neoforgePermissionAdapterDefinesEveryCommonNodeWithCommonDefaults() throws IOException {
        String source = Files.readString(Path.of(
            "src/neoforge/java/dev/charles/multigolem/neoforge/permissions/NeoForgeMultiGolemPermissions.java"));

        assertTrue(source.contains("MultiGolemPermissionNodes.ADMIN_BYPASS"));
        assertTrue(source.contains("MultiGolemPermissionNodes.defaultAllowed(nodeName)"));
        assertTrue(source.contains("Map<String, PermissionNode<Boolean>>"));

        for (String variant : VARIANTS) {
            assertTrue(source.contains("MultiGolemPermissionNodes.create(GolemVariant." + variant + ")"));
            assertTrue(source.contains("MultiGolemPermissionNodes.heal(GolemVariant." + variant + ")"));
        }
    }

    @Test
    void neoforgeEntrypointRegistersPermissionsBeforeGameplayAdapters() throws IOException {
        String source = Files.readString(Path.of(
            "src/neoforge/java/dev/charles/multigolem/neoforge/MultiGolemNeoForge.java"));

        int permissions = source.indexOf("NeoForgeMultiGolemPermissions.register();");
        int events = source.indexOf("NeoForgeMultiGolemEvents.register(modBus);");
        int abilities = source.indexOf("NeoForgeAbilityEvents.register();");

        assertTrue(permissions >= 0);
        assertTrue(events > permissions);
        assertTrue(abilities > permissions);
    }
}
