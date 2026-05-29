package dev.charles.multigolem.identity;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GolemFamily {
    IRON_GOLEM("iron_golem");

    public static final Codec<GolemFamily> CODEC = Codec.STRING.flatXmap(
        id -> fromId(id)
            .map(com.mojang.serialization.DataResult::success)
            .orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "Unknown GolemFamily id: " + id)),
        family -> com.mojang.serialization.DataResult.success(family.id)
    );
    public static final StreamCodec<ByteBuf, GolemFamily> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(s -> fromId(s).orElse(IRON_GOLEM), GolemFamily::id);

    private static final Map<String, GolemFamily> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(GolemFamily::id, Function.identity()));

    private final String id;

    GolemFamily(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Optional<GolemFamily> fromId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }
}
