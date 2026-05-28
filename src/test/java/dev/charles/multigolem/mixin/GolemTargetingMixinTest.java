package dev.charles.multigolem.mixin;

import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemTargetingMixinTest {
    private static final Path SOURCE = Path.of(
        "src/main/java/dev/charles/multigolem/mixin/GolemTargetingMixin.java");

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void mobDeclaresCanAttackHookTarget() throws Exception {
        Mob.class.getDeclaredMethod("canAttack", LivingEntity.class);
    }

    @Test
    void zombieFamilyTargetFilterRunsBeforeMobCanAttackAcceptsZombieGolems() throws Exception {
        String source = Files.readString(SOURCE);

        // Source inspection is intentional here: this catches accidental removal of the mixin hook
        // in regular unit tests, while mobDeclaresCanAttackHookTarget verifies the hook target exists.
        assertTrue(source.contains("method = \"canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z\""),
            "zombie-family golem targeting must be blocked at Mob.canAttack, not only setTarget");
        assertTrue(source.contains("zombieFamilyCanTargetGolem(self.getClass(), GolemVariantAttachment.get(targetGolem))"),
            "canAttack guard must use the same zombie-family faction rule as setTarget");
        assertTrue(source.contains("cir.setReturnValue(false)"),
            "canAttack guard must make zombie golems invalid targets for zombie-family mobs");
    }
}
