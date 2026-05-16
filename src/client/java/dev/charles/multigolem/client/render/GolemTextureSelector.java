package dev.charles.multigolem.client.render;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
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

    private GolemTextureSelector() {}
}
