package dev.charles.multigolem.permissions;

import dev.charles.multigolem.GolemVariant;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class MultiGolemPermissions {
    public static final String ADMIN_BYPASS_NODE = "multigolem.admin.bypass";

    // Package-private for unit testing; production callers use the public static methods.
    @FunctionalInterface
    interface PermissionLookup {
        boolean check(String node, boolean defaultValue);
    }

    private MultiGolemPermissions() {}

    public static boolean canCreate(ServerPlayer player, GolemVariant variant) {
        return canCreate(variant, (node, defaultValue) -> Permissions.check(player, node, defaultValue));
    }

    public static boolean canHeal(Player player, GolemVariant variant) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return canHealForSidePrediction(false, variant, (node, defaultValue) -> true);
        }
        return canHealForSidePrediction(true, variant,
            (node, defaultValue) -> Permissions.check(serverPlayer, node, defaultValue));
    }

    static boolean canCreate(GolemVariant variant, PermissionLookup lookup) {
        return checkWithBypass(createNode(variant), lookup);
    }

    static boolean canHeal(GolemVariant variant, PermissionLookup lookup) {
        return checkWithBypass(healNode(variant), lookup);
    }

    static boolean canHealForSidePrediction(boolean serverSide, GolemVariant variant, PermissionLookup lookup) {
        if (!serverSide) {
            return true;
        }
        return canHeal(variant, lookup);
    }

    private static boolean checkWithBypass(String node, PermissionLookup lookup) {
        if (lookup.check(ADMIN_BYPASS_NODE, false)) {
            return true;
        }
        return lookup.check(node, true);
    }

    static String createNode(GolemVariant variant) {
        return "multigolem.create." + variant.id();
    }

    static String healNode(GolemVariant variant) {
        return "multigolem.heal." + variant.id();
    }

    public static void sendCreateDenied(ServerPlayer player, GolemVariant variant) {
        player.sendOverlayMessage(Component.literal(createDeniedMessage(variant)));
    }

    public static void sendHealDenied(Player player, GolemVariant variant) {
        player.sendOverlayMessage(Component.literal(healDeniedMessage(variant)));
    }

    static String createDeniedMessage(GolemVariant variant) {
        return "You do not have permission to create " + article(variant) + " " + variant.displayName() + " golem.";
    }

    static String healDeniedMessage(GolemVariant variant) {
        return "You do not have permission to heal " + article(variant) + " " + variant.displayName() + " golem.";
    }

    private static String article(GolemVariant variant) {
        String displayName = variant.displayName();
        if (displayName.startsWith("Iron") || displayName.startsWith("Emerald")) {
            return "an";
        }
        return "a";
    }
}
