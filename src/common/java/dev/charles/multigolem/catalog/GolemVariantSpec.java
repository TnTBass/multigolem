package dev.charles.multigolem.catalog;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemFamily;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;
import java.util.function.Supplier;

public record GolemVariantSpec(
    GolemVariant variant,
    GolemFamily family,
    Supplier<Predicate<BlockState>> bodyBlockMatcherSupplier,
    Item healItem,
    Item dropItem,
    int lootMin,
    int lootMax,
    boolean spawnEggEnabled,
    boolean lootEnabled,
    boolean playerBuildable,
    boolean healEnabled,
    boolean permissionEnabled,
    boolean renderable,
    String permissionSuffix,
    String entityTexturePath,
    String spawnEggModelPath,
    String spawnEggTexturePath
) {
    public boolean matchesBodyBlock(BlockState state) {
        return bodyBlockMatcherSupplier.get().test(state);
    }
}
