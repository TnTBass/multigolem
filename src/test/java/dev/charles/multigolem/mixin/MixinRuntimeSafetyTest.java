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
}
