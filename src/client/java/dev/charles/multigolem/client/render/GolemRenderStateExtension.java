package dev.charles.multigolem.client.render;

import dev.charles.multigolem.identity.GolemIdentity;

public interface GolemRenderStateExtension {
    GolemIdentity multigolem$getIdentity();
    void multigolem$setIdentity(GolemIdentity identity);
}
