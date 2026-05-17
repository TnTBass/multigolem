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
import dev.charles.multigolem.spawn.VillageSpawnWeights;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
    private final VillageSpawnWeights villageSpawnWeights;

    private MultiGolemConfig(boolean allowGolemHealing, Map<GolemVariant, TierStats> tiers, VillageSpawnWeights villageSpawnWeights) {
        this.allowGolemHealing = allowGolemHealing;
        this.tiers = new EnumMap<>(tiers);
        this.villageSpawnWeights = villageSpawnWeights;
    }

    public boolean allowGolemHealing() { return allowGolemHealing; }
    public TierStats tier(GolemVariant variant) { return tiers.get(variant); }
    public VillageSpawnWeights villageSpawnWeights() { return villageSpawnWeights; }

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
            1.75, true, true,
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
        return new MultiGolemConfig(true, m, VillageSpawnWeights.defaults());
    }

    public static MultiGolemConfig loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            MultiGolemConfig defaults = defaults();
            JsonObject defaultsJson = toJson(defaults);
            atomicWrite(path, defaultsJson);
            return defaults;
        }

        String originalContent;
        JsonObject onDisk;
        try {
            originalContent = Files.readString(path, StandardCharsets.UTF_8);
            onDisk = GSON.fromJson(originalContent, JsonObject.class);
            if (onDisk == null) {
                MultiGolem.LOG.warn("multigolem config at {} is empty; using defaults; NOT overwriting", path);
                return defaults();
            }
        } catch (JsonSyntaxException e) {
            MultiGolem.LOG.warn("multigolem config at {} is malformed; using defaults; NOT overwriting: {}",
                path, e.getMessage());
            return defaults();
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not read multigolem config at {}: {}", path, e.getMessage());
            return defaults();
        }

        JsonObject defaultsJson = toJson(defaults());
        mergeDefaultsRecursive(onDisk, defaultsJson);   // (1) fill missing keys with defaults
        canonicalizeAndValidateInPlace(onDisk);          // (2) clamp/canonicalize known fields in place
        MultiGolemConfig parsed = parse(onDisk);         // (3) hand to runtime

        String merged = GSON.toJson(onDisk);
        if (!merged.equals(originalContent)) {           // (4) compare bytes
            atomicWrite(path, onDisk);                   // (5) write if different
        }
        return parsed;
    }

    private static void mergeDefaultsRecursive(JsonObject onDisk, JsonObject defaults) {
        for (Map.Entry<String, JsonElement> e : defaults.entrySet()) {
            String key = e.getKey();
            JsonElement defVal = e.getValue();
            if (!onDisk.has(key)) {
                onDisk.add(key, defVal.deepCopy());
            } else if (defVal.isJsonObject() && onDisk.get(key).isJsonObject()) {
                mergeDefaultsRecursive(onDisk.getAsJsonObject(key), defVal.getAsJsonObject());
            }
            // else: keep on-disk value as-is
        }
        // Unknown on-disk keys NOT in defaults are preserved untouched.
    }

    private static void canonicalizeAndValidateInPlace(JsonObject root) {
        canonicalizeVillageSpawningInPlace(root);

        JsonObject tiers = root.has("tiers") && root.get("tiers").isJsonObject()
            ? root.getAsJsonObject("tiers") : null;
        if (tiers == null) return;
        for (GolemVariant v : GolemVariant.values()) {
            if (!tiers.has(v.id()) || !tiers.get(v.id()).isJsonObject()) continue;
            canonicalizeTierInPlace(v, tiers.getAsJsonObject(v.id()));
        }
    }

    private static void canonicalizeVillageSpawningInPlace(JsonObject root) {
        if (!root.has("village_spawning") || !root.get("village_spawning").isJsonObject()) {
            MultiGolem.LOG.warn("village_spawning is missing or malformed; using defaults");
            root.add("village_spawning", villageSpawnWeightsToJson(VillageSpawnWeights.defaults()));
            return;
        }

        JsonObject village = root.getAsJsonObject("village_spawning");
        if (!village.has("enabled") || !village.get("enabled").isJsonPrimitive()
                || !village.get("enabled").getAsJsonPrimitive().isBoolean()) {
            if (village.has("enabled")) {
                MultiGolem.LOG.warn("village_spawning.enabled is not a boolean; using true");
            }
            village.addProperty("enabled", true);
        }

        if (!village.has("weights") || !village.get("weights").isJsonObject()) {
            MultiGolem.LOG.warn("village_spawning.weights is missing or malformed; using defaults");
            village.add("weights", villageWeightDefaultsJson());
            return;
        }

        JsonObject weights = village.getAsJsonObject("weights");
        boolean hasRecognizedKey = false;
        for (GolemVariant variant : GolemVariant.values()) {
            if (weights.has(variant.id())) {
                hasRecognizedKey = true;
                break;
            }
        }

        for (Map.Entry<String, JsonElement> entry : weights.entrySet()) {
            if (GolemVariant.fromId(entry.getKey()).isEmpty()) {
                MultiGolem.LOG.warn("unknown village_spawning weight key '{}'; preserved but ignored", entry.getKey());
            }
        }

        VillageSpawnWeights defaults = VillageSpawnWeights.defaults();
        for (GolemVariant variant : GolemVariant.values()) {
            String key = variant.id();
            if (!weights.has(key)) {
                MultiGolem.LOG.warn("village_spawning.weights.{} missing; using default {}", key, defaults.weight(variant));
                weights.addProperty(key, defaults.weight(variant));
                continue;
            }

            JsonElement value = weights.get(key);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
                MultiGolem.LOG.warn("village_spawning.weights.{} is not a number; using default {}", key, defaults.weight(variant));
                weights.addProperty(key, defaults.weight(variant));
                continue;
            }

            int raw = value.getAsInt();
            if (raw < 0) {
                MultiGolem.LOG.warn("village_spawning.weights.{} = {} below 0; clamped to 0", key, raw);
                weights.addProperty(key, 0);
            }
        }

        if (!hasRecognizedKey) {
            MultiGolem.LOG.warn("village_spawning.weights contains no recognized variant keys; using defaults");
            for (GolemVariant variant : GolemVariant.values()) {
                weights.addProperty(variant.id(), defaults.weight(variant));
            }
        }
    }

    private static void canonicalizeTierInPlace(GolemVariant variant, JsonObject t) {
        // max_health clamp [1, 2048]
        if (t.has("max_health") && t.get("max_health").isJsonPrimitive() && t.get("max_health").getAsJsonPrimitive().isNumber()) {
            int v = t.get("max_health").getAsInt();
            int c = Math.max(TierStats.MIN_HEALTH, Math.min(TierStats.MAX_HEALTH, v));
            if (v != c) { MultiGolem.LOG.warn("max_health {} clamped to {}", v, c); t.addProperty("max_health", c); }
        }
        // attack_damage clamp [0, 2048]
        canonicalizeDouble(t, "attack_damage", TierStats.MIN_DAMAGE, TierStats.MAX_DAMAGE, Double.NaN);
        // ignored_target_types: drop unknowns
        if (t.has("ignored_target_types") && t.get("ignored_target_types").isJsonArray()) {
            JsonArray arr = t.getAsJsonArray("ignored_target_types");
            JsonArray filtered = new JsonArray();
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()
                        && RECOGNIZED_IGNORED_TARGET_TYPES.contains(el.getAsString())) {
                    filtered.add(el);
                }
            }
            t.add("ignored_target_types", filtered);
        }
        // Copper
        canonicalizeDouble(t, "copper_lightning_heal_amount", 0.0, 2048.0, Double.NaN);
        // Gold
        canonicalizeDouble(t, "gold_speed_multiplier", 0.1, 10.0, Double.NaN);
        // Emerald
        canonicalizeInt(t, "emerald_aura_range", 1, 64);
        canonicalizeDoubleMin(t, "emerald_heal_interval_seconds", 0.5);
        canonicalizeDouble(t, "emerald_heal_per_tick", 0.0, 2048.0, Double.NaN);
        // Diamond
        if (variant == GolemVariant.DIAMOND) {
            // diamond_target_mode: case-insensitive, canonical uppercase
            if (t.has("diamond_target_mode") && t.get("diamond_target_mode").isJsonPrimitive()
                    && t.get("diamond_target_mode").getAsJsonPrimitive().isString()) {
                String raw = t.get("diamond_target_mode").getAsString();
                String canonical = raw.toUpperCase(Locale.ROOT);
                if (!RECOGNIZED_DIAMOND_TARGET_MODES.contains(canonical)) {
                    MultiGolem.LOG.warn("unknown diamond_target_mode '{}'; using ALL_HOSTILE_MOBS", raw);
                    t.addProperty("diamond_target_mode", "ALL_HOSTILE_MOBS");
                } else {
                    t.addProperty("diamond_target_mode", canonical);
                }
            }
            canonicalizeInt(t, "diamond_cooldown_min_seconds", 0, 3600);
            canonicalizeInt(t, "diamond_cooldown_max_seconds", 0, 3600);
            // swap if min > max
            if (t.has("diamond_cooldown_min_seconds") && t.has("diamond_cooldown_max_seconds")
                    && t.get("diamond_cooldown_min_seconds").isJsonPrimitive()
                    && t.get("diamond_cooldown_min_seconds").getAsJsonPrimitive().isNumber()
                    && t.get("diamond_cooldown_max_seconds").isJsonPrimitive()
                    && t.get("diamond_cooldown_max_seconds").getAsJsonPrimitive().isNumber()) {
                int mn = t.get("diamond_cooldown_min_seconds").getAsInt();
                int mx = t.get("diamond_cooldown_max_seconds").getAsInt();
                if (mn > mx) {
                    MultiGolem.LOG.warn("diamond cooldown min ({}) > max ({}); swapping", mn, mx);
                    t.addProperty("diamond_cooldown_min_seconds", mx);
                    t.addProperty("diamond_cooldown_max_seconds", mn);
                }
            }
            canonicalizeInt(t, "diamond_aura_range", 1, 64);
        }
        // Netherite
        canonicalizeInt(t, "netherite_ignite_seconds", 0, 300);
    }

    private static void canonicalizeInt(JsonObject t, String key, int min, int max) {
        if (!t.has(key) || !t.get(key).isJsonPrimitive() || !t.get(key).getAsJsonPrimitive().isNumber()) return;
        int v = t.get(key).getAsInt();
        int c = Math.max(min, Math.min(max, v));
        if (v != c) { MultiGolem.LOG.warn("field {} = {} clamped to {}", key, v, c); t.addProperty(key, c); }
    }

    private static void canonicalizeDouble(JsonObject t, String key, double min, double max, double nanDefault) {
        if (!t.has(key) || t.get(key) instanceof JsonNull) return;
        JsonElement el = t.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) return;
        double v = el.getAsDouble();
        if (!Double.isFinite(v)) {
            MultiGolem.LOG.warn("field {} is non-finite; using default", key);
            return;
        }
        double c = Math.max(min, Math.min(max, v));
        if (v != c) { MultiGolem.LOG.warn("field {} = {} clamped to {}", key, v, c); t.addProperty(key, c); }
    }

    private static void canonicalizeDoubleMin(JsonObject t, String key, double min) {
        if (!t.has(key) || t.get(key) instanceof JsonNull) return;
        JsonElement el = t.get(key);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) return;
        double v = el.getAsDouble();
        if (!Double.isFinite(v)) return;
        if (v < min) { MultiGolem.LOG.warn("field {} = {} below min {}; clamped", key, v, min); t.addProperty(key, min); }
    }

    private static void atomicWrite(Path target, JsonObject root) {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp." + UUID.randomUUID());
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException fallbackEx) {
                    try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
                    MultiGolem.LOG.warn("could not write multigolem config at {} (atomic move unsupported, non-atomic fallback also failed): {}", target, fallbackEx.getMessage());
                }
            }
        } catch (IOException e) {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            MultiGolem.LOG.warn("failed to write multigolem config at {}: {}", target, e.getMessage());
        }
    }

    static MultiGolemConfig parse(JsonObject root) {
        MultiGolemConfig defaults = defaults();
        boolean healing = readBoolean(root, "allow_golem_healing", defaults.allowGolemHealing);
        VillageSpawnWeights villageSpawnWeights = parseVillageSpawnWeights(root, defaults.villageSpawnWeights);

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
        return new MultiGolemConfig(healing, tiers, villageSpawnWeights);
    }

    private static VillageSpawnWeights parseVillageSpawnWeights(JsonObject root, VillageSpawnWeights fallback) {
        if (!root.has("village_spawning") || !root.get("village_spawning").isJsonObject()) {
            return fallback;
        }

        JsonObject village = root.getAsJsonObject("village_spawning");
        boolean enabled = readBoolean(village, "enabled", fallback.enabled());
        JsonObject weightsJson = village.has("weights") && village.get("weights").isJsonObject()
            ? village.getAsJsonObject("weights")
            : null;
        if (weightsJson == null) return fallback.withEnabled(enabled);

        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        for (GolemVariant variant : GolemVariant.values()) {
            weights.put(variant, readInt(weightsJson, variant.id(), fallback.weight(variant)));
        }
        return new VillageSpawnWeights(enabled, weights);
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

    static JsonObject toJson(MultiGolemConfig cfg) {
        JsonObject root = new JsonObject();
        root.addProperty("allow_golem_healing", cfg.allowGolemHealing);
        root.add("village_spawning", villageSpawnWeightsToJson(cfg.villageSpawnWeights));
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

    private static JsonObject villageSpawnWeightsToJson(VillageSpawnWeights weights) {
        JsonObject village = new JsonObject();
        village.addProperty("enabled", weights.enabled());
        JsonObject weightsJson = new JsonObject();
        for (GolemVariant variant : GolemVariant.values()) {
            weightsJson.addProperty(variant.id(), weights.weight(variant));
        }
        village.add("weights", weightsJson);
        return village;
    }

    private static JsonObject villageWeightDefaultsJson() {
        return villageSpawnWeightsToJson(VillageSpawnWeights.defaults()).getAsJsonObject("weights");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiGolemConfig that)) return false;
        return allowGolemHealing == that.allowGolemHealing
            && tiers.equals(that.tiers)
            && villageSpawnWeights.equals(that.villageSpawnWeights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowGolemHealing, tiers, villageSpawnWeights);
    }
}
