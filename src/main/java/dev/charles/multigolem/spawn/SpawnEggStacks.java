package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public final class SpawnEggStacks {
    private static final String ROOT_KEY = MultiGolem.MOD_ID;
    private static final String VARIANT_KEY = "variant";

    private SpawnEggStacks() {}

    public static ItemStack create(GolemVariant variant) {
        if (variant == GolemVariant.IRON) {
            throw new IllegalArgumentException("Iron is vanilla-owned and has no marked V4 spawn egg");
        }
        ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(markerTag(variant)));
        return stack;
    }

    public static Optional<GolemVariant> variantFrom(ItemStack stack) {
        if (!stack.is(Items.IRON_GOLEM_SPAWN_EGG)) {
            return Optional.empty();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag multigolem = data.copyTag().getCompoundOrEmpty(ROOT_KEY);
        return GolemVariant.fromId(multigolem.getStringOr(VARIANT_KEY, ""))
            .filter(variant -> variant != GolemVariant.IRON);
    }

    public static String customDataSnbt(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().toString();
    }

    private static CompoundTag markerTag(GolemVariant variant) {
        CompoundTag root = new CompoundTag();
        CompoundTag multigolem = new CompoundTag();
        multigolem.putString(VARIANT_KEY, variant.id());
        root.put(ROOT_KEY, multigolem);
        return root;
    }
}
