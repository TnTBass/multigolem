package dev.charles.multigolem.config;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiGolemConfigV2Test {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void defaults_V2_perTierAbilityFields() {
        MultiGolemConfig cfg = MultiGolemConfig.defaults();
        // Copper
        TierStats copper = cfg.tier(GolemVariant.COPPER);
        assertEquals(List.of("CREEPERS"), copper.ignoredTargetTypes());
        assertTrue(copper.copperLightningImmune());
        assertNull(copper.copperLightningHealAmount(), "null = full heal");
        // Iron — ignored_target_types defaults to empty
        assertEquals(List.of(), cfg.tier(GolemVariant.IRON).ignoredTargetTypes());
        // Gold
        TierStats gold = cfg.tier(GolemVariant.GOLD);
        assertEquals(1.75, gold.goldSpeedMultiplier(), 0.0001);
        assertTrue(gold.goldSprintParticlesEnabled());
        assertTrue(gold.goldSunlightShineEnabled());
        // Emerald
        TierStats em = cfg.tier(GolemVariant.EMERALD);
        assertEquals(8, em.emeraldAuraRange());
        assertEquals(2.0, em.emeraldHealIntervalSeconds(), 0.0001);
        assertEquals(1.0, em.emeraldHealPerTick(), 0.0001);
        assertTrue(em.emeraldCountWanderingTraders());
        // Diamond
        TierStats di = cfg.tier(GolemVariant.DIAMOND);
        assertEquals("ALL_HOSTILE_MOBS", di.diamondTargetMode());
        assertEquals(30, di.diamondCooldownMinSeconds());
        assertEquals(60, di.diamondCooldownMaxSeconds());
        assertEquals(12, di.diamondAuraRange());
        assertTrue(di.diamondLightningProof());
        // Netherite
        TierStats nh = cfg.tier(GolemVariant.NETHERITE);
        assertTrue(nh.netheriteFireImmune());
        assertEquals(5, nh.netheriteIgniteSeconds());
    }

    @Test
    void v2_clamps_outOfRange_values(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "allow_golem_healing": true,
              "tiers": {
                "copper":    { "max_health": 60, "attack_damage": 8.5, "anger_on_hit": true, "ignored_target_types": ["CREEPERS"], "copper_lightning_immune": true, "copper_lightning_heal_amount": -5.0 },
                "iron":      { "max_health": 100, "attack_damage": 15.0, "anger_on_hit": true, "ignored_target_types": [] },
                "gold":      { "max_health": 130, "attack_damage": 22.5, "anger_on_hit": true, "ignored_target_types": ["CREEPERS"], "gold_speed_multiplier": 0.0, "gold_sprint_particles_enabled": true, "gold_sunlight_shine_enabled": true },
                "emerald":   { "max_health": 200, "attack_damage": 40.0, "anger_on_hit": true, "ignored_target_types": ["CREEPERS"], "emerald_aura_range": 0, "emerald_heal_interval_seconds": 0.0, "emerald_heal_per_tick": -1.0, "emerald_count_wandering_traders": true },
                "diamond":   { "max_health": 350, "attack_damage": 62.5, "anger_on_hit": true, "ignored_target_types": ["CREEPERS"], "diamond_target_mode": "all_hostile_mobs", "diamond_cooldown_min_seconds": -10, "diamond_cooldown_max_seconds": 999999, "diamond_aura_range": 100, "diamond_lightning_proof": true },
                "netherite": { "max_health": 600, "attack_damage": 85.0, "anger_on_hit": true, "ignored_target_types": ["CREEPERS"], "netherite_fire_immune": true, "netherite_ignite_seconds": -1 }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(0.0, cfg.tier(GolemVariant.COPPER).copperLightningHealAmount(), 0.0001);
        assertEquals(0.1, cfg.tier(GolemVariant.GOLD).goldSpeedMultiplier(), 0.0001);
        assertEquals(1, cfg.tier(GolemVariant.EMERALD).emeraldAuraRange());
        assertEquals(0.5, cfg.tier(GolemVariant.EMERALD).emeraldHealIntervalSeconds(), 0.0001);
        assertEquals(0.0, cfg.tier(GolemVariant.EMERALD).emeraldHealPerTick(), 0.0001);
        assertEquals("ALL_HOSTILE_MOBS", cfg.tier(GolemVariant.DIAMOND).diamondTargetMode());
        assertEquals(0, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMinSeconds());
        assertEquals(3600, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMaxSeconds());
        assertEquals(64, cfg.tier(GolemVariant.DIAMOND).diamondAuraRange());
        assertEquals(0, cfg.tier(GolemVariant.NETHERITE).netheriteIgniteSeconds());
    }

    @Test
    void v2_unknownDiamondTargetMode_fallsBackToDefault(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "diamond": { "diamond_target_mode": "DRAGONS_ONLY" }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals("ALL_HOSTILE_MOBS", cfg.tier(GolemVariant.DIAMOND).diamondTargetMode());
    }

    @Test
    void v2_diamondCooldown_swapsIfMinGreaterThanMax(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "diamond": { "diamond_cooldown_min_seconds": 90, "diamond_cooldown_max_seconds": 30 }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(30, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMinSeconds());
        assertEquals(90, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMaxSeconds());
    }

    @Test
    void v2_caseInsensitiveDiamondTargetMode(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": { "diamond": { "diamond_target_mode": "bosses_only" } }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals("BOSSES_ONLY", cfg.tier(GolemVariant.DIAMOND).diamondTargetMode());
    }

    @Test
    void v2_unknownIgnoredTargetTypeValue_dropped(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": { "copper": { "ignored_target_types": ["CREEPERS", "DRAGONS_AND_CASTLES", "ENDERMEN"] } }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(List.of("CREEPERS", "ENDERMEN"), cfg.tier(GolemVariant.COPPER).ignoredTargetTypes());
    }

    @Test
    void v2_nonFiniteDoubles_replacedWithDefaults(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": { "gold": { "gold_speed_multiplier": "NaN" } }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(1.75, cfg.tier(GolemVariant.GOLD).goldSpeedMultiplier(), 0.0001);
    }

    @Test
    void v2_copperHealAmount_jsonNull_meansFullHeal(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": { "copper": { "copper_lightning_heal_amount": null } }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertNull(cfg.tier(GolemVariant.COPPER).copperLightningHealAmount(),
            "explicit JSON null preserved as null (= full heal)");
    }

    @Test
    void v2_stringCooldownValues_doNotCrash(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "diamond": {
                  "diamond_cooldown_min_seconds": "oops",
                  "diamond_cooldown_max_seconds": "also_bad"
                }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        assertEquals(30, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMinSeconds());
        assertEquals(60, cfg.tier(GolemVariant.DIAMOND).diamondCooldownMaxSeconds());
    }

    @Test
    void migration_preservesUnknownTopLevelFields(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "_user_note": "lowered copper hp",
              "_third_party_mod_settings": { "foo": 42 },
              "allow_golem_healing": true,
              "tiers": {}
            }
            """);
        MultiGolemConfig.loadOrCreate(file);
        String after = Files.readString(file);
        assertTrue(after.contains("_user_note"), "unknown top-level field must be preserved");
        assertTrue(after.contains("_third_party_mod_settings"), "unknown top-level object must be preserved");
        assertTrue(after.contains("\"foo\""), "nested unknown fields must be preserved");
    }

    @Test
    void migration_preservesUnknownTierFields(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": {
                "copper": {
                  "max_health": 60,
                  "attack_damage": 8.5,
                  "anger_on_hit": true,
                  "_my_experimental_field": "preserve_me"
                }
              }
            }
            """);
        MultiGolemConfig.loadOrCreate(file);
        String after = Files.readString(file);
        assertTrue(after.contains("_my_experimental_field"), "unknown per-tier field must be preserved");
        assertTrue(after.contains("preserve_me"));
    }

    @Test
    void migration_fillsV2Defaults_preservesV1Values(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        // V1-shaped config (no V2 fields)
        Files.writeString(file, """
            {
              "allow_golem_healing": true,
              "tiers": {
                "copper": { "max_health": 75, "attack_damage": 10.0, "anger_on_hit": true }
              }
            }
            """);
        MultiGolemConfig cfg = MultiGolemConfig.loadOrCreate(file);
        // V1 value preserved
        assertEquals(75, cfg.tier(GolemVariant.COPPER).maxHealth());
        // V2 default filled
        assertTrue(cfg.tier(GolemVariant.COPPER).copperLightningImmune());
        // Disk written with V2 schema
        String after = Files.readString(file);
        assertTrue(after.contains("\"copper_lightning_immune\""));
        assertTrue(after.contains("\"max_health\": 75"), "V1 value preserved on disk");
    }

    @Test
    void migration_canonicalizesDiamondTargetMode(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        Files.writeString(file, """
            {
              "tiers": { "diamond": { "diamond_target_mode": "all_hostile_mobs_and_players" } }
            }
            """);
        MultiGolemConfig.loadOrCreate(file);
        String after = Files.readString(file);
        assertTrue(after.contains("\"ALL_HOSTILE_MOBS_AND_PLAYERS\""), "canonical uppercase rewritten on disk");
        assertFalse(after.contains("all_hostile_mobs_and_players"), "lowercase removed");
    }

    @Test
    void migration_doesNotRewriteIdenticalFile(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("multigolem.json");
        // First load writes defaults
        MultiGolemConfig.loadOrCreate(file);
        long mtime1 = Files.getLastModifiedTime(file).toMillis();
        // Tiny sleep so mtime would change if a re-write happens
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        // Second load should NOT rewrite
        MultiGolemConfig.loadOrCreate(file);
        long mtime2 = Files.getLastModifiedTime(file).toMillis();
        assertEquals(mtime1, mtime2, "identical config should not be rewritten");
    }
}
