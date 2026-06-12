package dev.charles.multigolem.permissions;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricPermissionsBoundarySourceTest {
    @Test
    void commonPermissionsDoNotImportFabricPermissionsApi() throws IOException {
        String source = Files.readString(Path.of("src/common/java/dev/charles/multigolem/permissions/MultiGolemPermissions.java"));

        assertFalse(source.contains("me.lucko.fabric"));
        assertFalse(source.contains("Permissions.check"));
    }

    @Test
    void fabricAdapterOwnsFabricPermissionsApiLookup() throws IOException {
        String source = Files.readString(Path.of("src/fabric/java/dev/charles/multigolem/fabric/permissions/FabricMultiGolemPermissions.java"));

        assertTrue(source.contains("me.lucko.fabric.api.permissions.v0.Permissions"));
        assertTrue(source.contains("Permissions.check(player, node, defaultValue)"));
        assertTrue(source.contains("MultiGolemPermissions.registerLookup"));
    }
}
