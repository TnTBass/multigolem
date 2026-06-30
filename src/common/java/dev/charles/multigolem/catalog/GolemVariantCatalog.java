package dev.charles.multigolem.catalog;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.GolemAvailability;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class GolemVariantCatalog {
    private static final EnumMap<GolemVariant, GolemVariantSpec> SPECS = buildSpecs();

    private GolemVariantCatalog() {}

    public static Collection<GolemVariantSpec> entries() {
        return SPECS.values();
    }

    public static EnumSet<GolemVariant> variants() {
        return EnumSet.copyOf(SPECS.keySet());
    }

    public static GolemVariantSpec require(GolemVariant variant) {
        GolemVariantSpec spec = SPECS.get(variant);
        if (spec == null) {
            throw new IllegalArgumentException("No catalog entry for golem variant " + variant);
        }
        return spec;
    }

    public static Optional<GolemVariantSpec> forBodyBlock(BlockState state) {
        return entries().stream()
            .filter(spec -> spec.matchesBodyBlock(state))
            .findFirst();
    }

    public static Optional<GolemVariantSpec> forHealItem(net.minecraft.world.item.Item item) {
        return entries().stream()
            .filter(spec -> spec.healItem() == item)
            .findFirst();
    }

    public static boolean contains(GolemFamily family, GolemVariant variant) {
        GolemVariantSpec spec = SPECS.get(variant);
        return spec != null && spec.family() == family;
    }

    public static Optional<GolemVariantSpec> find(GolemFamily family, GolemVariant variant) {
        GolemVariantSpec spec = SPECS.get(variant);
        if (spec == null || spec.family() != family) return Optional.empty();
        return Optional.of(spec);
    }

    public static List<GolemIdentity> identitiesWhereAvailable(GolemAvailability availability) {
        return entries().stream()
            .map(GolemVariantSpec::identity)
            .filter(availability::isAvailable)
            .toList();
    }

    public static List<GolemVariant> variantsWhere(Predicate<GolemVariantSpec> predicate) {
        return entries().stream()
            .filter(predicate)
            .map(GolemVariantSpec::variant)
            .toList();
    }

    private static EnumMap<GolemVariant, GolemVariantSpec> buildSpecs() {
        EnumMap<GolemVariant, GolemVariantSpec> specs = new EnumMap<>(GolemVariant.class);
        add(specs, GolemVariant.COPPER, 3, 5, true, true, true, true);
        add(specs, GolemVariant.IRON, 0, 0, false, false, false, false);
        add(specs, GolemVariant.REDSTONE, 3, 5, true, true, true, true);
        add(specs, GolemVariant.GOLD, 3, 5, true, true, true, true);
        add(specs, GolemVariant.LAPIS, 3, 5, true, true, true, true);
        add(specs, GolemVariant.EMERALD, 3, 5, true, true, true, true);
        add(specs, GolemVariant.DIAMOND, 3, 5, true, true, true, true);
        add(specs, GolemVariant.NETHERITE, 2, 3, true, true, true, true);
        add(specs, GolemVariant.ZOMBIE, 3, 5, true, true, true, true);
        return specs;
    }

    private static void add(
        Map<GolemVariant, GolemVariantSpec> specs,
        GolemVariant variant,
        int lootMin,
        int lootMax,
        boolean spawnEggEnabled,
        boolean lootEnabled,
        boolean playerBuildable,
        boolean renderable
    ) {
        specs.put(variant, new GolemVariantSpec(
            variant,
            GolemFamily.IRON_GOLEM,
            () -> variant::matchesBodyBlock,
            variant.healIngot(),
            dropItemFor(variant),
            lootMin,
            lootMax,
            spawnEggEnabled,
            lootEnabled,
            playerBuildable,
            true,
            true,
            renderable,
            variant.id(),
            renderable ? variant.id() + "_golem.png" : "",
            spawnEggEnabled ? variant.id() + "_golem_spawn_egg.json" : "",
            spawnEggEnabled ? variant.id() + "_golem_spawn_egg.png" : ""
        ));
    }

    private static net.minecraft.world.item.Item dropItemFor(GolemVariant variant) {
        return switch (variant) {
            case COPPER -> Items.COPPER_INGOT;
            case IRON -> Items.IRON_INGOT;
            case REDSTONE -> Items.REDSTONE;
            case GOLD -> Items.GOLD_INGOT;
            case LAPIS -> Items.LAPIS_LAZULI;
            case EMERALD -> Items.EMERALD;
            case DIAMOND -> Items.DIAMOND;
            case NETHERITE -> Items.NETHERITE_SCRAP;
            case ZOMBIE -> Items.ROTTEN_FLESH;
        };
    }
}
