package dev.charles.multigolem.neoforge.event;

import com.mojang.serialization.MapCodec;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.loot.HasGolemVariantLootCondition;
import dev.charles.multigolem.spawn.SpawnEggStacks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

public final class NeoForgeMultiGolemEvents {
    private static final DeferredRegister<MapCodec<? extends LootItemCondition>> LOOT_CONDITION_TYPES =
        DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, MultiGolem.MOD_ID);

    @SuppressWarnings("unused")
    private static final Supplier<MapCodec<? extends LootItemCondition>> HAS_GOLEM_VARIANT =
        LOOT_CONDITION_TYPES.register("has_golem_variant", () -> HasGolemVariantLootCondition.MAP_CODEC);

    private NeoForgeMultiGolemEvents() {}

    public static void register(IEventBus modBus) {
        LOOT_CONDITION_TYPES.register(modBus);
        modBus.addListener(NeoForgeMultiGolemEvents::buildCreativeTabs);
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemEvents::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(NeoForgeMultiGolemEvents::onLootTableLoad);
    }

    private static void buildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            return;
        }

        for (GolemVariant variant : MultiGolem.creativeSpawnEggVariants()) {
            event.accept(SpawnEggStacks.create(variant));
        }
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof IronGolem golem && !event.getLevel().isClientSide()) {
            VariantAttributes.apply(golem);
        }
    }

    private static void onLootTableLoad(LootTableLoadEvent event) {
        Optional<ResourceKey<LootTable>> ironGolemTable = EntityTypes.IRON_GOLEM.getDefaultLootTable();
        if (ironGolemTable.isEmpty()) {
            MultiGolem.LOG.warn("Iron golem default loot table key not present; variant drops disabled");
            return;
        }
        if (!event.getKey().equals(ironGolemTable.get())) {
            return;
        }

        LootTable table = event.getTable();
        for (GolemVariant variant : GolemVariant.lootVariants()) {
            MultiGolem.VariantLootDrop drop = MultiGolem.lootDropFor(variant);
            table.addPool(variantPool(variant, drop.item(), drop.min(), drop.max()));
        }
    }

    private static LootPool variantPool(GolemVariant variant, Item drop, int min, int max) {
        return LootPool.lootPool()
            .when(HasGolemVariantLootCondition.builder(variant))
            .add(LootItem.lootTableItem(drop)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max))))
            .build();
    }
}
