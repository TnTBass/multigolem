package dev.charles.multigolem.identity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GolemSurfaceState(GolemWeatheringStage weatheringStage, boolean waxed) {
    public static final GolemSurfaceState DEFAULT =
        new GolemSurfaceState(GolemWeatheringStage.UNAFFECTED, false);

    public static final Codec<GolemSurfaceState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GolemWeatheringStage.CODEC.fieldOf("weathering_stage").forGetter(GolemSurfaceState::weatheringStage),
        Codec.BOOL.fieldOf("waxed").forGetter(GolemSurfaceState::waxed)
    ).apply(instance, GolemSurfaceState::new));

    public static final StreamCodec<ByteBuf, GolemSurfaceState> STREAM_CODEC = StreamCodec.composite(
        GolemWeatheringStage.STREAM_CODEC, GolemSurfaceState::weatheringStage,
        ByteBufCodecs.BOOL, GolemSurfaceState::waxed,
        GolemSurfaceState::new
    );

    public boolean isDefault() {
        return DEFAULT.equals(this);
    }
}
