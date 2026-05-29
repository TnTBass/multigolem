package dev.charles.multigolem.identity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.charles.multigolem.GolemVariant;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GolemIdentity(GolemFamily family, GolemVariant variant) {
    private static final String STREAM_SEPARATOR = ":";
    private static final GolemIdentity DEFAULT_IRON =
        new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.IRON);

    public static final Codec<GolemIdentity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        GolemFamily.CODEC.fieldOf("family").forGetter(GolemIdentity::family),
        GolemVariant.CODEC.fieldOf("variant").forGetter(GolemIdentity::variant)
    ).apply(instance, GolemIdentity::new));

    public static final StreamCodec<ByteBuf, GolemIdentity> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(GolemIdentity::fromStreamId, GolemIdentity::streamId);

    public static GolemIdentity defaultIron() {
        return DEFAULT_IRON;
    }

    public static GolemIdentity ofIronVariant(GolemVariant variant) {
        return new GolemIdentity(GolemFamily.IRON_GOLEM, variant);
    }

    public boolean isDefaultIron() {
        return DEFAULT_IRON.equals(this);
    }

    public boolean isValidForPhase2() {
        return family == GolemFamily.IRON_GOLEM && variant != null;
    }

    private String streamId() {
        return (family == null ? "" : family.id()) + STREAM_SEPARATOR + (variant == null ? "" : variant.id());
    }

    private static GolemIdentity fromStreamId(String id) {
        String[] parts = id == null ? new String[0] : id.split(STREAM_SEPARATOR, 2);
        if (parts.length != 2) return DEFAULT_IRON;
        GolemFamily family = GolemFamily.fromId(parts[0]).orElse(GolemFamily.IRON_GOLEM);
        GolemVariant variant = GolemVariant.fromId(parts[1]).orElse(GolemVariant.IRON);
        return new GolemIdentity(family, variant);
    }
}
