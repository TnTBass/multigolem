package dev.charles.multigolem.client.render;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class GolemTextureSelector {

    private static final Identifier IRON_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");

    private static final Map<GolemVariant, Identifier> TEXTURES = new EnumMap<>(GolemVariant.class);

    static {
        TEXTURES.put(GolemVariant.IRON, IRON_TEXTURE);
        for (GolemVariant v : GolemVariant.values()) {
            if (v != GolemVariant.IRON) {
                TEXTURES.put(v, Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID,
                    "textures/entity/" + v.id() + "_golem.png"));
            }
        }
    }

    public static Identifier get(GolemVariant variant) {
        return TEXTURES.getOrDefault(variant, IRON_TEXTURE);
    }

    public static Identifier get(GolemIdentity identity) {
        if (identity.family() == GolemFamily.IRON_GOLEM && identity.variant() == GolemVariant.COPPER) {
            return copperTexture(identity.surfaceState().orElse(GolemSurfaceState.DEFAULT));
        }
        return get(identity.variant());
    }

    private static Identifier copperTexture(GolemSurfaceState surface) {
        String suffix = switch (surface.weatheringStage()) {
            case UNAFFECTED -> surface.waxed() ? "_waxed" : "";
            case EXPOSED -> surface.waxed() ? "_waxed_exposed" : "_exposed";
            case WEATHERED -> surface.waxed() ? "_waxed_weathered" : "_weathered";
            case OXIDIZED -> surface.waxed() ? "_waxed_oxidized" : "_oxidized";
        };
        return Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID,
            "textures/entity/iron_golem/copper_golem" + suffix + ".png");
    }

    private GolemTextureSelector() {}
}
