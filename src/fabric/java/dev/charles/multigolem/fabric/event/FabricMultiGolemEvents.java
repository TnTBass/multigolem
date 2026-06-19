package dev.charles.multigolem.fabric.event;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.fabric.ability.FabricAbilityEvents;
import dev.charles.multigolem.loot.HasGolemVariantLootCondition;
import dev.charles.multigolem.spawn.SpawnEggStacks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Optional;

public final class FabricMultiGolemEvents {
    private FabricMultiGolemEvents() {}

    public static void register() {
        FabricAbilityEvents.register();

        Registry.register(
            BuiltInRegistries.LOOT_CONDITION_TYPE,
            Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "has_golem_variant"),
            HasGolemVariantLootCondition.MAP_CODEC
        );

        registerVariantLoot();
        registerCreativeSpawnEggs();

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof IronGolem golem) {
                VariantAttributes.apply(golem);
            }
        });
    }

    private static void registerCreativeSpawnEggs() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            for (GolemVariant variant : MultiGolem.creativeSpawnEggVariants()) {
                entries.accept(SpawnEggStacks.create(variant));
            }
        });
    }

    private static void registerVariantLoot() {
        Optional<ResourceKey<LootTable>> ironGolemTable = EntityTypes.IRON_GOLEM.getDefaultLootTable();
        if (ironGolemTable.isEmpty()) {
            MultiGolem.LOG.warn("Iron golem default loot table key not present; variant drops disabled");
            return;
        }
        ResourceKey<LootTable> targetKey = ironGolemTable.get();

        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (!key.equals(targetKey)) return;
            if (!source.isBuiltin()) return;

            for (GolemVariant variant : GolemVariant.lootVariants()) {
                MultiGolem.VariantLootDrop drop = MultiGolem.lootDropFor(variant);
                MultiGolem.addVariantPool(builder, variant, drop.item(), drop.min(), drop.max());
            }
        });
    }
}
