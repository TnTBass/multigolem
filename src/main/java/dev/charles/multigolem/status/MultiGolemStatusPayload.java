package dev.charles.multigolem.status;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.internal.modstatus.ModStatusVersion;
import dev.charles.multigolem.internal.modstatus.ModStatusVersionPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Arrays;

public record MultiGolemStatusPayload(byte[] encodedVersion) implements CustomPacketPayload {
    private static final int MAX_ENCODED_VERSION_BYTES = 512;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, MultiGolemStatus.PAYLOAD_PATH);
    public static final Type<MultiGolemStatusPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiGolemStatusPayload> CODEC = StreamCodec.of(
        MultiGolemStatusPayload::write,
        MultiGolemStatusPayload::read
    );

    public MultiGolemStatusPayload {
        ModStatusVersionPayload.decodeServerVersion(encodedVersion);
        encodedVersion = Arrays.copyOf(encodedVersion, encodedVersion.length);
    }

    public static MultiGolemStatusPayload fromServerVersion(String serverVersion) {
        return new MultiGolemStatusPayload(ModStatusVersionPayload.encodeServerVersion(serverVersion));
    }

    public static MultiGolemStatusPayload fromServerVersion(String serverVersion, String serverBuild) {
        return new MultiGolemStatusPayload(ModStatusVersionPayload.encodeServerVersion(serverVersion, serverBuild));
    }

    public String serverVersion() {
        return serverVersionInfo().version();
    }

    public String serverBuild() {
        return serverVersionInfo().build();
    }

    public ModStatusVersion serverVersionInfo() {
        return ModStatusVersionPayload.decodeServerVersionInfo(encodedVersion);
    }

    @Override
    public byte[] encodedVersion() {
        return Arrays.copyOf(encodedVersion, encodedVersion.length);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MultiGolemStatusPayload other
            && Arrays.equals(encodedVersion, other.encodedVersion);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(encodedVersion);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static MultiGolemStatusPayload read(RegistryFriendlyByteBuf buf) {
        return new MultiGolemStatusPayload(buf.readByteArray(MAX_ENCODED_VERSION_BYTES));
    }

    private static void write(RegistryFriendlyByteBuf buf, MultiGolemStatusPayload payload) {
        buf.writeByteArray(payload.encodedVersion());
    }
}
