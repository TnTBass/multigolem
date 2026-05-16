package dev.charles.multigolem;

import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiGolem implements ModInitializer {
    public static final String MOD_ID = "multigolem";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GolemVariantAttachment.touch();
        LOG.info("MultiGolem starting up - Charles & Tyler's golem variants");
    }
}
