package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public final class SpawnerVariantMarker {
    private static final String FAMILY_KEY = "family";
    private static final String VARIANT_KEY = "variant";

    private SpawnerVariantMarker() {}

    public static void write(CompoundTag entityTag, GolemVariant variant) {
        writeIdentity(entityTag, GolemIdentity.ofIronVariant(variant));
    }

    public static void writeIdentity(CompoundTag entityTag, GolemIdentity identity) {
        CompoundTag multigolem = new CompoundTag();
        multigolem.putString(FAMILY_KEY, identity.family().id());
        multigolem.putString(VARIANT_KEY, identity.variant().id());
        entityTag.put(MultiGolem.MOD_ID, multigolem);
    }

    public static Optional<GolemVariant> read(CompoundTag entityTag) {
        return readIdentity(entityTag).map(GolemIdentity::variant);
    }

    public static Optional<GolemIdentity> readIdentity(CompoundTag entityTag) {
        CompoundTag multigolem = entityTag.getCompoundOrEmpty(MultiGolem.MOD_ID);
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
}
