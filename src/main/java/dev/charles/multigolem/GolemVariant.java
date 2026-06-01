package dev.charles.multigolem;

import com.mojang.serialization.Codec;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GolemVariant {
    COPPER   ("copper",    "Copper",    Blocks.COPPER_BLOCK,    Items.COPPER_INGOT,    Items.COPPER_INGOT),
    IRON     ("iron",      "Iron",      Blocks.IRON_BLOCK,      Items.IRON_INGOT,      Items.IRON_INGOT),
    GOLD     ("gold",      "Gold",      Blocks.GOLD_BLOCK,      Items.GOLD_INGOT,      Items.GOLD_INGOT),
    EMERALD  ("emerald",   "Emerald",   Blocks.EMERALD_BLOCK,   Items.EMERALD,         Items.EMERALD),
    DIAMOND  ("diamond",   "Diamond",   Blocks.DIAMOND_BLOCK,   Items.DIAMOND,         Items.DIAMOND),
    NETHERITE("netherite", "Netherite", Blocks.NETHERITE_BLOCK, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP),
    ZOMBIE   ("zombie",    "Zombie",    Blocks.MOSSY_COBBLESTONE, Items.ROTTEN_FLESH,  Items.ROTTEN_FLESH);

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
    private final String displayName;
    private final Block bodyBlock;
    private final Item healIngot;
    private final Item dropItem;

    GolemVariant(String id, String displayName, Block bodyBlock, Item healIngot, Item dropItem) {
        this.id = id;
        this.displayName = displayName;
        this.bodyBlock = bodyBlock;
        this.healIngot = healIngot;
        this.dropItem = dropItem;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Block bodyBlock() { return bodyBlock; }
    public Item healIngot() { return healIngot; }
    public Item dropItem() { return dropItem; }

    public static List<GolemVariant> nonIronVariants() {
        return GolemVariantCatalog.variantsWhere(spec -> spec.variant() != IRON);
    }

    public static List<GolemVariant> spawnEggVariants() {
        return GolemVariantCatalog.variantsWhere(GolemVariantSpec::spawnEggEnabled);
    }

    public static List<GolemVariant> lootVariants() {
        return GolemVariantCatalog.variantsWhere(GolemVariantSpec::lootEnabled);
    }

    public static List<GolemVariant> multiGolemPlayerBuildableVariants() {
        return GolemVariantCatalog.variantsWhere(GolemVariantSpec::playerBuildable);
    }

    public boolean matchesBodyBlock(BlockState state) {
        if (this == COPPER) {
            return state.is(BlockTags.COPPER) || isCopperFamilyBlock(state.getBlock());
        }
        return state.is(bodyBlock);
    }

    public static Optional<GolemVariant> fromBodyBlock(Block block) {
        return GolemVariantCatalog.forBodyBlock(block.defaultBlockState())
            .map(GolemVariantSpec::variant);
    }

    public static boolean isCopperFamilyBlock(Block block) {
        if (block instanceof WeatheringCopper) {
            return true;
        }
        Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
        return unwaxed instanceof WeatheringCopper;
    }

    public static Optional<GolemVariant> fromIngot(Item item) {
        return GolemVariantCatalog.forHealItem(item)
            .map(GolemVariantSpec::variant);
    }

    public static Optional<GolemVariant> fromId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(BY_ID.get(id));
    }
}
