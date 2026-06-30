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
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.spawn.VillageSpawnWeights;
import dev.charles.multigolem.spawn.ZombieVillageSpawningConfig;
import net.minecraft.resources.Identifier;

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
    private static final String VILLAGE_SPAWNING_NOTE =
        "Keep in mind, villages are made of wood and netherite golems start fires.";
    private static final List<String> DEFAULT_LAPIS_WARD_EFFECT_IDS = List.of(
        "minecraft:poison",
        "minecraft:wither",
        "minecraft:weakness",
        "minecraft:slowness",
        "minecraft:blindness",
        "minecraft:nausea",
        "minecraft:levitation",
        "minecraft:darkness",
        "minecraft:mining_fatigue"
    );

    private final boolean allowGolemHealing;
    private final Map<GolemVariant, TierStats> tiers;
    private final VillageSpawnWeights villageSpawnWeights;
    private final ZombieVillageSpawningConfig zombieVillageSpawning;
    private final GolemAvailability golemAvailability;

    private MultiGolemConfig(boolean allowGolemHealing, Map<GolemVariant, TierStats> tiers, VillageSpawnWeights villageSpawnWeights,
                             ZombieVillageSpawningConfig zombieVillageSpawning, GolemAvailability golemAvailability) {
        this.allowGolemHealing = allowGolemHealing;
        this.tiers = new EnumMap<>(tiers);
        this.villageSpawnWeights = villageSpawnWeights;
        this.zombieVillageSpawning = zombieVillageSpawning;
        this.golemAvailability = golemAvailability;
    }

    public boolean allowGolemHealing() { return allowGolemHealing; }
    public TierStats tier(GolemVariant variant) { return tiers.get(variant); }
    public VillageSpawnWeights villageSpawnWeights() { return villageSpawnWeights; }
    public ZombieVillageSpawningConfig zombieVillageSpawning() { return zombieVillageSpawning; }
    public GolemAvailability golemAvailability() { return golemAvailability; }

    Map<GolemVariant, TierStats> tiersForTesting() {
        return new EnumMap<>(tiers);
    }

    static MultiGolemConfig forTesting(
        boolean allowGolemHealing,
        Map<GolemVariant, TierStats> tiers,
        VillageSpawnWeights villageSpawnWeights,
        ZombieVillageSpawningConfig zombieVillageSpawning
    ) {
        return forTesting(allowGolemHealing, tiers, villageSpawnWeights, zombieVillageSpawning, GolemAvailability.defaults());
    }

    static MultiGolemConfig forTesting(
        boolean allowGolemHealing,
        Map<GolemVariant, TierStats> tiers,
        VillageSpawnWeights villageSpawnWeights,
        ZombieVillageSpawningConfig zombieVillageSpawning,
        GolemAvailability golemAvailability
    ) {
        return new MultiGolemConfig(allowGolemHealing, tiers, villageSpawnWeights, zombieVillageSpawning, golemAvailability);
    }

    public MultiGolemConfig withGolemAvailability(GolemAvailability golemAvailability) {
        return new MultiGolemConfig(allowGolemHealing, tiers, villageSpawnWeights, zombieVillageSpawning, golemAvailability);
    }

    public static MultiGolemConfig defaults() {
        EnumMap<GolemVariant, TierStats> m = new EnumMap<>(GolemVariant.class);
        m.put(GolemVariant.COPPER, new TierStats(60, 8.5, true, List.of("CREEPERS"),
            true, null,  // copper
            null, null, null,  // gold
            null, null, null, null,  // emerald
            null, null, null, null, null,  // diamond
            null, null, null));  // netherite
        m.put(GolemVariant.IRON, new TierStats(100, 15.0, true, List.of(),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null));
        m.put(GolemVariant.REDSTONE, new TierStats(90, 13.0, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null,
            null, null, null, null,
            true, 0.25, 12.0, 45.0, 1.5, 1, 3.0,
            true, 8, 6.0, 9, true, true,
            null, null, null, null, null, null, null, null));
        m.put(GolemVariant.GOLD, new TierStats(130, 22.5, true, List.of("CREEPERS"),
            null, null,
            1.75, true, true,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null));
        m.put(GolemVariant.LAPIS, new TierStats(50, 7.5, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null,
            null, null, null, null,
            null, null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null,
            true, 15, 5, false, true, true, DEFAULT_LAPIS_WARD_EFFECT_IDS, true));
        m.put(GolemVariant.EMERALD, new TierStats(200, 40.0, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            8, 2.0, 1.0, true,
            null, null, null, null, null,
            null, null, null));
        m.put(GolemVariant.DIAMOND, new TierStats(350, 62.5, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            null, null, null, null,
            "ALL_HOSTILE_MOBS", 30, 60, 12, true,
            null, null, null));
        m.put(GolemVariant.NETHERITE, new TierStats(600, 85.0, true, List.of("CREEPERS"),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            true, 5, 0));
        m.put(GolemVariant.ZOMBIE, new TierStats(100, 15.0, true, List.of(),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null,
            25.0, true, 12, 0,
            true, 4, 0,
            true, 4, 0,
            true, 1.0, true, 1.0));
        return new MultiGolemConfig(true, m, VillageSpawnWeights.defaults(), ZombieVillageSpawningConfig.defaults(), GolemAvailability.defaults());
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
        canonicalizeZombieVillageSpawningInPlace(root);
        canonicalizeGolemAvailabilityInPlace(root);

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
        for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
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
        if (!hasRecognizedKey) {
            MultiGolem.LOG.warn("village_spawning.weights contains no recognized variant keys; using defaults");
            for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
                weights.addProperty(variant.id(), defaults.weight(variant));
            }
            return;
        }

        for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
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
        if (variant == GolemVariant.NETHERITE) {
            canonicalizeInt(t, "netherite_ignite_seconds", 0, 300);
            canonicalizeInt(t, "netherite_village_ignite_seconds", 0, 300);
        }
        if (variant == GolemVariant.ZOMBIE) {
            canonicalizeDouble(t, "zombie_rotten_flesh_heal_amount", 0.0, 2048.0, Double.NaN);
            canonicalizeInt(t, "zombie_hunger_seconds", 0, 300);
            canonicalizeInt(t, "zombie_hunger_amplifier", 0, 255);
            canonicalizeInt(t, "zombie_nausea_seconds", 0, 300);
            canonicalizeInt(t, "zombie_nausea_amplifier", 0, 255);
            canonicalizeInt(t, "zombie_poison_seconds", 0, 300);
            canonicalizeInt(t, "zombie_poison_amplifier", 0, 255);
            canonicalizeDouble(t, "zombie_villager_conversion_chance", 0.0, 1.0, Double.NaN);
            canonicalizeDouble(t, "zombie_wandering_trader_conversion_chance", 0.0, 1.0, Double.NaN);
        }
        if (variant == GolemVariant.REDSTONE) {
            canonicalizeDouble(t, "redstone_overcharge_health_threshold_percent", 0.01, 1.0, Double.NaN);
            canonicalizeDouble(t, "redstone_overcharge_duration_seconds", 0.0, 3600.0, Double.NaN);
            canonicalizeDouble(t, "redstone_overcharge_cooldown_seconds", 0.0, 3600.0, Double.NaN);
            canonicalizeDouble(t, "redstone_overcharge_attack_multiplier", 1.0, 10.0, Double.NaN);
            canonicalizeInt(t, "redstone_overcharge_resistance_amplifier", 0, 4);
            canonicalizeDoubleMin(t, "redstone_overcharge_resistance_refresh_seconds", 0.5);
            canonicalizeInt(t, "redstone_death_pulse_radius", 1, 64);
            canonicalizeDouble(t, "redstone_death_pulse_slowness_seconds", 0.0, 3600.0, Double.NaN);
            canonicalizeInt(t, "redstone_death_pulse_slowness_amplifier", 0, 9);
        }
        if (variant == GolemVariant.LAPIS) {
            canonicalizeInt(t, "lapis_ward_range", 1, 64);
            canonicalizeInt(t, "lapis_ward_scan_interval_ticks", 1, 200);
        }
    }

    private static void canonicalizeZombieVillageSpawningInPlace(JsonObject root) {
        if (!root.has("zombie_village_spawning") || !root.get("zombie_village_spawning").isJsonObject()) {
            root.add("zombie_village_spawning", zombieVillageSpawningToJson(ZombieVillageSpawningConfig.defaults()));
            return;
        }

        JsonObject zombie = root.getAsJsonObject("zombie_village_spawning");
        ZombieVillageSpawningConfig defaults = ZombieVillageSpawningConfig.defaults();
        if (!zombie.has("enabled") || !zombie.get("enabled").isJsonPrimitive()
                || !zombie.get("enabled").getAsJsonPrimitive().isBoolean()) {
            zombie.addProperty("enabled", defaults.enabled());
        }
        canonicalizeInt(zombie, "min_zombie_villagers", 1, Integer.MAX_VALUE);
        canonicalizeInt(zombie, "zombie_villagers_per_golem", 1, Integer.MAX_VALUE);
        if (!zombie.has("regular_zombie_bonus_enabled") || !zombie.get("regular_zombie_bonus_enabled").isJsonPrimitive()
                || !zombie.get("regular_zombie_bonus_enabled").getAsJsonPrimitive().isBoolean()) {
            zombie.addProperty("regular_zombie_bonus_enabled", defaults.regularZombieBonusEnabled());
        }
        canonicalizeInt(zombie, "regular_zombie_bonus_threshold", 1, Integer.MAX_VALUE);
        canonicalizeInt(zombie, "max_zombie_golems_per_village", 0, Integer.MAX_VALUE);
    }

    private static void canonicalizeGolemAvailabilityInPlace(JsonObject root) {
        if (!root.has("golem_availability") || !root.get("golem_availability").isJsonObject()) {
            if (root.has("golem_availability")) {
                MultiGolem.LOG.warn("golem_availability is malformed; using defaults");
            }
            root.add("golem_availability", golemAvailabilityToJson(GolemAvailability.defaults()));
            return;
        }

        JsonObject availability = root.getAsJsonObject("golem_availability");
        for (Map.Entry<String, JsonElement> entry : availability.entrySet()) {
            String familyKey = entry.getKey();
            GolemFamily family = GolemFamily.fromId(familyKey).orElse(null);
            if (family == null) {
                MultiGolem.LOG.warn("unknown golem_availability family key '{}'; preserved but ignored", familyKey);
                warnUnknownAvailabilityVariants(familyKey, entry.getValue());
                continue;
            }
            if (!entry.getValue().isJsonObject()) {
                MultiGolem.LOG.warn("golem_availability.{} is malformed; using defaults", familyKey);
                entry.setValue(familyAvailabilityToJson(GolemAvailability.familyDefault(family)));
                continue;
            }
            canonicalizeFamilyAvailabilityInPlace(familyKey, family, entry.getValue().getAsJsonObject());
        }

        for (GolemFamily family : GolemFamily.values()) {
            String familyKey = family.id();
            if (!availability.has(familyKey) || !availability.get(familyKey).isJsonObject()) {
                availability.add(familyKey, familyAvailabilityToJson(GolemAvailability.familyDefault(family)));
            }
        }
    }

    private static void canonicalizeFamilyAvailabilityInPlace(String familyKey, GolemFamily family, JsonObject familyJson) {
        boolean defaultEnabled = GolemAvailability.familyDefault(family).enabled();
        if (!familyJson.has("enabled") || !familyJson.get("enabled").isJsonPrimitive()
                || !familyJson.get("enabled").getAsJsonPrimitive().isBoolean()) {
            if (familyJson.has("enabled")) {
                MultiGolem.LOG.warn("golem_availability.{}.enabled is not a boolean; using {}", familyKey, defaultEnabled);
            }
            familyJson.addProperty("enabled", defaultEnabled);
        }

        if (!familyJson.has("variants") || !familyJson.get("variants").isJsonObject()) {
            if (familyJson.has("variants")) {
                MultiGolem.LOG.warn("golem_availability.{}.variants is malformed; using defaults", familyKey);
            }
            familyJson.add("variants", new JsonObject());
        }

        JsonObject variants = familyJson.getAsJsonObject("variants");
        for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
            String variantKey = entry.getKey();
            GolemVariant variant = GolemVariant.fromId(variantKey).orElse(null);
            if (variant == null || !GolemVariantCatalog.contains(family, variant)) {
                MultiGolem.LOG.warn("unknown golem_availability.{}.variants key '{}'; preserved but ignored", familyKey, variantKey);
                continue;
            }
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
                MultiGolem.LOG.warn("golem_availability.{}.variants.{} is not a boolean; using true", familyKey, variantKey);
                entry.setValue(new JsonPrimitive(true));
            }
        }

        for (Map.Entry<GolemVariant, Boolean> entry : GolemAvailability.familyDefault(family).variants().entrySet()) {
            if (!variants.has(entry.getKey().id())) {
                variants.addProperty(entry.getKey().id(), entry.getValue());
            }
        }
    }

    private static void warnUnknownAvailabilityVariants(String familyKey, JsonElement familyElement) {
        if (!familyElement.isJsonObject()) return;
        JsonObject familyJson = familyElement.getAsJsonObject();
        if (!familyJson.has("variants") || !familyJson.get("variants").isJsonObject()) return;
        for (Map.Entry<String, JsonElement> variantEntry : familyJson.getAsJsonObject("variants").entrySet()) {
            MultiGolem.LOG.warn("unknown golem_availability.{}.variants key '{}'; preserved but ignored", familyKey, variantEntry.getKey());
        }
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
        ZombieVillageSpawningConfig zombieVillageSpawning = parseZombieVillageSpawning(root, defaults.zombieVillageSpawning);
        GolemAvailability golemAvailability = parseGolemAvailability(root);

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
        return new MultiGolemConfig(healing, tiers, villageSpawnWeights, zombieVillageSpawning, golemAvailability);
    }

    private static GolemAvailability parseGolemAvailability(JsonObject root) {
        if (!root.has("golem_availability") || !root.get("golem_availability").isJsonObject()) {
            return GolemAvailability.defaults();
        }

        GolemAvailability availability = GolemAvailability.defaults();
        JsonObject availabilityJson = root.getAsJsonObject("golem_availability");
        for (GolemFamily family : GolemFamily.values()) {
            String familyKey = family.id();
            if (!availabilityJson.has(familyKey) || !availabilityJson.get(familyKey).isJsonObject()) {
                continue;
            }

            JsonObject familyJson = availabilityJson.getAsJsonObject(familyKey);
            boolean enabled = true;
            if (familyJson.has("enabled") && familyJson.get("enabled").isJsonPrimitive()
                    && familyJson.get("enabled").getAsJsonPrimitive().isBoolean()) {
                enabled = familyJson.get("enabled").getAsBoolean();
            }
            availability = availability.withFamily(family, enabled);

            if (!familyJson.has("variants") || !familyJson.get("variants").isJsonObject()) {
                continue;
            }
            JsonObject variants = familyJson.getAsJsonObject("variants");
            for (GolemVariant variant : GolemVariant.values()) {
                String variantKey = variant.id();
                if (!GolemVariantCatalog.contains(family, variant)
                        || !variants.has(variantKey)
                        || !variants.get(variantKey).isJsonPrimitive()
                        || !variants.get(variantKey).getAsJsonPrimitive().isBoolean()) {
                    continue;
                }
                availability = availability.withVariant(family, variant, variants.get(variantKey).getAsBoolean());
            }
        }
        return availability;
    }

    private static ZombieVillageSpawningConfig parseZombieVillageSpawning(JsonObject root, ZombieVillageSpawningConfig fallback) {
        if (!root.has("zombie_village_spawning") || !root.get("zombie_village_spawning").isJsonObject()) {
            return fallback;
        }
        JsonObject zombie = root.getAsJsonObject("zombie_village_spawning");
        return new ZombieVillageSpawningConfig(
            readBoolean(zombie, "enabled", fallback.enabled()),
            clampInt(readInt(zombie, "min_zombie_villagers", fallback.minZombieVillagers()), 1, Integer.MAX_VALUE, "min_zombie_villagers"),
            clampInt(readInt(zombie, "zombie_villagers_per_golem", fallback.zombieVillagersPerGolem()), 1, Integer.MAX_VALUE, "zombie_villagers_per_golem"),
            readBoolean(zombie, "regular_zombie_bonus_enabled", fallback.regularZombieBonusEnabled()),
            clampInt(readInt(zombie, "regular_zombie_bonus_threshold", fallback.regularZombieBonusThreshold()), 1, Integer.MAX_VALUE, "regular_zombie_bonus_threshold"),
            clampInt(readInt(zombie, "max_zombie_golems_per_village", fallback.maxZombieGolemsPerVillage()), 0, Integer.MAX_VALUE, "max_zombie_golems_per_village")
        );
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
        for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
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
        Integer netheriteVillageIgnite = def.netheriteVillageIgniteSeconds() != null
            ? clampInt(readInt(t, "netherite_village_ignite_seconds", def.netheriteVillageIgniteSeconds()), 0, 300, "netherite_village_ignite_seconds")
            : null;
        Double zombieHeal = def.zombieRottenFleshHealAmount() != null
            ? clampDouble(readDouble(t, "zombie_rotten_flesh_heal_amount", def.zombieRottenFleshHealAmount()), 0.0, 2048.0, "zombie_rotten_flesh_heal_amount")
            : null;
        Boolean zombieHungerEnabled = def.zombieHungerEnabled() != null
            ? readBoolean(t, "zombie_hunger_enabled", def.zombieHungerEnabled())
            : null;
        Integer zombieHungerSeconds = def.zombieHungerSeconds() != null
            ? clampInt(readInt(t, "zombie_hunger_seconds", def.zombieHungerSeconds()), 0, 300, "zombie_hunger_seconds")
            : null;
        Integer zombieHungerAmplifier = def.zombieHungerAmplifier() != null
            ? clampInt(readInt(t, "zombie_hunger_amplifier", def.zombieHungerAmplifier()), 0, 255, "zombie_hunger_amplifier")
            : null;
        Boolean zombieNauseaEnabled = def.zombieNauseaEnabled() != null
            ? readBoolean(t, "zombie_nausea_enabled", def.zombieNauseaEnabled())
            : null;
        Integer zombieNauseaSeconds = def.zombieNauseaSeconds() != null
            ? clampInt(readInt(t, "zombie_nausea_seconds", def.zombieNauseaSeconds()), 0, 300, "zombie_nausea_seconds")
            : null;
        Integer zombieNauseaAmplifier = def.zombieNauseaAmplifier() != null
            ? clampInt(readInt(t, "zombie_nausea_amplifier", def.zombieNauseaAmplifier()), 0, 255, "zombie_nausea_amplifier")
            : null;
        Boolean zombiePoisonEnabled = def.zombiePoisonEnabled() != null
            ? readBoolean(t, "zombie_poison_enabled", def.zombiePoisonEnabled())
            : null;
        Integer zombiePoisonSeconds = def.zombiePoisonSeconds() != null
            ? clampInt(readInt(t, "zombie_poison_seconds", def.zombiePoisonSeconds()), 0, 300, "zombie_poison_seconds")
            : null;
        Integer zombiePoisonAmplifier = def.zombiePoisonAmplifier() != null
            ? clampInt(readInt(t, "zombie_poison_amplifier", def.zombiePoisonAmplifier()), 0, 255, "zombie_poison_amplifier")
            : null;
        Boolean zombieConvertVillagersEnabled = def.zombieConvertVillagersEnabled() != null
            ? readBoolean(t, "zombie_convert_villagers_enabled", def.zombieConvertVillagersEnabled())
            : null;
        Double zombieVillagerChance = def.zombieVillagerConversionChance() != null
            ? clampDouble(readDouble(t, "zombie_villager_conversion_chance", def.zombieVillagerConversionChance()), 0.0, 1.0, "zombie_villager_conversion_chance")
            : null;
        Boolean zombieConvertWanderingTradersEnabled = def.zombieConvertWanderingTradersEnabled() != null
            ? readBoolean(t, "zombie_convert_wandering_traders_enabled", def.zombieConvertWanderingTradersEnabled())
            : null;
        Double zombieWanderingTraderChance = def.zombieWanderingTraderConversionChance() != null
            ? clampDouble(readDouble(t, "zombie_wandering_trader_conversion_chance", def.zombieWanderingTraderConversionChance()), 0.0, 1.0, "zombie_wandering_trader_conversion_chance")
            : null;
        Boolean redstoneOverchargeEnabled = def.redstoneOverchargeEnabled() != null
            ? readBoolean(t, "redstone_overcharge_enabled", def.redstoneOverchargeEnabled())
            : null;
        Double redstoneThreshold = def.redstoneOverchargeHealthThresholdPercent() != null
            ? clampDouble(readDouble(t, "redstone_overcharge_health_threshold_percent", def.redstoneOverchargeHealthThresholdPercent()), 0.01, 1.0, "redstone_overcharge_health_threshold_percent")
            : null;
        Double redstoneDuration = def.redstoneOverchargeDurationSeconds() != null
            ? clampDouble(readDouble(t, "redstone_overcharge_duration_seconds", def.redstoneOverchargeDurationSeconds()), 0.0, 3600.0, "redstone_overcharge_duration_seconds")
            : null;
        Double redstoneCooldown = def.redstoneOverchargeCooldownSeconds() != null
            ? clampDouble(readDouble(t, "redstone_overcharge_cooldown_seconds", def.redstoneOverchargeCooldownSeconds()), 0.0, 3600.0, "redstone_overcharge_cooldown_seconds")
            : null;
        Double redstoneAttackMultiplier = def.redstoneOverchargeAttackMultiplier() != null
            ? clampDouble(readDouble(t, "redstone_overcharge_attack_multiplier", def.redstoneOverchargeAttackMultiplier()), 1.0, 10.0, "redstone_overcharge_attack_multiplier")
            : null;
        Integer redstoneResistanceAmplifier = def.redstoneOverchargeResistanceAmplifier() != null
            ? clampInt(readInt(t, "redstone_overcharge_resistance_amplifier", def.redstoneOverchargeResistanceAmplifier()), 0, 4, "redstone_overcharge_resistance_amplifier")
            : null;
        Double redstoneResistanceRefresh = def.redstoneOverchargeResistanceRefreshSeconds() != null
            ? Math.max(0.5, readDouble(t, "redstone_overcharge_resistance_refresh_seconds", def.redstoneOverchargeResistanceRefreshSeconds()))
            : null;
        Boolean redstoneDeathPulseEnabled = def.redstoneDeathPulseEnabled() != null
            ? readBoolean(t, "redstone_death_pulse_enabled", def.redstoneDeathPulseEnabled())
            : null;
        Integer redstoneDeathPulseRadius = def.redstoneDeathPulseRadius() != null
            ? clampInt(readInt(t, "redstone_death_pulse_radius", def.redstoneDeathPulseRadius()), 1, 64, "redstone_death_pulse_radius")
            : null;
        Double redstoneSlownessSeconds = def.redstoneDeathPulseSlownessSeconds() != null
            ? clampDouble(readDouble(t, "redstone_death_pulse_slowness_seconds", def.redstoneDeathPulseSlownessSeconds()), 0.0, 3600.0, "redstone_death_pulse_slowness_seconds")
            : null;
        Integer redstoneSlownessAmplifier = def.redstoneDeathPulseSlownessAmplifier() != null
            ? clampInt(readInt(t, "redstone_death_pulse_slowness_amplifier", def.redstoneDeathPulseSlownessAmplifier()), 0, 9, "redstone_death_pulse_slowness_amplifier")
            : null;
        Boolean redstoneParticlesEnabled = def.redstoneParticlesEnabled() != null
            ? readBoolean(t, "redstone_particles_enabled", def.redstoneParticlesEnabled())
            : null;
        Boolean redstoneDeathPulseParticlesEnabled = def.redstoneDeathPulseParticlesEnabled() != null
            ? readBoolean(t, "redstone_death_pulse_particles_enabled", def.redstoneDeathPulseParticlesEnabled())
            : null;
        Boolean lapisWardEnabled = def.lapisWardEnabled() != null
            ? readBoolean(t, "lapis_ward_enabled", def.lapisWardEnabled())
            : null;
        Integer lapisWardRange = def.lapisWardRange() != null
            ? clampInt(readInt(t, "lapis_ward_range", def.lapisWardRange()), 1, 64, "lapis_ward_range")
            : null;
        Integer lapisScanInterval = def.lapisWardScanIntervalTicks() != null
            ? clampInt(readInt(t, "lapis_ward_scan_interval_ticks", def.lapisWardScanIntervalTicks()), 1, 200, "lapis_ward_scan_interval_ticks")
            : null;
        Boolean lapisPlayers = def.lapisWardAffectsPlayers() != null
            ? readBoolean(t, "lapis_ward_affects_players", def.lapisWardAffectsPlayers())
            : null;
        Boolean lapisMagicDamage = def.lapisWardMagicDamageEnabled() != null
            ? readBoolean(t, "lapis_ward_magic_damage_enabled", def.lapisWardMagicDamageEnabled())
            : null;
        Boolean lapisEffectCleanup = def.lapisWardEffectCleanupEnabled() != null
            ? readBoolean(t, "lapis_ward_effect_cleanup_enabled", def.lapisWardEffectCleanupEnabled())
            : null;
        List<String> lapisEffectIds = def.lapisWardEffectIds() != null
            ? parseStringList(t, "lapis_ward_effect_ids", def.lapisWardEffectIds())
            : null;
        Boolean lapisParticles = def.lapisParticlesEnabled() != null
            ? readBoolean(t, "lapis_particles_enabled", def.lapisParticlesEnabled())
            : null;

        return new TierStats(health, damage, anger, ignored,
            copperImmune, copperHeal,
            goldSpeed, goldSprint, goldShine,
            emeraldRange, emeraldInterval, emeraldHeal, emeraldWandering,
            diamondMode, diamondMin, diamondMax, diamondRange, diamondProof,
            netheriteImmune, netheriteIgnite, netheriteVillageIgnite,
            zombieHeal, zombieHungerEnabled, zombieHungerSeconds, zombieHungerAmplifier,
            zombieNauseaEnabled, zombieNauseaSeconds, zombieNauseaAmplifier,
            zombiePoisonEnabled, zombiePoisonSeconds, zombiePoisonAmplifier,
            zombieConvertVillagersEnabled, zombieVillagerChance,
            zombieConvertWanderingTradersEnabled, zombieWanderingTraderChance,
            redstoneOverchargeEnabled, redstoneThreshold, redstoneDuration,
            redstoneCooldown, redstoneAttackMultiplier, redstoneResistanceAmplifier,
            redstoneResistanceRefresh, redstoneDeathPulseEnabled,
            redstoneDeathPulseRadius, redstoneSlownessSeconds,
            redstoneSlownessAmplifier, redstoneParticlesEnabled,
            redstoneDeathPulseParticlesEnabled,
            lapisWardEnabled, lapisWardRange, lapisScanInterval, lapisPlayers,
            lapisMagicDamage, lapisEffectCleanup, lapisEffectIds, lapisParticles);
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

    private static List<String> parseStringList(JsonObject t, String key, List<String> fallback) {
        if (!t.has(key) || !t.get(key).isJsonArray()) {
            return fallback;
        }
        JsonArray arr = t.getAsJsonArray(key);
        List<String> result = new ArrayList<>();
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                String value = e.getAsString();
                if (isNamespacedResourceId(value)) {
                    result.add(value);
                } else {
                    MultiGolem.LOG.warn("{} value '{}' is not a namespaced resource id; skipped", key, value);
                }
            } else {
                MultiGolem.LOG.warn("{} value '{}' is not a string; skipped", key, e);
            }
        }
        return List.copyOf(result);
    }

    private static boolean isNamespacedResourceId(String value) {
        return value != null && value.contains(":") && Identifier.tryParse(value) != null;
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
        root.add("golem_availability", golemAvailabilityToJson(cfg.golemAvailability));
        root.add("village_spawning", villageSpawnWeightsToJson(cfg.villageSpawnWeights));
        root.add("zombie_village_spawning", zombieVillageSpawningToJson(cfg.zombieVillageSpawning));
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
            if (s.netheriteVillageIgniteSeconds() != null) t.addProperty("netherite_village_ignite_seconds", s.netheriteVillageIgniteSeconds());
            // Zombie
            if (s.zombieRottenFleshHealAmount() != null) t.addProperty("zombie_rotten_flesh_heal_amount", s.zombieRottenFleshHealAmount());
            if (s.zombieHungerEnabled() != null) t.addProperty("zombie_hunger_enabled", s.zombieHungerEnabled());
            if (s.zombieHungerSeconds() != null) t.addProperty("zombie_hunger_seconds", s.zombieHungerSeconds());
            if (s.zombieHungerAmplifier() != null) t.addProperty("zombie_hunger_amplifier", s.zombieHungerAmplifier());
            if (s.zombieNauseaEnabled() != null) t.addProperty("zombie_nausea_enabled", s.zombieNauseaEnabled());
            if (s.zombieNauseaSeconds() != null) t.addProperty("zombie_nausea_seconds", s.zombieNauseaSeconds());
            if (s.zombieNauseaAmplifier() != null) t.addProperty("zombie_nausea_amplifier", s.zombieNauseaAmplifier());
            if (s.zombiePoisonEnabled() != null) t.addProperty("zombie_poison_enabled", s.zombiePoisonEnabled());
            if (s.zombiePoisonSeconds() != null) t.addProperty("zombie_poison_seconds", s.zombiePoisonSeconds());
            if (s.zombiePoisonAmplifier() != null) t.addProperty("zombie_poison_amplifier", s.zombiePoisonAmplifier());
            if (s.zombieConvertVillagersEnabled() != null) t.addProperty("zombie_convert_villagers_enabled", s.zombieConvertVillagersEnabled());
            if (s.zombieVillagerConversionChance() != null) t.addProperty("zombie_villager_conversion_chance", s.zombieVillagerConversionChance());
            if (s.zombieConvertWanderingTradersEnabled() != null) t.addProperty("zombie_convert_wandering_traders_enabled", s.zombieConvertWanderingTradersEnabled());
            if (s.zombieWanderingTraderConversionChance() != null) t.addProperty("zombie_wandering_trader_conversion_chance", s.zombieWanderingTraderConversionChance());
            // Redstone
            if (s.redstoneOverchargeEnabled() != null) t.addProperty("redstone_overcharge_enabled", s.redstoneOverchargeEnabled());
            if (s.redstoneOverchargeHealthThresholdPercent() != null) t.addProperty("redstone_overcharge_health_threshold_percent", s.redstoneOverchargeHealthThresholdPercent());
            if (s.redstoneOverchargeDurationSeconds() != null) t.addProperty("redstone_overcharge_duration_seconds", s.redstoneOverchargeDurationSeconds());
            if (s.redstoneOverchargeCooldownSeconds() != null) t.addProperty("redstone_overcharge_cooldown_seconds", s.redstoneOverchargeCooldownSeconds());
            if (s.redstoneOverchargeAttackMultiplier() != null) t.addProperty("redstone_overcharge_attack_multiplier", s.redstoneOverchargeAttackMultiplier());
            if (s.redstoneOverchargeResistanceAmplifier() != null) t.addProperty("redstone_overcharge_resistance_amplifier", s.redstoneOverchargeResistanceAmplifier());
            if (s.redstoneOverchargeResistanceRefreshSeconds() != null) t.addProperty("redstone_overcharge_resistance_refresh_seconds", s.redstoneOverchargeResistanceRefreshSeconds());
            if (s.redstoneDeathPulseEnabled() != null) t.addProperty("redstone_death_pulse_enabled", s.redstoneDeathPulseEnabled());
            if (s.redstoneDeathPulseRadius() != null) t.addProperty("redstone_death_pulse_radius", s.redstoneDeathPulseRadius());
            if (s.redstoneDeathPulseSlownessSeconds() != null) t.addProperty("redstone_death_pulse_slowness_seconds", s.redstoneDeathPulseSlownessSeconds());
            if (s.redstoneDeathPulseSlownessAmplifier() != null) t.addProperty("redstone_death_pulse_slowness_amplifier", s.redstoneDeathPulseSlownessAmplifier());
            if (s.redstoneParticlesEnabled() != null) t.addProperty("redstone_particles_enabled", s.redstoneParticlesEnabled());
            if (s.redstoneDeathPulseParticlesEnabled() != null) t.addProperty("redstone_death_pulse_particles_enabled", s.redstoneDeathPulseParticlesEnabled());
            // Lapis
            if (s.lapisWardEnabled() != null) t.addProperty("lapis_ward_enabled", s.lapisWardEnabled());
            if (s.lapisWardRange() != null) t.addProperty("lapis_ward_range", s.lapisWardRange());
            if (s.lapisWardScanIntervalTicks() != null) t.addProperty("lapis_ward_scan_interval_ticks", s.lapisWardScanIntervalTicks());
            if (s.lapisWardAffectsPlayers() != null) t.addProperty("lapis_ward_affects_players", s.lapisWardAffectsPlayers());
            if (s.lapisWardMagicDamageEnabled() != null) t.addProperty("lapis_ward_magic_damage_enabled", s.lapisWardMagicDamageEnabled());
            if (s.lapisWardEffectCleanupEnabled() != null) t.addProperty("lapis_ward_effect_cleanup_enabled", s.lapisWardEffectCleanupEnabled());
            if (s.lapisWardEffectIds() != null) {
                JsonArray effects = new JsonArray();
                for (String effect : s.lapisWardEffectIds()) effects.add(effect);
                t.add("lapis_ward_effect_ids", effects);
            }
            if (s.lapisParticlesEnabled() != null) t.addProperty("lapis_particles_enabled", s.lapisParticlesEnabled());
            tiers.add(v.id(), t);
        }
        root.add("tiers", tiers);
        return root;
    }

    private static JsonObject villageSpawnWeightsToJson(VillageSpawnWeights weights) {
        JsonObject village = new JsonObject();
        village.addProperty("_note", VILLAGE_SPAWNING_NOTE);
        village.addProperty("enabled", weights.enabled());
        JsonObject weightsJson = new JsonObject();
        for (GolemVariant variant : VillageSpawnWeights.rollOrder()) {
            weightsJson.addProperty(variant.id(), weights.weight(variant));
        }
        village.add("weights", weightsJson);
        return village;
    }

    private static JsonObject golemAvailabilityToJson(GolemAvailability availability) {
        JsonObject root = new JsonObject();
        Map<GolemFamily, GolemAvailability.FamilyAvailability> families = availability.knownFamilies();
        for (GolemFamily family : GolemFamily.values()) {
            root.add(family.id(), familyAvailabilityToJson(
                families.getOrDefault(family, GolemAvailability.familyDefault(family))));
        }
        return root;
    }

    private static JsonObject familyAvailabilityToJson(GolemAvailability.FamilyAvailability availability) {
        JsonObject familyJson = new JsonObject();
        familyJson.addProperty("enabled", availability.enabled());
        JsonObject variantsJson = new JsonObject();
        for (Map.Entry<GolemVariant, Boolean> entry : availability.variants().entrySet()) {
            variantsJson.addProperty(entry.getKey().id(), entry.getValue());
        }
        familyJson.add("variants", variantsJson);
        return familyJson;
    }

    private static JsonObject villageWeightDefaultsJson() {
        return villageSpawnWeightsToJson(VillageSpawnWeights.defaults()).getAsJsonObject("weights");
    }

    private static JsonObject zombieVillageSpawningToJson(ZombieVillageSpawningConfig cfg) {
        JsonObject zombie = new JsonObject();
        zombie.addProperty("enabled", cfg.enabled());
        zombie.addProperty("min_zombie_villagers", cfg.minZombieVillagers());
        zombie.addProperty("zombie_villagers_per_golem", cfg.zombieVillagersPerGolem());
        zombie.addProperty("regular_zombie_bonus_enabled", cfg.regularZombieBonusEnabled());
        zombie.addProperty("regular_zombie_bonus_threshold", cfg.regularZombieBonusThreshold());
        zombie.addProperty("max_zombie_golems_per_village", cfg.maxZombieGolemsPerVillage());
        return zombie;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiGolemConfig that)) return false;
        return allowGolemHealing == that.allowGolemHealing
            && tiers.equals(that.tiers)
            && villageSpawnWeights.equals(that.villageSpawnWeights)
            && zombieVillageSpawning.equals(that.zombieVillageSpawning)
            && golemAvailability.equals(that.golemAvailability);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowGolemHealing, tiers, villageSpawnWeights, zombieVillageSpawning, golemAvailability);
    }
}
