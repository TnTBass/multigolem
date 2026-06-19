package dev.charles.multigolem.test;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;

/** Shared helper to bootstrap Minecraft static state for unit tests. */
public final class MinecraftBootstrap {

    private static boolean done = false;

    private MinecraftBootstrap() {}

    /** Idempotent. Call from @BeforeAll in any test that touches Blocks, Items, or other registry-backed statics. */
    public static synchronized void ensure() {
        if (done) return;
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        bindIronGolemSpawnEggComponents();
        done = true;
    }

    @SuppressWarnings("deprecation")
    private static void bindIronGolemSpawnEggComponents() {
        Holder.Reference<Item> holder = Items.IRON_GOLEM_SPAWN_EGG.builtInRegistryHolder();
        if (holder.areComponentsBound()) {
            return;
        }
        holder.bindComponents(DataComponentMap.builder()
            .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
            .set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityTypes.IRON_GOLEM, new CompoundTag()))
            .build());
    }
}
