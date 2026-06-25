package dev.charles.multigolem.golempedia;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.spawn.VillageSpawnWeights;
import net.minecraft.world.item.Item;

import java.util.List;

public final class GolempediaCatalog {
    private GolempediaCatalog() {
    }

    public static List<GolempediaEntry> entries() {
        MultiGolemConfig defaults = MultiGolemConfig.defaults();
        return GolemVariantCatalog.entries().stream()
            .filter(spec -> spec.variant() != GolemVariant.IRON)
            .map(spec -> entryFor(spec, defaults))
            .toList();
    }

    private static GolempediaEntry entryFor(GolemVariantSpec spec, MultiGolemConfig defaults) {
        GolemVariant variant = spec.variant();
        return new GolempediaEntry(
            variant,
            variant.displayName(),
            creationFor(variant),
            itemName(spec.healItem()),
            dropSummary(spec),
            GolempediaStats.linesFor(variant, defaults.tier(variant)),
            spec.spawnEggEnabled() ? "Spawn egg available in creative tabs." : "No spawn egg in this build.",
            villageSpawnSummary(variant, defaults),
            abilityFor(variant),
            caveatsFor(variant)
        );
    }

    private static String creationFor(GolemVariant variant) {
        return switch (variant) {
            case COPPER -> "Build with a copper-family body block and carved pumpkin head.";
            case REDSTONE -> "Build with a redstone block body and carved pumpkin head.";
            case GOLD -> "Build with a gold block body and carved pumpkin head.";
            case EMERALD -> "Build with an emerald block body and carved pumpkin head.";
            case DIAMOND -> "Build with a diamond block body and carved pumpkin head.";
            case NETHERITE -> "Build with a netherite block body and carved pumpkin head.";
            case ZOMBIE -> "Build with a mossy cobblestone body and carved pumpkin head.";
            case IRON -> throw new IllegalArgumentException("Iron is not a Golempedia entry");
        };
    }

    private static String dropSummary(GolemVariantSpec spec) {
        if (!spec.lootEnabled()) {
            return "Uses vanilla iron golem drops.";
        }
        return itemName(spec.dropItem()) + " x" + spec.lootMin() + "-" + spec.lootMax();
    }

    private static String itemName(Item item) {
        String id = item.getDescriptionId();
        String key = id.substring(id.lastIndexOf('.') + 1);
        String[] words = key.split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    private static String villageSpawnSummary(GolemVariant variant, MultiGolemConfig defaults) {
        return GolempediaVillageSpawns.summary(
            variant,
            defaults.villageSpawnWeights().enabled(),
            defaults.villageSpawnWeights().weights(),
            defaults.zombieVillageSpawning().enabled()
        );
    }

    private static String abilityFor(GolemVariant variant) {
        return switch (variant) {
            case COPPER -> "Lightning does not hurt Copper golems; it heals them instead.";
            case REDSTONE -> "Redstone golems overcharge near death for attack and resistance without speed, then release a Slowness X pulse.";
            case GOLD -> "Gold golems move faster and can show sprint and sunlight shine behavior.";
            case EMERALD -> "Emerald golems heal themselves when villagers are nearby.";
            case DIAMOND -> "Diamond golems call lightning onto nearby hostile mobs after a cooldown.";
            case NETHERITE -> "Netherite golems are fireproof and can ignite nearby attackers.";
            case ZOMBIE -> "Zombie golems attack players, villagers, wandering traders, and non-zombie golems. "
                + "When they hit villagers or wandering traders, they can turn them into zombie villagers.";
            case IRON -> throw new IllegalArgumentException("Iron is not a Golempedia entry");
        };
    }

    private static List<String> caveatsFor(GolemVariant variant) {
        return switch (variant) {
            case COPPER -> List.of("Server settings may change lightning healing amounts or target rules.");
            case REDSTONE -> List.of("Server settings may change overcharge timing, resistance, attack, pulse radius, or Slowness duration.");
            case GOLD -> List.of("Server settings may change speed, sprint particles, or sunlight shine behavior.");
            case EMERALD -> List.of("Server settings may change aura range, interval, healing amount, or which villagers count.");
            case DIAMOND -> List.of("Server settings may change target mode, aura range, cooldowns, or lightning protection.");
            case NETHERITE -> List.of("Netherite golems can be dangerous near villages when fire ignition is enabled.");
            case ZOMBIE -> List.of("Zombie village spawning and conversion effects are server-configurable.");
            case IRON -> throw new IllegalArgumentException("Iron is not a Golempedia entry");
        };
    }
}
