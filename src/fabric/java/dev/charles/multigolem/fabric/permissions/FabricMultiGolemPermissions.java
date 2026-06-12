package dev.charles.multigolem.fabric.permissions;

import dev.charles.multigolem.permissions.MultiGolemPermissions;
import me.lucko.fabric.api.permissions.v0.Permissions;

public final class FabricMultiGolemPermissions {
    private FabricMultiGolemPermissions() {}

    public static void register() {
        MultiGolemPermissions.registerLookup((player, node, defaultValue) ->
            Permissions.check(player, node, defaultValue));
    }
}
