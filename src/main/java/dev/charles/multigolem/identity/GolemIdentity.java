package dev.charles.multigolem.identity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.charles.multigolem.GolemVariant;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

public record GolemIdentity(
    GolemFamily family,
    GolemVariant variant,
    Optional<GolemSurfaceState> surfaceState
) {
    private static final GolemIdentity DEFAULT_IRON =
        new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON, Optional.empty());

    public static final Codec<GolemIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GolemFamily.CODEC.fieldOf("family").forGetter(GolemIdentity::family),
        GolemVariant.CODEC.fieldOf("variant").forGetter(GolemIdentity::variant),
        GolemSurfaceState.CODEC.optionalFieldOf("surface_state").forGetter(GolemIdentity::surfaceState)
    ).apply(instance, GolemIdentity::new));

    public static final StreamCodec<ByteBuf, GolemIdentity> STREAM_CODEC = StreamCodec.composite(
        GolemFamily.STREAM_CODEC, GolemIdentity::family,
        GolemVariant.STREAM_CODEC, GolemIdentity::variant,
        ByteBufCodecs.BOOL, identity -> identity.surfaceState().isPresent(),
        GolemSurfaceState.STREAM_CODEC, identity -> identity.surfaceState().orElse(GolemSurfaceState.DEFAULT),
        (family, variant, hasSurface, surface) ->
            new GolemIdentity(family, variant, hasSurface ? Optional.of(surface) : Optional.empty())
    );

    public GolemIdentity {
        if (surfaceState == null) {
            surfaceState = Optional.empty();
        }
    }

    public GolemIdentity(GolemFamily family, GolemVariant variant) {
        this(family, variant, Optional.empty());
    }

    public static GolemIdentity defaultIron() {
        return DEFAULT_IRON;
    }

    public static GolemIdentity ofIronVariant(GolemVariant variant) {
        return new GolemIdentity(GolemFamily.IRON_GOLEM, variant, Optional.empty());
    }

    public static GolemIdentity ofIronVariant(GolemVariant variant, GolemSurfaceState surfaceState) {
        return new GolemIdentity(GolemFamily.IRON_GOLEM, variant, Optional.of(surfaceState));
    }

    public boolean isDefaultIron() {
        return DEFAULT_IRON.equals(this);
    }

    public boolean isValidForPhase2() {
        return family == GolemFamily.IRON_GOLEM && variant != null && surfaceState.isEmpty();
    }

    public boolean isValidForPhase3() {
        if (family != GolemFamily.IRON_GOLEM || variant == null) return false;
        return surfaceState.isEmpty() || variant == GolemVariant.COPPER;
    }
}
