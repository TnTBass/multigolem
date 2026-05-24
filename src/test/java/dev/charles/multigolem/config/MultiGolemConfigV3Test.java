package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.spawn.VillageSpawnWeights;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemConfigV3Test {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void defaults_V3_villageSpawningFields() {
        VillageSpawnWeights weights = MultiGolemConfig.defaults().villageSpawnWeights();
        assertTrue(weights.enabled());
        assertEquals(19, weights.weight(GolemVariant.IRON));
        assertEquals(19, weights.weight(GolemVariant.COPPER));
        assertEquals(19, weights.weight(GolemVariant.GOLD));
        assertEquals(19, weights.weight(GolemVariant.EMERALD));
        assertEquals(5, weights.weight(GolemVariant.DIAMOND));
        assertEquals(0, weights.weight(GolemVariant.NETHERITE));
    }

    @Test
    void migration_fillsV3Defaults_preservesV2ValuesAndUnknownFields(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "_user_note": "keep me",
              "allow_golem_healing": false,
              "tiers": {
                "gold": {
                  "max_health": 130,
                  "attack_damage": 22.5,
                  "anger_on_hit": true,
                  "gold_speed_multiplier": 1.5,
                  "_tier_note": "also keep me"
                }
              }
            }
            """);

        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertFalse(cfg.allowGolemHealing());
        assertEquals(1.5, cfg.tier(GolemVariant.GOLD).goldSpeedMultiplier(), 0.0001);
        assertTrue(cfg.villageSpawnWeights().enabled());

        String after = Files.readString(file);
        assertTrue(after.contains("\"village_spawning\""));
        assertTrue(after.contains("villages are made of wood"));
        assertTrue(after.contains("_user_note"));
        assertTrue(after.contains("_tier_note"));

        MultiGolemConfig reloaded = MultiGolemConfig.loadOrCreate(file);
        assertTrue(reloaded.villageSpawnWeights().enabled());
        assertEquals(81, reloaded.villageSpawnWeights().totalWeight());
    }

    @Test
    void enabledFalse_parsesCorrectly(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "village_spawning": {
                "enabled": false,
                "weights": { "iron": 19, "copper": 19, "gold": 19, "emerald": 19, "diamond": 5, "netherite": 2 }
              }
            }
            """);

        assertFalse(MultiGolemConfig.loadOrCreate(file).villageSpawnWeights().enabled());
    }

    @Test
    void missingEnabled_fillsTrue(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "village_spawning": {
                "weights": { "iron": 1, "copper": 0, "gold": 0, "emerald": 0, "diamond": 0, "netherite": 0 }
              }
            }
            """);

        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertTrue(cfg.villageSpawnWeights().enabled());
        assertTrue(Files.readString(file).contains("\"enabled\": true"));
    }

    @Test
    void malformedEnabled_fallsBackToTrue(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            { "village_spawning": { "enabled": "yes" } }
            """);

        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertTrue(cfg.villageSpawnWeights().enabled());
        assertTrue(Files.readString(file).contains("\"enabled\": true"));
    }

    @Test
    void explicitAllZeroWeights_parseAsIntentionalDisablement(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "village_spawning": {
                "enabled": true,
                "weights": { "iron": 0, "copper": 0, "gold": 0, "emerald": 0, "diamond": 0, "netherite": 0 }
              }
            }
            """);

        VillageSpawnWeights weights = MultiGolemConfig.loadOrCreate(file).villageSpawnWeights();
        assertTrue(weights.enabled());
        assertTrue(weights.isAllZero());
    }

    @Test
    void missingOrMalformedWeights_fallBackToDefaults(@TempDir Path tmp) throws IOException {
        Path missing = tmp.resolve("missing.json");
        Files.writeString(missing, """
            { "village_spawning": { "enabled": true } }
            """);
        assertEquals(81, MultiGolemConfig.loadOrCreate(missing).villageSpawnWeights().totalWeight());

        Path malformed = tmp.resolve("malformed.json");
        Files.writeString(malformed, """
            { "village_spawning": { "enabled": true, "weights": "heavy" } }
            """);
        assertEquals(81, MultiGolemConfig.loadOrCreate(malformed).villageSpawnWeights().totalWeight());
    }

    @Test
    void partialWeights_fillMissingIndividualKeysFromDefaults(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "village_spawning": {
                "weights": { "iron": 1, "copper": 2 }
              }
            }
            """);

        VillageSpawnWeights weights = MultiGolemConfig.loadOrCreate(file).villageSpawnWeights();
        assertEquals(1, weights.weight(GolemVariant.IRON));
        assertEquals(2, weights.weight(GolemVariant.COPPER));
        assertEquals(19, weights.weight(GolemVariant.GOLD));
        assertEquals(19, weights.weight(GolemVariant.EMERALD));
        assertEquals(5, weights.weight(GolemVariant.DIAMOND));
        assertEquals(0, weights.weight(GolemVariant.NETHERITE));
    }

    @Test
    void onlyUnknownWeightKeys_fallsBackToDefaultsAndPreservesUnknowns(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "village_spawning": {
                "weights": { "obsidian": 100, "lapis": 50 }
              }
            }
            """);

        VillageSpawnWeights weights = MultiGolemConfig.loadOrCreate(file).villageSpawnWeights();
        assertEquals(81, weights.totalWeight());
        String after = Files.readString(file);
        assertTrue(after.contains("\"obsidian\""));
        assertTrue(after.contains("\"lapis\""));
    }

    @Test
    void negativeAndNonNumericWeights_areRepairedPerVariant(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "village_spawning": {
                "weights": {
                  "iron": -1,
                  "copper": "many",
                  "gold": 3,
                  "emerald": 4,
                  "diamond": 5,
                  "netherite": 6
                }
              }
            }
            """);

        VillageSpawnWeights weights = MultiGolemConfig.loadOrCreate(file).villageSpawnWeights();
        assertEquals(0, weights.weight(GolemVariant.IRON));
        assertEquals(19, weights.weight(GolemVariant.COPPER));
        assertEquals(3, weights.weight(GolemVariant.GOLD));
        assertEquals(4, weights.weight(GolemVariant.EMERALD));
        assertEquals(5, weights.weight(GolemVariant.DIAMOND));
        assertEquals(6, weights.weight(GolemVariant.NETHERITE));
    }

    @Test
    void unknownVariantWeightKeys_preservedButIgnored(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "village_spawning": {
                "weights": {
                  "iron": 1,
                  "copper": 1,
                  "gold": 1,
                  "emerald": 1,
                  "diamond": 1,
                  "netherite": 1,
                  "ruby": 100
                }
              }
            }
            """);

        VillageSpawnWeights weights = MultiGolemConfig.loadOrCreate(file).villageSpawnWeights();
        assertEquals(6, weights.totalWeight());
        assertTrue(Files.readString(file).contains("\"ruby\""));
    }
}
