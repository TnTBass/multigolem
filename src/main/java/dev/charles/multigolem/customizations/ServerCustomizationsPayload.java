package dev.charles.multigolem.customizations;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ServerCustomizationsPayload(ServerCustomizationsSnapshot snapshot) implements CustomPacketPayload {
    public static final String PAYLOAD_PATH = "server_customizations";
    public static final Identifier ID = Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, PAYLOAD_PATH);
    public static final Type<ServerCustomizationsPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerCustomizationsPayload> CODEC = StreamCodec.of(
        ServerCustomizationsPayload::write,
        ServerCustomizationsPayload::read
    );

    private static final int MAX_WEIGHTS = 16;
    private static final int MAX_VARIANTS = 16;
    private static final int MAX_LINES = 64;

    public ServerCustomizationsPayload {
        Objects.requireNonNull(snapshot, "snapshot");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static ServerCustomizationsPayload read(RegistryFriendlyByteBuf buf) {
        boolean healingEnabled = buf.readBoolean();
        boolean villageSpawnsEnabled = buf.readBoolean();
        int weightCount = readLimitedCount(buf, MAX_WEIGHTS, "village spawn weights");
        EnumMap<GolemVariant, Integer> weights = new EnumMap<>(GolemVariant.class);
        for (int i = 0; i < weightCount; i++) {
            weights.put(readVariant(buf), buf.readVarInt());
        }
        boolean zombieVillageSpawningEnabled = buf.readBoolean();
        String permissionsMode = buf.readUtf(256);
        int variantCount = readLimitedCount(buf, MAX_VARIANTS, "variant overrides");
        List<VariantCustomizationSummary> variants = new ArrayList<>();
        for (int i = 0; i < variantCount; i++) {
            GolemVariant variant = readVariant(buf);
            int lineCount = readLimitedCount(buf, MAX_LINES, "variant lines");
            List<String> lines = new ArrayList<>();
            for (int j = 0; j < lineCount; j++) {
                lines.add(buf.readUtf(512));
            }
            variants.add(new VariantCustomizationSummary(variant, lines));
        }
        return new ServerCustomizationsPayload(new ServerCustomizationsSnapshot(
            healingEnabled,
            villageSpawnsEnabled,
            weights,
            zombieVillageSpawningEnabled,
            permissionsMode,
            variants
        ));
    }

    private static void write(RegistryFriendlyByteBuf buf, ServerCustomizationsPayload payload) {
        ServerCustomizationsSnapshot snapshot = payload.snapshot();
        buf.writeBoolean(snapshot.healingEnabled());
        buf.writeBoolean(snapshot.villageSpawnsEnabled());
        buf.writeVarInt(snapshot.villageSpawnWeights().size());
        for (Map.Entry<GolemVariant, Integer> entry : snapshot.villageSpawnWeights().entrySet()) {
            buf.writeUtf(entry.getKey().id());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeBoolean(snapshot.zombieVillageSpawningEnabled());
        buf.writeUtf(snapshot.permissionsMode(), 256);
        buf.writeVarInt(snapshot.variantOverrides().size());
        for (VariantCustomizationSummary variant : snapshot.variantOverrides()) {
            buf.writeUtf(variant.variant().id());
            buf.writeVarInt(variant.lines().size());
            for (String line : variant.lines()) {
                buf.writeUtf(line, 512);
            }
        }
    }

    private static int readLimitedCount(RegistryFriendlyByteBuf buf, int max, String name) {
        int count = buf.readVarInt();
        if (count < 0 || count > max) {
            throw new IllegalArgumentException(name + " count out of range: " + count);
        }
        return count;
    }

    private static GolemVariant readVariant(RegistryFriendlyByteBuf buf) {
        String id = buf.readUtf(64);
        return GolemVariant.fromId(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown golem variant in server customizations payload: " + id));
    }
}
