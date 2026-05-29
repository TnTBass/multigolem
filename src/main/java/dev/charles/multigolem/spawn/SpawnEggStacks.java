package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

public final class SpawnEggStacks {
    private static final String ROOT_KEY = MultiGolem.MOD_ID;
    private static final String FAMILY_KEY = "family";
    private static final String VARIANT_KEY = "variant";

    private SpawnEggStacks() {}

    public static ItemStack create(GolemVariant variant) {
        if (variant == GolemVariant.IRON) {
            throw new IllegalArgumentException("Iron is vanilla-owned and has no marked V4 spawn egg");
        }
        ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
        stack.set(DataComponents.ITEM_NAME, Component.literal(variant.displayName() + " Golem Spawn Egg"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(markerTag(variant)));
        return stack;
    }

    public static Optional<GolemVariant> variantFrom(ItemStack stack) {
        return identityFrom(stack).map(GolemIdentity::variant);
    }

    public static Optional<GolemIdentity> identityFrom(ItemStack stack) {
        if (!stack.is(Items.IRON_GOLEM_SPAWN_EGG)) {
            return Optional.empty();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag multigolem = data.copyTag().getCompoundOrEmpty(ROOT_KEY);
        Optional<GolemFamily> family = GolemFamily.fromId(multigolem.getStringOr(FAMILY_KEY, GolemFamily.IRON_GOLEM.id()));
        Optional<GolemVariant> variant = GolemVariant.fromId(multigolem.getStringOr(VARIANT_KEY, ""));
        if (family.isEmpty() || variant.isEmpty()) {
            return Optional.empty();
        }
        GolemIdentity identity = new GolemIdentity(family.get(), variant.get());
        if (!identity.isValidForPhase2() || identity.isDefaultIron()) {
            return Optional.empty();
        }
        return Optional.of(identity);
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
