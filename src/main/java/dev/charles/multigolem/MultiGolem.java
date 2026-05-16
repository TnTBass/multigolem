package dev.charles.multigolem;

import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.MultiGolemConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class MultiGolem implements ModInitializer {
    public static final String MOD_ID = "multigolem";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static volatile MultiGolemConfig CONFIG = MultiGolemConfig.defaults();

    public static MultiGolemConfig config() { return CONFIG; }

    @Override
    public void onInitialize() {
        GolemVariantAttachment.touch();
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
        CONFIG = MultiGolemConfig.loadOrCreate(configFile);
        LOG.info("MultiGolem starting up - config loaded from {}", configFile);
    }
}
