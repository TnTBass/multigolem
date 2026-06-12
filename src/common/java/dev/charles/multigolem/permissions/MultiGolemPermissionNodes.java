package dev.charles.multigolem.permissions;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;

public final class MultiGolemPermissionNodes {
    public static final String ADMIN_BYPASS = "multigolem.admin.bypass";

    private MultiGolemPermissionNodes() {}

    public static String create(GolemVariant variant) {
        return "multigolem.create." + GolemVariantCatalog.require(variant).permissionSuffix();
    }

    public static String heal(GolemVariant variant) {
        return "multigolem.heal." + GolemVariantCatalog.require(variant).permissionSuffix();
    }

    public static boolean defaultAllowed(String node) {
        return !ADMIN_BYPASS.equals(node);
    }
}
