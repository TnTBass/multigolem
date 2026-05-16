package dev.charles.multigolem.client.render;

import dev.charles.multigolem.GolemVariant;

public interface GolemRenderStateExtension {
    GolemVariant multigolem$getVariant();
    void multigolem$setVariant(GolemVariant variant);
}
