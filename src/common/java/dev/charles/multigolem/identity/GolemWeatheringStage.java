package dev.charles.multigolem.identity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GolemWeatheringStage {
    UNAFFECTED("unaffected"),
    EXPOSED("exposed"),
    WEATHERED("weathered"),
    OXIDIZED("oxidized");

    public static final Codec<GolemWeatheringStage> CODEC = Codec.STRING.flatXmap(
        id -> fromId(id)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown GolemWeatheringStage id: " + id)),
        stage -> DataResult.success(stage.id)
    );
    public static final StreamCodec<ByteBuf, GolemWeatheringStage> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(
            s -> fromId(s).orElseThrow(() -> new DecoderException("Unknown GolemWeatheringStage id: " + s)),
            GolemWeatheringStage::id
        );

    private static final Map<String, GolemWeatheringStage> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(GolemWeatheringStage::id, Function.identity()));

    private final String id;

    GolemWeatheringStage(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isAfter(GolemWeatheringStage other) {
        return ordinal() > other.ordinal();
    }

    public static Optional<GolemWeatheringStage> fromId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }
}
