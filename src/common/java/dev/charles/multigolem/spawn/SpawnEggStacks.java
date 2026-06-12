package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
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
    private static final String SURFACE_KEY = "surface";
    private static final String WEATHERING_STAGE_KEY = "weathering_stage";
    private static final String WAXED_KEY = "waxed";

    private SpawnEggStacks() {}

    public static ItemStack create(GolemVariant variant) {
        return create(GolemIdentity.ofIronVariant(variant));
    }

    public static ItemStack create(GolemIdentity identity) {
        if (identity.variant() == GolemVariant.IRON || !identity.isValidForPhase3()) {
            throw new IllegalArgumentException("Unsupported marked spawn egg identity: " + identity);
        }
        ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
        stack.set(DataComponents.ITEM_NAME, Component.literal(identity.variant().displayName() + " Golem Spawn Egg"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(markerTag(identity)));
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
        return identityFromMarker(multigolem);
    }

    public static String customDataSnbt(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().toString();
    }

    // Package-private so spawner markers reuse the exact same legacy marker parser.
    static Optional<GolemIdentity> identityFromMarker(CompoundTag multigolem) {
        Optional<GolemFamily> family = GolemFamily.fromId(multigolem.getStringOr(FAMILY_KEY, GolemFamily.IRON_GOLEM.id()));
        Optional<GolemVariant> variant = GolemVariant.fromId(multigolem.getStringOr(VARIANT_KEY, ""));
        Optional<Optional<GolemSurfaceState>> surface = surfaceFrom(multigolem);
        if (family.isEmpty() || variant.isEmpty() || surface.isEmpty()) {
            return Optional.empty();
        }
        GolemIdentity identity = new GolemIdentity(family.get(), variant.get(), surface.get());
        if (!identity.isValidForPhase3() || identity.isDefaultIron()) {
            return Optional.empty();
        }
        return Optional.of(identity);
    }

    private static Optional<Optional<GolemSurfaceState>> surfaceFrom(CompoundTag multigolem) {
        if (!multigolem.contains(SURFACE_KEY)) {
            return Optional.of(Optional.empty());
        }
        CompoundTag surface = multigolem.getCompoundOrEmpty(SURFACE_KEY);
        Optional<GolemWeatheringStage> stage =
            GolemWeatheringStage.fromId(surface.getStringOr(WEATHERING_STAGE_KEY, ""));
        if (stage.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Optional.of(new GolemSurfaceState(stage.get(), surface.getBooleanOr(WAXED_KEY, false))));
    }

    private static CompoundTag markerTag(GolemIdentity identity) {
        CompoundTag root = new CompoundTag();
        CompoundTag multigolem = new CompoundTag();
        multigolem.putString(FAMILY_KEY, identity.family().id());
        multigolem.putString(VARIANT_KEY, identity.variant().id());
        identity.surfaceState().ifPresent(surfaceState -> {
            CompoundTag surface = new CompoundTag();
            surface.putString(WEATHERING_STAGE_KEY, surfaceState.weatheringStage().id());
            surface.putBoolean(WAXED_KEY, surfaceState.waxed());
            multigolem.put(SURFACE_KEY, surface);
        });
        root.put(ROOT_KEY, multigolem);
        return root;
    }
}
