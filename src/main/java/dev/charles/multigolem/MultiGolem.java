package dev.charles.multigolem;

import dev.charles.multigolem.ability.AbilityRegistry;
import dev.charles.multigolem.attachment.GolemAbilityStateAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.loot.HasGolemVariantLootCondition;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;

public class MultiGolem implements ModInitializer {
    public static final String MOD_ID = "multigolem";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static volatile MultiGolemConfig CONFIG = MultiGolemConfig.defaults();

    public static MultiGolemConfig config() { return CONFIG; }

    @Override
    public void onInitialize() {
        GolemVariantAttachment.touch();
        GolemAbilityStateAttachment.touch();

        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
        CONFIG = MultiGolemConfig.loadOrCreate(configFile);

        AbilityRegistry.register();

        Registry.register(
            BuiltInRegistries.LOOT_CONDITION_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "has_golem_variant"),
            HasGolemVariantLootCondition.MAP_CODEC
        );

        registerVariantLoot();

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof IronGolem golem) {
                VariantAttributes.apply(golem);
            }
        });

        LOG.info("MultiGolem starting up - config loaded from {}", configFile);
    }

    private static void registerVariantLoot() {
        Optional<ResourceKey<LootTable>> ironGolemTable = EntityType.IRON_GOLEM.getDefaultLootTable();
        if (ironGolemTable.isEmpty()) {
            LOG.warn("Iron golem default loot table key not present; variant drops disabled");
            return;
        }
        ResourceKey<LootTable> targetKey = ironGolemTable.get();

        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (!key.equals(targetKey)) return;
            if (!source.isBuiltin()) return; // don't touch other mods' overrides

            // For each non-IRON variant, add a single-roll pool that drops the variant's matching item,
            // gated on the killed entity carrying that variant attachment.
            // Iron is unchanged (vanilla iron-ingot pool still rolls for IRON variant goems via the existing table).
            addVariantPool(builder, GolemVariant.COPPER,    Items.COPPER_INGOT,    3, 5);
            addVariantPool(builder, GolemVariant.GOLD,      Items.GOLD_INGOT,      3, 5);
            addVariantPool(builder, GolemVariant.EMERALD,   Items.EMERALD,         3, 5);
            addVariantPool(builder, GolemVariant.DIAMOND,   Items.DIAMOND,         3, 5);
            addVariantPool(builder, GolemVariant.NETHERITE, Items.NETHERITE_SCRAP, 2, 3);
        });
    }

    private static void addVariantPool(LootTable.Builder builder, GolemVariant variant, Item drop, int min, int max) {
        builder.withPool(LootPool.lootPool()
            .when(new HasGolemVariantLootCondition(variant))
            .add(LootItem.lootTableItem(drop)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))));
    }
}
