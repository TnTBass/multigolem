package dev.charles.multigolem.fabric.status;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.status.MultiGolemStatus;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

public final class FabricMultiGolemStatus {
    private FabricMultiGolemStatus() {}

    public static void initializeVersion() {
        MultiGolemStatus.initializeVersion(
            versionFromFabric().orElseGet(() -> System.getProperty("multigolem.version")),
            MultiGolemStatus.config().clientBuild()
        );
    }

    private static Optional<String> versionFromFabric() {
        try {
            return FabricLoader.getInstance()
                .getModContainer(MultiGolem.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
