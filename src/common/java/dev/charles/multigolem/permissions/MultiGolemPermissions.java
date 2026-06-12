package dev.charles.multigolem.permissions;

import dev.charles.multigolem.GolemVariant;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;

public final class MultiGolemPermissions {
    public static final String ADMIN_BYPASS_NODE = MultiGolemPermissionNodes.ADMIN_BYPASS;

    @FunctionalInterface
    public interface PermissionLookup {
        boolean check(ServerPlayer player, String node, boolean defaultValue);
    }

    private static volatile PermissionLookup lookup = (player, node, defaultValue) -> defaultValue;

    private MultiGolemPermissions() {}

    public static void registerLookup(PermissionLookup permissionLookup) {
        lookup = Objects.requireNonNull(permissionLookup, "permissionLookup");
    }

    public static boolean canCreate(ServerPlayer player, GolemVariant variant) {
        return canCreate(player, variant, lookup);
    }

    public static boolean canHeal(Player player, GolemVariant variant) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return canHealForSidePrediction(false, null, variant, lookup);
        }
        return canHealForSidePrediction(true, serverPlayer, variant, lookup);
    }

    static boolean canCreate(GolemVariant variant, PermissionLookup lookup) {
        return canCreate(null, variant, lookup);
    }

    static boolean canCreate(ServerPlayer player, GolemVariant variant, PermissionLookup lookup) {
        return checkWithBypass(player, createNode(variant), lookup);
    }

    static boolean canHeal(GolemVariant variant, PermissionLookup lookup) {
        return canHeal(null, variant, lookup);
    }

    static boolean canHeal(ServerPlayer player, GolemVariant variant, PermissionLookup lookup) {
        return checkWithBypass(player, healNode(variant), lookup);
    }

    static boolean canHealForSidePrediction(boolean serverSide, GolemVariant variant, PermissionLookup lookup) {
        return canHealForSidePrediction(serverSide, null, variant, lookup);
    }

    static boolean canHealForSidePrediction(boolean serverSide, ServerPlayer player, GolemVariant variant, PermissionLookup lookup) {
        if (!serverSide) {
            return true;
        }
        return canHeal(player, variant, lookup);
    }

    private static boolean checkWithBypass(ServerPlayer player, String node, PermissionLookup lookup) {
        if (lookup.check(player, ADMIN_BYPASS_NODE, MultiGolemPermissionNodes.defaultAllowed(ADMIN_BYPASS_NODE))) {
            return true;
        }
        return lookup.check(player, node, MultiGolemPermissionNodes.defaultAllowed(node));
    }

    static String createNode(GolemVariant variant) {
        return MultiGolemPermissionNodes.create(variant);
    }

    static String healNode(GolemVariant variant) {
        return MultiGolemPermissionNodes.heal(variant);
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
