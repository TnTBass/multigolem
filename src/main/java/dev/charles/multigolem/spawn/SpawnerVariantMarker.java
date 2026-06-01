package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public final class SpawnerVariantMarker {
    private static final String FAMILY_KEY = "family";
    private static final String VARIANT_KEY = "variant";
    private static final String SURFACE_KEY = "surface";
    private static final String WEATHERING_STAGE_KEY = "weathering_stage";
    private static final String WAXED_KEY = "waxed";

    private SpawnerVariantMarker() {}

    public static void write(CompoundTag entityTag, GolemVariant variant) {
        writeIdentity(entityTag, GolemIdentity.ofIronVariant(variant));
    }

    public static void writeIdentity(CompoundTag entityTag, GolemIdentity identity) {
        CompoundTag multigolem = new CompoundTag();
        multigolem.putString(FAMILY_KEY, identity.family().id());
        multigolem.putString(VARIANT_KEY, identity.variant().id());
        identity.surfaceState().ifPresent(surfaceState -> {
            CompoundTag surface = new CompoundTag();
            surface.putString(WEATHERING_STAGE_KEY, surfaceState.weatheringStage().id());
            surface.putBoolean(WAXED_KEY, surfaceState.waxed());
            multigolem.put(SURFACE_KEY, surface);
        });
        entityTag.put(MultiGolem.MOD_ID, multigolem);
    }

    public static Optional<GolemVariant> read(CompoundTag entityTag) {
        return readIdentity(entityTag).map(GolemIdentity::variant);
    }

    public static Optional<GolemIdentity> readIdentity(CompoundTag entityTag) {
        CompoundTag multigolem = entityTag.getCompoundOrEmpty(MultiGolem.MOD_ID);
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

    /**
     * Preview display entities use the same marker contract as real spawner output;
     * default Iron remains unmarked so vanilla previews stay untouched.
     */
    public static Optional<GolemIdentity> previewIdentity(CompoundTag entityTag) {
        return readIdentity(entityTag);
    }

    public static void clear(CompoundTag entityTag) {
        entityTag.remove(MultiGolem.MOD_ID);
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
}
