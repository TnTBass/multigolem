package dev.charles.multigolem.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronGolemRegisterGoalsMixinTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Pattern TARGET_SELECTOR_SHADOW =
        Pattern.compile("@Shadow[\\s\\S]{0,120}\\btargetSelector\\b");

    @Test
    void targetSelectorComesFromMobAccessorNotIronGolemShadow() throws Exception {
        String mixin = Files.readString(PROJECT_ROOT.resolve(
            "src/common/java/dev/charles/multigolem/mixin/IronGolemRegisterGoalsMixin.java"));
        String accessor = Files.readString(PROJECT_ROOT.resolve(
            "src/common/java/dev/charles/multigolem/mixin/MobTargetSelectorAccessor.java"));
        String mixinsJson = Files.readString(PROJECT_ROOT.resolve("src/fabric/resources/multigolem.mixins.json"));

        assertFalse(TARGET_SELECTOR_SHADOW.matcher(mixin).find(),
            "IronGolem does not declare targetSelector in Minecraft 26.1.2; it is inherited from Mob.");
        assertTrue(mixin.contains("MobTargetSelectorAccessor"),
            "IronGolemRegisterGoalsMixin should read the inherited Mob target selector through an accessor.");
        assertTrue(accessor.contains("@Accessor(\"targetSelector\")"),
            "The Mob accessor must target the inherited targetSelector field.");
        assertTrue(accessor.contains("GoalSelector multigolem$getTargetSelector()"),
            "The Mob accessor must return the inherited GoalSelector.");
        assertTrue(mixinsJson.contains("\"MobTargetSelectorAccessor\""),
            "The Mob accessor mixin must be registered so it is applied at runtime.");
    }
}
