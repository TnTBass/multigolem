package dev.charles.multigolem.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IronGolemDeathMixinSourceTest {
    @Test
    void redstoneDeathPulseRunsOnlyOnServerLevel() throws IOException {
        String source = Files.readString(Path.of(
            "src/common/java/dev/charles/multigolem/mixin/IronGolemDeathMixin.java"));

        assertTrue(source.contains("@Mixin(IronGolem.class)"));
        assertTrue(source.contains("method = \"die(Lnet/minecraft/world/damagesource/DamageSource;)V\""));
        assertTrue(source.contains("if (!(self.level() instanceof ServerLevel level)) return;"));
        assertTrue(source.contains("RedstoneAbility.emitDeathPulse(level, self);"));
    }
}
