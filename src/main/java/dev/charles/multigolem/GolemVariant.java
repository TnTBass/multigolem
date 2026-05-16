package dev.charles.multigolem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GolemVariant {
    COPPER   ("copper",    Blocks.COPPER_BLOCK,    Items.COPPER_INGOT,    Items.COPPER_INGOT),
    IRON     ("iron",      Blocks.IRON_BLOCK,      Items.IRON_INGOT,      Items.IRON_INGOT),
    GOLD     ("gold",      Blocks.GOLD_BLOCK,      Items.GOLD_INGOT,      Items.GOLD_INGOT),
    EMERALD  ("emerald",   Blocks.EMERALD_BLOCK,   Items.EMERALD,         Items.EMERALD),
    DIAMOND  ("diamond",   Blocks.DIAMOND_BLOCK,   Items.DIAMOND,         Items.DIAMOND),
    NETHERITE("netherite", Blocks.NETHERITE_BLOCK, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP);

    public static final StreamCodec<ByteBuf, GolemVariant> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(
            s -> fromId(s).orElse(IRON),
            GolemVariant::id);

    public static final Codec<GolemVariant> CODEC = Codec.STRING.flatXmap(
        id -> fromId(id)
            .map(com.mojang.serialization.DataResult::success)
            .orElseGet(() -> com.mojang.serialization.DataResult.error(() -> "Unknown GolemVariant id: " + id)),
        v -> com.mojang.serialization.DataResult.success(v.id)
    );

    private static final Map<Block, GolemVariant> BY_BODY_BLOCK = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.bodyBlock, Function.identity()));
    private static final Map<Item, GolemVariant> BY_HEAL_INGOT = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.healIngot, Function.identity()));
    private static final Map<String, GolemVariant> BY_ID = Arrays.stream(values())
        .collect(Collectors.toUnmodifiableMap(v -> v.id, Function.identity()));

    private final String id;
    private final Block bodyBlock;
    private final Item healIngot;
    private final Item dropItem;

    GolemVariant(String id, Block bodyBlock, Item healIngot, Item dropItem) {
        this.id = id;
        this.bodyBlock = bodyBlock;
        this.healIngot = healIngot;
        this.dropItem = dropItem;
    }

    public String id() { return id; }
    public Block bodyBlock() { return bodyBlock; }
    public Item healIngot() { return healIngot; }
    public Item dropItem() { return dropItem; }

    public static Optional<GolemVariant> fromBodyBlock(Block block) {
        return Optional.ofNullable(BY_BODY_BLOCK.get(block));
    }

    public static Optional<GolemVariant> fromIngot(Item item) {
        return Optional.ofNullable(BY_HEAL_INGOT.get(item));
    }

    public static Optional<GolemVariant> fromId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }
}
