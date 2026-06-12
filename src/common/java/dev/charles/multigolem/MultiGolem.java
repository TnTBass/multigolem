package dev.charles.multigolem;

import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.loot.HasGolemVariantLootCondition;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.nio.file.Path;

public final class MultiGolem {
    public static final String MOD_ID = "multigolem";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static volatile MultiGolemConfig CONFIG = MultiGolemConfig.defaults();

    private MultiGolem() {}

    public static MultiGolemConfig config() { return CONFIG; }

    public record VariantLootDrop(Item item, int min, int max) {}

    public static void initialize(Path configFile) {
        CONFIG = MultiGolemConfig.loadOrCreate(configFile);
        LOG.info("MultiGolem starting up - config loaded from {}", configFile);
    }

    public static List<GolemVariant> creativeSpawnEggVariants() {
        return GolemVariant.spawnEggVariants();
    }

    public static List<GolemVariant> lootVariants() {
        return GolemVariant.lootVariants();
    }

    public static VariantLootDrop lootDropFor(GolemVariant variant) {
        GolemVariantSpec spec = GolemVariantCatalog.require(variant);
        if (!spec.lootEnabled()) {
            throw new IllegalArgumentException("Iron uses vanilla iron golem loot");
        }
        return new VariantLootDrop(spec.dropItem(), spec.lootMin(), spec.lootMax());
    }

    public static void addVariantPool(LootTable.Builder builder, GolemVariant variant, Item drop, int min, int max) {
        builder.withPool(LootPool.lootPool()
            .when(HasGolemVariantLootCondition.builder(variant))
            .add(LootItem.lootTableItem(drop)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))));
    }
}
