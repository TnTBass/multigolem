package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public final class SpawnerVariantMarker {
    private static final String VARIANT_KEY = "variant";

    private SpawnerVariantMarker() {}

    public static void write(CompoundTag entityTag, GolemVariant variant) {
        CompoundTag multigolem = new CompoundTag();
        multigolem.putString(VARIANT_KEY, variant.id());
        entityTag.put(MultiGolem.MOD_ID, multigolem);
    }

    public static Optional<GolemVariant> read(CompoundTag entityTag) {
        CompoundTag multigolem = entityTag.getCompoundOrEmpty(MultiGolem.MOD_ID);
        return GolemVariant.fromId(multigolem.getStringOr(VARIANT_KEY, ""))
            .filter(variant -> variant != GolemVariant.IRON);
    }

    public static void clear(CompoundTag entityTag) {
        entityTag.remove(MultiGolem.MOD_ID);
    }
}
