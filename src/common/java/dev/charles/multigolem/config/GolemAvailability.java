package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class GolemAvailability {
    private final EnumMap<GolemFamily, FamilyAvailability> knownFamilies;

    private GolemAvailability(Map<GolemFamily, FamilyAvailability> knownFamilies) {
        this.knownFamilies = new EnumMap<>(GolemFamily.class);
        for (Map.Entry<GolemFamily, FamilyAvailability> entry : knownFamilies.entrySet()) {
            this.knownFamilies.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public static GolemAvailability defaults() {
        EnumMap<GolemFamily, FamilyAvailability> families = new EnumMap<>(GolemFamily.class);
        for (GolemFamily family : GolemFamily.values()) {
            families.put(family, familyDefault(family));
        }
        return new GolemAvailability(families);
    }

    public static FamilyAvailability familyDefault(GolemFamily family) {
        EnumMap<GolemVariant, Boolean> variants = new EnumMap<>(GolemVariant.class);
        for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
            if (spec.family() == family) {
                variants.put(spec.variant(), true);
            }
        }
        return new FamilyAvailability(true, variants);
    }

    public boolean isFamilyAvailable(GolemFamily family) {
        if (family == null) return false;
        return knownFamilies.getOrDefault(family, familyDefault(family)).enabled();
    }

    public boolean isAvailable(GolemIdentity identity) {
        if (identity == null) return false;
        return isAvailable(identity.family(), identity.variant());
    }

    public boolean isAvailable(GolemFamily family, GolemVariant variant) {
        if (family == null || variant == null || !GolemVariantCatalog.contains(family, variant)) return false;
        return knownFamilies.getOrDefault(family, familyDefault(family)).isAvailable(variant);
    }

    public Map<GolemFamily, FamilyAvailability> knownFamilies() {
        EnumMap<GolemFamily, FamilyAvailability> copy = new EnumMap<>(GolemFamily.class);
        for (Map.Entry<GolemFamily, FamilyAvailability> entry : knownFamilies.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    public GolemAvailability withFamily(GolemFamily family, boolean enabled) {
        EnumMap<GolemFamily, FamilyAvailability> families = copyFamilies();
        FamilyAvailability current = families.getOrDefault(family, familyDefault(family));
        families.put(family, new FamilyAvailability(enabled, current.variants()));
        return new GolemAvailability(families);
    }

    public GolemAvailability withVariant(GolemFamily family, GolemVariant variant, boolean enabled) {
        EnumMap<GolemFamily, FamilyAvailability> families = copyFamilies();
        FamilyAvailability current = families.getOrDefault(family, familyDefault(family));
        EnumMap<GolemVariant, Boolean> variants = current.variants();
        variants.put(variant, enabled);
        families.put(family, new FamilyAvailability(current.enabled(), variants));
        return new GolemAvailability(families);
    }

    private EnumMap<GolemFamily, FamilyAvailability> copyFamilies() {
        EnumMap<GolemFamily, FamilyAvailability> copy = new EnumMap<>(GolemFamily.class);
        for (Map.Entry<GolemFamily, FamilyAvailability> entry : knownFamilies.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GolemAvailability that)) return false;
        return knownFamilies.equals(that.knownFamilies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(knownFamilies);
    }

    public record FamilyAvailability(boolean enabled, EnumMap<GolemVariant, Boolean> variants) {
        public FamilyAvailability {
            variants = variants == null ? new EnumMap<>(GolemVariant.class) : new EnumMap<>(variants);
        }

        public boolean isAvailable(GolemVariant variant) {
            return enabled && variants.getOrDefault(variant, true);
        }

        FamilyAvailability copy() {
            return new FamilyAvailability(enabled, variants);
        }
    }
}
