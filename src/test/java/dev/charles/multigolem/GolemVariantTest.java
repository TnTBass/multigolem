package dev.charles.multigolem;

import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GolemVariantTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void fromBodyBlock_recognizesAllSixTiers() {
        assertEquals(GolemVariant.COPPER,    GolemVariant.fromBodyBlock(copper(WeatheringCopper.WeatherState.UNAFFECTED)).orElseThrow());
        assertEquals(GolemVariant.IRON,      GolemVariant.fromBodyBlock(Blocks.IRON_BLOCK).orElseThrow());
        assertEquals(GolemVariant.REDSTONE,  GolemVariant.fromBodyBlock(Blocks.REDSTONE_BLOCK).orElseThrow());
        assertEquals(GolemVariant.GOLD,      GolemVariant.fromBodyBlock(Blocks.GOLD_BLOCK).orElseThrow());
        assertEquals(GolemVariant.EMERALD,   GolemVariant.fromBodyBlock(Blocks.EMERALD_BLOCK).orElseThrow());
        assertEquals(GolemVariant.DIAMOND,   GolemVariant.fromBodyBlock(Blocks.DIAMOND_BLOCK).orElseThrow());
        assertEquals(GolemVariant.NETHERITE, GolemVariant.fromBodyBlock(Blocks.NETHERITE_BLOCK).orElseThrow());
    }

    @Test
    void fromBodyBlock_recognizesCopperFamilyAsCopperTier() {
        assertEquals(GolemVariant.COPPER, GolemVariant.fromBodyBlock(waxedCopper(WeatheringCopper.WeatherState.UNAFFECTED)).orElseThrow());
        assertEquals(GolemVariant.COPPER, GolemVariant.fromBodyBlock(copper(WeatheringCopper.WeatherState.EXPOSED)).orElseThrow());
        assertEquals(GolemVariant.COPPER, GolemVariant.fromBodyBlock(copper(WeatheringCopper.WeatherState.WEATHERED)).orElseThrow());
        assertEquals(GolemVariant.COPPER, GolemVariant.fromBodyBlock(copper(WeatheringCopper.WeatherState.OXIDIZED)).orElseThrow());
        assertEquals(GolemVariant.COPPER, GolemVariant.fromBodyBlock(waxedCopper(WeatheringCopper.WeatherState.OXIDIZED)).orElseThrow());
    }

    @Test
    void fromBodyBlock_returnsEmptyForUnsupportedBlock() {
        assertTrue(GolemVariant.fromBodyBlock(Blocks.DIRT).isEmpty());
    }

    @Test
    void fromIngot_recognizesAllSixTiers() {
        assertEquals(GolemVariant.COPPER,    GolemVariant.fromIngot(Items.COPPER_INGOT).orElseThrow());
        assertEquals(GolemVariant.IRON,      GolemVariant.fromIngot(Items.IRON_INGOT).orElseThrow());
        assertEquals(GolemVariant.REDSTONE,  GolemVariant.fromIngot(Items.REDSTONE).orElseThrow());
        assertEquals(GolemVariant.GOLD,      GolemVariant.fromIngot(Items.GOLD_INGOT).orElseThrow());
        assertEquals(GolemVariant.EMERALD,   GolemVariant.fromIngot(Items.EMERALD).orElseThrow());
        assertEquals(GolemVariant.DIAMOND,   GolemVariant.fromIngot(Items.DIAMOND).orElseThrow());
        assertEquals(GolemVariant.NETHERITE, GolemVariant.fromIngot(Items.NETHERITE_INGOT).orElseThrow());
    }

    @Test
    void fromIngot_returnsEmptyForUnsupportedItem() {
        assertTrue(GolemVariant.fromIngot(Items.APPLE).isEmpty());
    }

    @Test
    void zombieUsesMossyCobblestoneAndRottenFlesh() {
        assertEquals(GolemVariant.ZOMBIE, GolemVariant.fromBodyBlock(Blocks.MOSSY_COBBLESTONE).orElseThrow());
        assertTrue(GolemVariant.ZOMBIE.matchesBodyBlock(Blocks.MOSSY_COBBLESTONE.defaultBlockState()));
        assertEquals(GolemVariant.ZOMBIE, GolemVariant.fromIngot(Items.ROTTEN_FLESH).orElseThrow());
        assertEquals(Items.ROTTEN_FLESH, GolemVariant.ZOMBIE.dropItem());
        assertEquals("zombie", GolemVariant.ZOMBIE.id());
        assertEquals("Zombie", GolemVariant.ZOMBIE.displayName());
    }

    @Test
    void redstoneUsesRedstoneBlockAndDust() {
        assertEquals(GolemVariant.REDSTONE, GolemVariant.fromBodyBlock(Blocks.REDSTONE_BLOCK).orElseThrow());
        assertTrue(GolemVariant.REDSTONE.matchesBodyBlock(Blocks.REDSTONE_BLOCK.defaultBlockState()));
        assertEquals(GolemVariant.REDSTONE, GolemVariant.fromIngot(Items.REDSTONE).orElseThrow());
        assertEquals(Items.REDSTONE, GolemVariant.REDSTONE.dropItem());
        assertEquals("redstone", GolemVariant.REDSTONE.id());
        assertEquals("Redstone", GolemVariant.REDSTONE.displayName());
    }

    @Test
    void idIsStableLowercase() {
        assertEquals("copper",    GolemVariant.COPPER.id());
        assertEquals("iron",      GolemVariant.IRON.id());
        assertEquals("redstone",  GolemVariant.REDSTONE.id());
        assertEquals("gold",      GolemVariant.GOLD.id());
        assertEquals("emerald",   GolemVariant.EMERALD.id());
        assertEquals("diamond",   GolemVariant.DIAMOND.id());
        assertEquals("netherite", GolemVariant.NETHERITE.id());
    }

    @Test
    void fromId_roundTripsAllVariants() {
        for (GolemVariant v : GolemVariant.values()) {
            assertEquals(v, GolemVariant.fromId(v.id()).orElseThrow());
        }
    }

    @Test
    void fromId_returnsEmptyForUnknown() {
        assertTrue(GolemVariant.fromId("obsidian").isEmpty());
        assertTrue(GolemVariant.fromId("").isEmpty());
    }

    @Test
    void dropItem_matchesExpected() {
        assertEquals(Items.COPPER_INGOT,    GolemVariant.COPPER.dropItem());
        assertEquals(Items.IRON_INGOT,      GolemVariant.IRON.dropItem());
        assertEquals(Items.REDSTONE,        GolemVariant.REDSTONE.dropItem());
        assertEquals(Items.GOLD_INGOT,      GolemVariant.GOLD.dropItem());
        assertEquals(Items.EMERALD,         GolemVariant.EMERALD.dropItem());
        assertEquals(Items.DIAMOND,         GolemVariant.DIAMOND.dropItem());
        assertEquals(Items.NETHERITE_SCRAP, GolemVariant.NETHERITE.dropItem());
    }

    private static Block copper(WeatheringCopper.WeatherState state) {
        return Blocks.COPPER_BLOCK.weathering().pick(state);
    }

    private static Block waxedCopper(WeatheringCopper.WeatherState state) {
        return Blocks.COPPER_BLOCK.waxed().pick(state);
    }
}
