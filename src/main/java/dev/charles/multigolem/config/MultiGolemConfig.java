package dev.charles.multigolem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class MultiGolemConfig {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .serializeNulls()
        .create();

    static final Set<String> RECOGNIZED_IGNORED_TARGET_TYPES =
        Set.of("CREEPERS", "ENDERMEN", "PLAYERS", "ALL_BOSSES");
    static final Set<String> RECOGNIZED_DIAMOND_TARGET_MODES =
        Set.of("ALL_HOSTILE_MOBS", "ALL_HOSTILE_MOBS_AND_PLAYERS", "BOSSES_ONLY", "NONE");

    private final boolean allowGolemHealing;
    private final Map<GolemVariant, TierStats> tiers;

    private MultiGolemConfig(boolean allowGolemHealing, Map<GolemVariant, TierStats> tiers) {
        this.allowGolemHealing = allowGolemHealing;
        this.tiers = new EnumMap<>(tiers);
    }

    public boolean allowGolemHealing() { return allowGolemHealing; }
    public TierStats tier(GolemVariant variant) { return tiers.get(variant); }

    public static MultiGolemConfig defaults() {
        EnumMap<GolemVariant, TierStats> m = new EnumMap<>(GolemVariant.class);
        m.put(GolemVariant.COPPER, new TierStats(60, 8.5, true, List.of("CREEPERS"),
            true, null,  // copper
            null, null, null,  // gold
            null, null, null, null,  // emerald
            null, null, null, null, null,  // diamond
            null, null));  // netherite
        m.put(GolemVariant.IRON, new TierStats(100, 15.0, true, List.of(),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null));
        m.put(GolemVariant.GOLD, new TierStats(130, 22.5, true, List.of("CREEPERS"),
            null, null,
            1.25, true, true,
            null, null, null, null,
            null, null, null, null, null,
            null, null));
        m.put(GolemVariant.EMERALD, new TierStats(200, 40.0, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            8, 2.0, 1.0, true,
            null, null, null, null, null,
            null, null));
        m.put(GolemVariant.DIAMOND, new TierStats(350, 62.5, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            null, null, null, null,
            "ALL_HOSTILE_MOBS", 30, 60, 12, true,
            null, null));
        m.put(GolemVariant.NETHERITE, new TierStats(600, 85.0, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            true, 5));
        return new MultiGolemConfig(true, m);
    }

    public static MultiGolemConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            MultiGolemConfig defaults = defaults();
            writeQuietly(path, defaults);
            return defaults;
        }
        try {
            String contents = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(contents, JsonObject.class);
            if (root == null) {
                MultiGolem.LOG.warn("multigolem config at {} is empty; using defaults", path);
                return defaults();
            }
            return parse(root);
        } catch (JsonSyntaxException e) {
            MultiGolem.LOG.warn("multigolem config at {} is malformed; using defaults, NOT overwriting: {}",
                path, e.getMessage());
            return defaults();
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not read multigolem config at {}: {}", path, e.getMessage());
            return defaults();
        }
    }

    static MultiGolemConfig parse(JsonObject root) {
        MultiGolemConfig defaults = defaults();
        boolean healing = readBoolean(root, "allow_golem_healing", defaults.allowGolemHealing);

        EnumMap<GolemVariant, TierStats> tiers = new EnumMap<>(GolemVariant.class);
        JsonObject tiersJson = root.has("tiers") && root.get("tiers").isJsonObject()
            ? root.getAsJsonObject("tiers") : new JsonObject();
        for (GolemVariant v : GolemVariant.values()) {
            TierStats def = defaults.tier(v);
            if (!tiersJson.has(v.id()) || !tiersJson.get(v.id()).isJsonObject()) {
                tiers.put(v, def);
                continue;
            }
            tiers.put(v, parseTier(v, tiersJson.getAsJsonObject(v.id()), def));
        }
        return new MultiGolemConfig(healing, tiers);
    }

    private static TierStats parseTier(GolemVariant variant, JsonObject t, TierStats def) {
        int health = clampInt(readInt(t, "max_health", def.maxHealth()), TierStats.MIN_HEALTH, TierStats.MAX_HEALTH, "max_health");
        double damage = clampDouble(readDouble(t, "attack_damage", def.attackDamage()), TierStats.MIN_DAMAGE, TierStats.MAX_DAMAGE, "attack_damage");
        boolean anger = readBoolean(t, "anger_on_hit", def.angerOnHit());
        List<String> ignored = parseIgnoredTargetTypes(t, def.ignoredTargetTypes());

        Boolean copperImmune = def.copperLightningImmune() != null
            ? readBoolean(t, "copper_lightning_immune", def.copperLightningImmune())
            : null;
        Double copperHeal = def.copperLightningImmune() != null
            ? parseCopperHealAmount(t, def.copperLightningHealAmount())
            : null;

        Double goldSpeed = def.goldSpeedMultiplier() != null
            ? clampDouble(readDouble(t, "gold_speed_multiplier", def.goldSpeedMultiplier()), 0.1, 10.0, "gold_speed_multiplier")
            : null;
        Boolean goldSprint = def.goldSprintParticlesEnabled() != null
            ? readBoolean(t, "gold_sprint_particles_enabled", def.goldSprintParticlesEnabled())
            : null;
        Boolean goldShine = def.goldSunlightShineEnabled() != null
            ? readBoolean(t, "gold_sunlight_shine_enabled", def.goldSunlightShineEnabled())
            : null;

        Integer emeraldRange = def.emeraldAuraRange() != null
            ? clampInt(readInt(t, "emerald_aura_range", def.emeraldAuraRange()), 1, 64, "emerald_aura_range")
            : null;
        Double emeraldInterval = def.emeraldHealIntervalSeconds() != null
            ? Math.max(0.5, readDouble(t, "emerald_heal_interval_seconds", def.emeraldHealIntervalSeconds()))
            : null;
        Double emeraldHeal = def.emeraldHealPerTick() != null
            ? clampDouble(readDouble(t, "emerald_heal_per_tick", def.emeraldHealPerTick()), 0.0, 2048.0, "emerald_heal_per_tick")
            : null;
        Boolean emeraldWandering = def.emeraldCountWanderingTraders() != null
            ? readBoolean(t, "emerald_count_wandering_traders", def.emeraldCountWanderingTraders())
            : null;

        String diamondMode = def.diamondTargetMode() != null
            ? parseDiamondTargetMode(t, def.diamondTargetMode())
            : null;
        Integer diamondMin = def.diamondCooldownMinSeconds() != null
            ? clampInt(readInt(t, "diamond_cooldown_min_seconds", def.diamondCooldownMinSeconds()), 0, 3600, "diamond_cooldown_min_seconds")
            : null;
        Integer diamondMax = def.diamondCooldownMaxSeconds() != null
            ? clampInt(readInt(t, "diamond_cooldown_max_seconds", def.diamondCooldownMaxSeconds()), 0, 3600, "diamond_cooldown_max_seconds")
            : null;
        if (diamondMin != null && diamondMax != null && diamondMin > diamondMax) {
            MultiGolem.LOG.warn("diamond cooldown min ({}) > max ({}); swapping", diamondMin, diamondMax);
            int tmp = diamondMin; diamondMin = diamondMax; diamondMax = tmp;
        }
        Integer diamondRange = def.diamondAuraRange() != null
            ? clampInt(readInt(t, "diamond_aura_range", def.diamondAuraRange()), 1, 64, "diamond_aura_range")
            : null;
        Boolean diamondProof = def.diamondLightningProof() != null
            ? readBoolean(t, "diamond_lightning_proof", def.diamondLightningProof())
            : null;

        Boolean netheriteImmune = def.netheriteFireImmune() != null
            ? readBoolean(t, "netherite_fire_immune", def.netheriteFireImmune())
            : null;
        Integer netheriteIgnite = def.netheriteIgniteSeconds() != null
            ? clampInt(readInt(t, "netherite_ignite_seconds", def.netheriteIgniteSeconds()), 0, 300, "netherite_ignite_seconds")
            : null;

        return new TierStats(health, damage, anger, ignored,
            copperImmune, copperHeal,
            goldSpeed, goldSprint, goldShine,
            emeraldRange, emeraldInterval, emeraldHeal, emeraldWandering,
            diamondMode, diamondMin, diamondMax, diamondRange, diamondProof,
            netheriteImmune, netheriteIgnite);
    }

    private static List<String> parseIgnoredTargetTypes(JsonObject t, List<String> fallback) {
        if (!t.has("ignored_target_types") || !t.get("ignored_target_types").isJsonArray()) {
            return fallback;
        }
        JsonArray arr = t.getAsJsonArray("ignored_target_types");
        List<String> result = new ArrayList<>();
        for (JsonElement e : arr) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) continue;
            String s = e.getAsString();
            if (RECOGNIZED_IGNORED_TARGET_TYPES.contains(s)) {
                result.add(s);
            } else {
                MultiGolem.LOG.warn("unknown ignored_target_types value '{}'; dropped", s);
            }
        }
        return result;
    }

    private static Double parseCopperHealAmount(JsonObject t, Double fallback) {
        if (!t.has("copper_lightning_heal_amount")) return fallback;
        JsonElement el = t.get("copper_lightning_heal_amount");
        if (el instanceof JsonNull) return null; // explicit null = full heal
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            MultiGolem.LOG.warn("copper_lightning_heal_amount is not a number or null; using default");
            return fallback;
        }
        double raw = el.getAsDouble();
        if (!Double.isFinite(raw)) {
            MultiGolem.LOG.warn("copper_lightning_heal_amount is not finite; using default");
            return fallback;
        }
        return Math.max(0.0, Math.min(2048.0, raw));
    }

    private static String parseDiamondTargetMode(JsonObject t, String fallback) {
        if (!t.has("diamond_target_mode") || !t.get("diamond_target_mode").isJsonPrimitive()
                || !t.get("diamond_target_mode").getAsJsonPrimitive().isString()) {
            return fallback;
        }
        String raw = t.get("diamond_target_mode").getAsString();
        String canonical = raw == null ? null : raw.toUpperCase(Locale.ROOT);
        if (canonical == null || !RECOGNIZED_DIAMOND_TARGET_MODES.contains(canonical)) {
            MultiGolem.LOG.warn("unknown diamond_target_mode '{}'; using {}", raw, fallback);
            return fallback;
        }
        return canonical;
    }

    private static int readInt(JsonObject obj, String key, int fallback) {
        if (!obj.has(key)) return fallback;
        JsonElement el = obj.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            MultiGolem.LOG.warn("field {} is not a number; using default {}", key, fallback);
            return fallback;
        }
        try {
            return el.getAsInt();
        } catch (Exception e) {
            MultiGolem.LOG.warn("field {} could not be parsed as int; using default {}", key, fallback);
            return fallback;
        }
    }

    private static double readDouble(JsonObject obj, String key, double fallback) {
        if (!obj.has(key)) return fallback;
        JsonElement el = obj.get(key);
        if (!el.isJsonPrimitive()) {
            MultiGolem.LOG.warn("field {} is not a number; using default {}", key, fallback);
            return fallback;
        }
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (!p.isNumber()) {
            // Accept string "NaN"/"Infinity" as malformed → default.
            MultiGolem.LOG.warn("field {} is not a number; using default {}", key, fallback);
            return fallback;
        }
        double raw = p.getAsDouble();
        if (!Double.isFinite(raw)) {
            MultiGolem.LOG.warn("field {} is not finite; using default {}", key, fallback);
            return fallback;
        }
        return raw;
    }

    private static boolean readBoolean(JsonObject obj, String key, boolean fallback) {
        if (!obj.has(key)) return fallback;
        JsonElement el = obj.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            MultiGolem.LOG.warn("field {} is not a boolean; using default {}", key, fallback);
            return fallback;
        }
        return el.getAsBoolean();
    }

    private static int clampInt(int raw, int min, int max, String name) {
        if (raw < min || raw > max) {
            int clamped = Math.max(min, Math.min(max, raw));
            MultiGolem.LOG.warn("field {} = {} outside [{}, {}]; clamped to {}", name, raw, min, max, clamped);
            return clamped;
        }
        return raw;
    }

    private static double clampDouble(double raw, double min, double max, String name) {
        if (raw < min || raw > max) {
            double clamped = Math.max(min, Math.min(max, raw));
            MultiGolem.LOG.warn("field {} = {} outside [{}, {}]; clamped to {}", name, raw, min, max, clamped);
            return clamped;
        }
        return raw;
    }

    private static void writeQuietly(Path path, MultiGolemConfig cfg) {
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = toJson(cfg);
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not write multigolem config to {}: {}", path, e.getMessage());
        }
    }

    static JsonObject toJson(MultiGolemConfig cfg) {
        JsonObject root = new JsonObject();
        root.addProperty("allow_golem_healing", cfg.allowGolemHealing);
        JsonObject tiers = new JsonObject();
        for (GolemVariant v : GolemVariant.values()) {
            TierStats s = cfg.tier(v);
            JsonObject t = new JsonObject();
            t.addProperty("max_health", s.maxHealth());
            t.addProperty("attack_damage", s.attackDamage());
            t.addProperty("anger_on_hit", s.angerOnHit());
            JsonArray ignArr = new JsonArray();
            for (String ig : s.ignoredTargetTypes()) ignArr.add(ig);
            t.add("ignored_target_types", ignArr);
            // Copper
            if (s.copperLightningImmune() != null) t.addProperty("copper_lightning_immune", s.copperLightningImmune());
            if (s.copperLightningImmune() != null) {
                if (s.copperLightningHealAmount() == null) t.add("copper_lightning_heal_amount", JsonNull.INSTANCE);
                else t.addProperty("copper_lightning_heal_amount", s.copperLightningHealAmount());
            }
            // Gold
            if (s.goldSpeedMultiplier() != null) t.addProperty("gold_speed_multiplier", s.goldSpeedMultiplier());
            if (s.goldSprintParticlesEnabled() != null) t.addProperty("gold_sprint_particles_enabled", s.goldSprintParticlesEnabled());
            if (s.goldSunlightShineEnabled() != null) t.addProperty("gold_sunlight_shine_enabled", s.goldSunlightShineEnabled());
            // Emerald
            if (s.emeraldAuraRange() != null) t.addProperty("emerald_aura_range", s.emeraldAuraRange());
            if (s.emeraldHealIntervalSeconds() != null) t.addProperty("emerald_heal_interval_seconds", s.emeraldHealIntervalSeconds());
            if (s.emeraldHealPerTick() != null) t.addProperty("emerald_heal_per_tick", s.emeraldHealPerTick());
            if (s.emeraldCountWanderingTraders() != null) t.addProperty("emerald_count_wandering_traders", s.emeraldCountWanderingTraders());
            // Diamond
            if (s.diamondTargetMode() != null) t.addProperty("diamond_target_mode", s.diamondTargetMode());
            if (s.diamondCooldownMinSeconds() != null) t.addProperty("diamond_cooldown_min_seconds", s.diamondCooldownMinSeconds());
            if (s.diamondCooldownMaxSeconds() != null) t.addProperty("diamond_cooldown_max_seconds", s.diamondCooldownMaxSeconds());
            if (s.diamondAuraRange() != null) t.addProperty("diamond_aura_range", s.diamondAuraRange());
            if (s.diamondLightningProof() != null) t.addProperty("diamond_lightning_proof", s.diamondLightningProof());
            // Netherite
            if (s.netheriteFireImmune() != null) t.addProperty("netherite_fire_immune", s.netheriteFireImmune());
            if (s.netheriteIgniteSeconds() != null) t.addProperty("netherite_ignite_seconds", s.netheriteIgniteSeconds());
            tiers.add(v.id(), t);
        }
        root.add("tiers", tiers);
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiGolemConfig that)) return false;
        return allowGolemHealing == that.allowGolemHealing && tiers.equals(that.tiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowGolemHealing, tiers);
    }
}
