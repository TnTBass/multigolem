package dev.charles.multigolem.neoforge.permissions;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.permissions.MultiGolemPermissionNodes;
import dev.charles.multigolem.permissions.MultiGolemPermissions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NeoForgeMultiGolemPermissions {
    private static final Map<String, PermissionNode<Boolean>> NODE_MAP = createNodes();
    private static final List<PermissionNode<?>> NODES = List.copyOf(NODE_MAP.values());
    private static volatile boolean registered;

    private NeoForgeMultiGolemPermissions() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemPermissions::registerNodes);
        MultiGolemPermissions.registerLookup((player, node, defaultValue) -> {
            PermissionNode<Boolean> permissionNode = nodeFor(node);
            return permissionNode == null ? defaultValue : PermissionAPI.getPermission(player, permissionNode);
        });
    }

    private static void registerNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(NODES);
    }

    private static PermissionNode<Boolean> nodeFor(String node) {
        return NODE_MAP.get(node);
    }

    private static Map<String, PermissionNode<Boolean>> createNodes() {
        Map<String, PermissionNode<Boolean>> nodes = new LinkedHashMap<>();
        add(nodes, MultiGolemPermissionNodes.ADMIN_BYPASS);
        add(nodes, MultiGolemPermissionNodes.create(GolemVariant.COPPER));
        add(nodes, MultiGolemPermissionNodes.heal(GolemVariant.COPPER));
        add(nodes, MultiGolemPermissionNodes.create(GolemVariant.IRON));
        add(nodes, MultiGolemPermissionNodes.heal(GolemVariant.IRON));
        add(nodes, MultiGolemPermissionNodes.create(GolemVariant.GOLD));
        add(nodes, MultiGolemPermissionNodes.heal(GolemVariant.GOLD));
        add(nodes, MultiGolemPermissionNodes.create(GolemVariant.EMERALD));
        add(nodes, MultiGolemPermissionNodes.heal(GolemVariant.EMERALD));
        add(nodes, MultiGolemPermissionNodes.create(GolemVariant.DIAMOND));
        add(nodes, MultiGolemPermissionNodes.heal(GolemVariant.DIAMOND));
        add(nodes, MultiGolemPermissionNodes.create(GolemVariant.NETHERITE));
        add(nodes, MultiGolemPermissionNodes.heal(GolemVariant.NETHERITE));
        add(nodes, MultiGolemPermissionNodes.create(GolemVariant.ZOMBIE));
        add(nodes, MultiGolemPermissionNodes.heal(GolemVariant.ZOMBIE));
        return Map.copyOf(nodes);
    }

    private static void add(Map<String, PermissionNode<Boolean>> nodes, String nodeName) {
        nodes.put(nodeName, new PermissionNode<>(
            MultiGolem.MOD_ID,
            nodeName.substring((MultiGolem.MOD_ID + ".").length()),
            PermissionTypes.BOOLEAN,
            (player, playerUUID, context) -> MultiGolemPermissionNodes.defaultAllowed(nodeName)
        ));
    }
}
