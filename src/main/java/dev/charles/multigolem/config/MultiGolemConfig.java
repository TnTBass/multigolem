package dev.charles.multigolem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class MultiGolemConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final boolean allowGolemHealing;
    private final Map<GolemVariant, TierStats> tiers;

    private MultiGolemConfig(boolean allowGolemHealing, Map<GolemVariant, TierStats> tiers) {
        this.allowGolemHealing = allowGolemHealing;
        this.tiers = new EnumMap<>(tiers);
    }

    public boolean allowGolemHealing() { return allowGolemHealing; }

    public TierStats tier(GolemVariant variant) {
        return tiers.get(variant);
    }

    public static MultiGolemConfig defaults() {
        EnumMap<GolemVariant, TierStats> map = new EnumMap<>(GolemVariant.class);
        map.put(GolemVariant.COPPER,    new TierStats(60,  8.5,  true));
        map.put(GolemVariant.IRON,      new TierStats(100, 15.0, true));
        map.put(GolemVariant.GOLD,      new TierStats(130, 22.5, true));
        map.put(GolemVariant.EMERALD,   new TierStats(200, 40.0, true));
        map.put(GolemVariant.DIAMOND,   new TierStats(350, 62.5, true));
        map.put(GolemVariant.NETHERITE, new TierStats(600, 85.0, true));
        return new MultiGolemConfig(true, map);
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
                MultiGolem.LOG.warn("multigolem config at {} is empty; using defaults (not overwriting)", path);
                return defaults();
            }
            return parse(root);
        } catch (JsonSyntaxException e) {
            MultiGolem.LOG.warn("multigolem config at {} is malformed; using defaults and NOT overwriting your file: {}", path, e.getMessage());
            return defaults();
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not read multigolem config at {}; using defaults: {}", path, e.getMessage());
            return defaults();
        }
    }

    private static MultiGolemConfig parse(JsonObject root) {
        MultiGolemConfig defaults = defaults();
        boolean healing = defaults.allowGolemHealing;
        if (root.has("allow_golem_healing") && root.get("allow_golem_healing").getAsJsonPrimitive().isBoolean()) {
            healing = root.get("allow_golem_healing").getAsBoolean();
        } else if (root.has("allow_golem_healing")) {
            MultiGolem.LOG.warn("allow_golem_healing is not a boolean; using default {}", healing);
        }

        EnumMap<GolemVariant, TierStats> tiers = new EnumMap<>(GolemVariant.class);
        JsonObject tiersJson = root.has("tiers") && root.get("tiers").isJsonObject()
            ? root.getAsJsonObject("tiers") : new JsonObject();
        for (GolemVariant v : GolemVariant.values()) {
            TierStats def = defaults.tier(v);
            if (!tiersJson.has(v.id()) || !tiersJson.get(v.id()).isJsonObject()) {
                tiers.put(v, def);
                continue;
            }
            JsonObject t = tiersJson.getAsJsonObject(v.id());
            int health = readInt(t, "max_health", def.maxHealth());
            double damage = readDouble(t, "attack_damage", def.attackDamage());
            boolean anger = readBoolean(t, "anger_on_hit", def.angerOnHit());
            TierStats raw = new TierStats(health, damage, anger);
            if (raw.isClamped()) {
                MultiGolem.LOG.warn("tier {} has out-of-range values; clamping to [{}..{}] HP / [{}..{}] dmg",
                    v.id(), TierStats.MIN_HEALTH, TierStats.MAX_HEALTH, TierStats.MIN_DAMAGE, TierStats.MAX_DAMAGE);
            }
            tiers.put(v, raw.clamped());
        }
        return new MultiGolemConfig(healing, tiers);
    }

    private static int readInt(JsonObject obj, String key, int fallback) {
        try {
            return obj.has(key) ? obj.get(key).getAsInt() : fallback;
        } catch (Exception e) {
            MultiGolem.LOG.warn("field {} is not an int; using default {}", key, fallback);
            return fallback;
        }
    }

    private static double readDouble(JsonObject obj, String key, double fallback) {
        try {
            return obj.has(key) ? obj.get(key).getAsDouble() : fallback;
        } catch (Exception e) {
            MultiGolem.LOG.warn("field {} is not a number; using default {}", key, fallback);
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject obj, String key, boolean fallback) {
        try {
            return obj.has(key) ? obj.get(key).getAsBoolean() : fallback;
        } catch (Exception e) {
            MultiGolem.LOG.warn("field {} is not a boolean; using default {}", key, fallback);
            return fallback;
        }
    }

    private static void writeQuietly(Path path, MultiGolemConfig cfg) {
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("allow_golem_healing", cfg.allowGolemHealing);
            JsonObject tiers = new JsonObject();
            for (GolemVariant v : GolemVariant.values()) {
                TierStats s = cfg.tier(v);
                JsonObject t = new JsonObject();
                t.addProperty("max_health", s.maxHealth());
                t.addProperty("attack_damage", s.attackDamage());
                t.addProperty("anger_on_hit", s.angerOnHit());
                tiers.add(v.id(), t);
            }
            root.add("tiers", tiers);
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MultiGolem.LOG.warn("could not write default multigolem config to {}: {}", path, e.getMessage());
        }
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
