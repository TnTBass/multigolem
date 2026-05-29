package dev.charles.multigolem.mixin;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinRuntimeSafetyTest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Pattern NON_PRIVATE_STATIC_METHOD =
        Pattern.compile("(?m)^\\s*(?!private\\s)(?:public\\s|protected\\s)?static\\s+[^=;]+\\([^;]*\\)\\s*\\{");

    @Test
    void mixinClassesDoNotDeclareNonPrivateStaticHelpers() throws Exception {
        Path mixinRoot = PROJECT_ROOT.resolve("src/main/java/dev/charles/multigolem/mixin");
        assertTrue(Files.isDirectory(mixinRoot), "mixin source root not found: " + mixinRoot);
        int scanned = 0;
        try (var files = Files.walk(mixinRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith("Mixin.java")).toList()) {
                scanned++;
                String source = Files.readString(file);
                assertFalse(NON_PRIVATE_STATIC_METHOD.matcher(source).find(),
                    file.getFileName() + " has a non-private static method that can fail runtime mixin application.");
            }
        }
        assertTrue(scanned >= 3, "expected at least 3 mixin files, scanned: " + scanned);
    }

    @Test
    void mixinPackageContainsOnlyMixinEntrypoints() throws Exception {
        Path mixinRoot = PROJECT_ROOT.resolve("src/main/java/dev/charles/multigolem/mixin");
        assertTrue(Files.isDirectory(mixinRoot), "mixin source root not found: " + mixinRoot);
        int scanned = 0;
        try (var files = Files.walk(mixinRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                scanned++;
                String name = file.getFileName().toString();
                assertTrue(name.endsWith("Mixin.java") || name.endsWith("Accessor.java"),
                    file.getFileName() + " is in the configured mixin package but is not a mixin entrypoint.");
            }
        }
        assertTrue(scanned >= 3, "expected at least 3 java files in mixin package, scanned: " + scanned);
    }

    @Test
    void ironGolemHealClientPredictionRunsBeforePermissionDenialSideEffects() throws Exception {
        Path mixin = PROJECT_ROOT.resolve("src/main/java/dev/charles/multigolem/mixin/IronGolemMixin.java");
        String source = Files.readString(mixin);
        int clientGuard = source.indexOf("if (self.level().isClientSide())");
        int denialSideEffect = source.indexOf("if (!permissionAllowed)");
        assertTrue(clientGuard >= 0, "IronGolemMixin client prediction guard not found");
        assertTrue(denialSideEffect >= 0, "IronGolemMixin permission denial branch not found");
        assertTrue(clientGuard < denialSideEffect,
            "client prediction must run before permission denial side effects");
    }
}
