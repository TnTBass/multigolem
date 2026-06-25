package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.MultiGolemConfig;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VillageGolemSpawnResolverTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void enabledFalseLeavesIronWithoutRolling(@TempDir Path tmp) throws IOException {
        MultiGolemConfig config = load(tmp, """
            { "village_spawning": { "enabled": false } }
            """);

        VillageGolemSpawnResolver resolver = new VillageGolemSpawnResolver(config);
        assertEquals(Optional.empty(), resolver.rollVariant(bound -> fail("disabled resolver must not roll")));
    }

    @Test
    void allZeroWeightsLeaveIronWithoutRolling(@TempDir Path tmp) throws IOException {
        MultiGolemConfig config = load(tmp, """
            {
              "village_spawning": {
                "weights": { "iron": 0, "copper": 0, "redstone": 0, "gold": 0, "emerald": 0, "diamond": 0, "netherite": 0 }
              }
            }
            """);

        VillageGolemSpawnResolver resolver = new VillageGolemSpawnResolver(config);
        assertEquals(Optional.empty(), resolver.rollVariant(bound -> fail("all-zero resolver must not roll")));
    }

    @Test
    void ironRollLeavesIron(@TempDir Path tmp) throws IOException {
        MultiGolemConfig config = load(tmp, """
            {
              "village_spawning": {
                "weights": { "iron": 1, "copper": 0, "redstone": 0, "gold": 0, "emerald": 0, "diamond": 0, "netherite": 0 }
              }
            }
            """);

        VillageGolemSpawnResolver resolver = new VillageGolemSpawnResolver(config);
        assertEquals(Optional.empty(), resolver.rollVariant(bound -> 0));
    }

    @Test
    void nonIronRollReturnsVariant(@TempDir Path tmp) throws IOException {
        MultiGolemConfig config = load(tmp, """
            {
              "village_spawning": {
                "weights": { "iron": 0, "copper": 1, "redstone": 0, "gold": 0, "emerald": 0, "diamond": 0, "netherite": 0 }
              }
            }
            """);

        VillageGolemSpawnResolver resolver = new VillageGolemSpawnResolver(config);
        assertEquals(Optional.of(GolemVariant.COPPER), resolver.rollVariant(bound -> 0));
    }

    @Test
    void usesConfiguredWeightsInsteadOfDefaults(@TempDir Path tmp) throws IOException {
        MultiGolemConfig config = load(tmp, """
            {
              "village_spawning": {
                "weights": { "iron": 0, "copper": 0, "redstone": 0, "gold": 0, "emerald": 0, "diamond": 0, "netherite": 1 }
              }
            }
            """);

        VillageGolemSpawnResolver resolver = new VillageGolemSpawnResolver(config);
        assertEquals(Optional.of(GolemVariant.NETHERITE), resolver.rollVariant(bound -> 0));
    }

    private static MultiGolemConfig load(Path tmp, String json) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, json);
        return MultiGolemConfig.loadOrCreate(file);
    }
}
