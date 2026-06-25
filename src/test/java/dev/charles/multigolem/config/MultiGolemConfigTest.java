package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemConfigTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void defaults_haveExpectedStats() {
        MultiGolemConfig cfg = MultiGolemConfig.defaults();
        assertTrue(cfg.allowGolemHealing());
        assertEquals(60,   cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(8.5,  cfg.tier(GolemVariant.COPPER).attackDamage(), 0.0001);
        assertEquals(100,  cfg.tier(GolemVariant.IRON).maxHealth());
        assertEquals(15.0, cfg.tier(GolemVariant.IRON).attackDamage(), 0.0001);
        TierStats redstone = cfg.tier(GolemVariant.REDSTONE);
        assertEquals(90, redstone.maxHealth());
        assertEquals(13.0, redstone.attackDamage(), 0.0001);
        assertEquals(java.util.List.of("CREEPERS"), redstone.ignoredTargetTypes());
        assertTrue(redstone.redstoneOverchargeEnabled());
        assertEquals(0.25, redstone.redstoneOverchargeHealthThresholdPercent(), 0.0001);
        assertEquals(12.0, redstone.redstoneOverchargeDurationSeconds(), 0.0001);
        assertEquals(45.0, redstone.redstoneOverchargeCooldownSeconds(), 0.0001);
        assertEquals(1.5, redstone.redstoneOverchargeAttackMultiplier(), 0.0001);
        assertEquals(1, redstone.redstoneOverchargeResistanceAmplifier());
        assertEquals(3.0, redstone.redstoneOverchargeResistanceRefreshSeconds(), 0.0001);
        assertTrue(redstone.redstoneDeathPulseEnabled());
        assertEquals(8, redstone.redstoneDeathPulseRadius());
        assertEquals(6.0, redstone.redstoneDeathPulseSlownessSeconds(), 0.0001);
        assertEquals(9, redstone.redstoneDeathPulseSlownessAmplifier());
        assertTrue(redstone.redstoneParticlesEnabled());
        assertTrue(redstone.redstoneDeathPulseParticlesEnabled());
        assertEquals(600,  cfg.tier(GolemVariant.NETHERITE).maxHealth());
        assertEquals(85.0, cfg.tier(GolemVariant.NETHERITE).attackDamage(), 0.0001);
        assertEquals(100,  cfg.tier(GolemVariant.ZOMBIE).maxHealth());
        assertEquals(15.0, cfg.tier(GolemVariant.ZOMBIE).attackDamage(), 0.0001);
        for (GolemVariant v : GolemVariant.values()) {
            assertTrue(cfg.tier(v).angerOnHit(), v + " should default angerOnHit=true");
        }
    }

    @Test
    void loadFromFile_missingFile_writesDefaults(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        MultiGolemConfig loaded = MultiGolemConfig.loadOrCreate(file);
        assertEquals(MultiGolemConfig.defaults(), loaded);
        assertTrue(Files.exists(file), "default config should be written on first run");
    }

    @Test
    void loadFromFile_validJson_parses(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "allow_golem_healing": false,
              "tiers": {
                "copper":    { "max_health": 50,  "attack_damage": 5.0,  "anger_on_hit": false },
                "iron":      { "max_health": 100, "attack_damage": 15.0, "anger_on_hit": true  },
                "gold":      { "max_health": 130, "attack_damage": 22.5, "anger_on_hit": true  },
                "emerald":   { "max_health": 200, "attack_damage": 40.0, "anger_on_hit": true  },
                "diamond":   { "max_health": 350, "attack_damage": 62.5, "anger_on_hit": true  },
                "netherite": { "max_health": 600, "attack_damage": 85.0, "anger_on_hit": true  }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertFalse(cfg.allowGolemHealing());
        assertEquals(50,  cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(5.0, cfg.tier(GolemVariant.COPPER).attackDamage(), 0.0001);
        assertFalse(cfg.tier(GolemVariant.COPPER).angerOnHit());
    }

    @Test
    void loadFromFile_malformedJson_returnsDefaultsAndDoesNotOverwrite(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        String original = "{not valid json";
        Files.writeString(file, original);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(MultiGolemConfig.defaults(), cfg);
        assertEquals(original, Files.readString(file), "malformed config must not be overwritten");
    }

    @Test
    void loadFromFile_outOfRangeValues_clamped(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "allow_golem_healing": true,
              "tiers": {
                "copper":    { "max_health": -10,    "attack_damage": -5.0,  "anger_on_hit": true },
                "iron":      { "max_health": 100,    "attack_damage": 15.0,  "anger_on_hit": true },
                "gold":      { "max_health": 130,    "attack_damage": 22.5,  "anger_on_hit": true },
                "emerald":   { "max_health": 200,    "attack_damage": 40.0,  "anger_on_hit": true },
                "diamond":   { "max_health": 350,    "attack_damage": 62.5,  "anger_on_hit": true },
                "netherite": { "max_health": 100000, "attack_damage": 1e9,   "anger_on_hit": true }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(1,    cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(0.0,  cfg.tier(GolemVariant.COPPER).attackDamage(), 0.0001);
        assertEquals(2048, cfg.tier(GolemVariant.NETHERITE).maxHealth());
        assertEquals(2048.0, cfg.tier(GolemVariant.NETHERITE).attackDamage(), 0.0001);
    }

    @Test
    void loadFromFile_redstoneOutOfRangeAbilityValues_clamped(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "redstone": {
                  "max_health": 90,
                  "attack_damage": 13.0,
                  "anger_on_hit": true,
                  "redstone_overcharge_health_threshold_percent": 0.0,
                  "redstone_overcharge_duration_seconds": -1.0,
                  "redstone_overcharge_cooldown_seconds": 5000.0,
                  "redstone_overcharge_attack_multiplier": 0.5,
                  "redstone_overcharge_resistance_amplifier": 99,
                  "redstone_overcharge_resistance_refresh_seconds": 0.1,
                  "redstone_death_pulse_radius": 1000,
                  "redstone_death_pulse_slowness_seconds": -2.0,
                  "redstone_death_pulse_slowness_amplifier": 99
                }
              }
            }
            """);

        TierStats redstone = MultiGolemConfig.loadOrCreate(file).tier(GolemVariant.REDSTONE);
        assertEquals(0.01, redstone.redstoneOverchargeHealthThresholdPercent(), 0.0001);
        assertEquals(0.0, redstone.redstoneOverchargeDurationSeconds(), 0.0001);
        assertEquals(3600.0, redstone.redstoneOverchargeCooldownSeconds(), 0.0001);
        assertEquals(1.0, redstone.redstoneOverchargeAttackMultiplier(), 0.0001);
        assertEquals(4, redstone.redstoneOverchargeResistanceAmplifier());
        assertEquals(0.5, redstone.redstoneOverchargeResistanceRefreshSeconds(), 0.0001);
        assertEquals(64, redstone.redstoneDeathPulseRadius());
        assertEquals(0.0, redstone.redstoneDeathPulseSlownessSeconds(), 0.0001);
        assertEquals(9, redstone.redstoneDeathPulseSlownessAmplifier());
    }

    @Test
    void loadFromFile_partialTiers_filledWithDefaults(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "copper": { "max_health": 70, "attack_damage": 10.0, "anger_on_hit": false }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertTrue(cfg.allowGolemHealing(), "missing top-level fields fall back to default");
        assertEquals(70, cfg.tier(GolemVariant.COPPER).maxHealth());
        assertEquals(100, cfg.tier(GolemVariant.IRON).maxHealth(), "missing tier uses default");
        assertEquals(90, cfg.tier(GolemVariant.REDSTONE).maxHealth(), "missing redstone tier uses default");
        assertEquals(600, cfg.tier(GolemVariant.NETHERITE).maxHealth(), "missing tier uses default");
        assertEquals(100, cfg.tier(GolemVariant.ZOMBIE).maxHealth(), "missing zombie tier uses default");
    }
}
