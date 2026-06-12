package dev.charles.multigolem.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GolemSpawnOrigin {
    UNKNOWN("unknown"),
    VILLAGE("village");

    public static final Codec<GolemSpawnOrigin> CODEC = Codec.STRING.flatXmap(
        id -> fromId(id)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown GolemSpawnOrigin id: " + id)),
        origin -> DataResult.success(origin.id)
    );

    private static final Map<String, GolemSpawnOrigin> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(origin -> origin.id, Function.identity()));

    private final String id;

    GolemSpawnOrigin(String id) {
        this.id = id;
    }

    public String id() { return id; }

    public static Optional<GolemSpawnOrigin> fromId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }
}
