package dev.charles.multigolem;

import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
        assertEquals(GolemVariant.COPPER,    GolemVariant.fromBodyBlock(Blocks.COPPER_BLOCK).orElseThrow());
        assertEquals(GolemVariant.IRON,      GolemVariant.fromBodyBlock(Blocks.IRON_BLOCK).orElseThrow());
        assertEquals(GolemVariant.GOLD,      GolemVariant.fromBodyBlock(Blocks.GOLD_BLOCK).orElseThrow());
        assertEquals(GolemVariant.EMERALD,   GolemVariant.fromBodyBlock(Blocks.EMERALD_BLOCK).orElseThrow());
        assertEquals(GolemVariant.DIAMOND,   GolemVariant.fromBodyBlock(Blocks.DIAMOND_BLOCK).orElseThrow());
        assertEquals(GolemVariant.NETHERITE, GolemVariant.fromBodyBlock(Blocks.NETHERITE_BLOCK).orElseThrow());
    }

    @Test
    void fromBodyBlock_returnsEmptyForUnsupportedBlock() {
        assertTrue(GolemVariant.fromBodyBlock(Blocks.DIRT).isEmpty());
    }

    @Test
    void fromIngot_recognizesAllSixTiers() {
        assertEquals(GolemVariant.COPPER,    GolemVariant.fromIngot(Items.COPPER_INGOT).orElseThrow());
        assertEquals(GolemVariant.IRON,      GolemVariant.fromIngot(Items.IRON_INGOT).orElseThrow());
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
    void idIsStableLowercase() {
        assertEquals("copper",    GolemVariant.COPPER.id());
        assertEquals("iron",      GolemVariant.IRON.id());
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
        assertEquals(Items.GOLD_INGOT,      GolemVariant.GOLD.dropItem());
        assertEquals(Items.EMERALD,         GolemVariant.EMERALD.dropItem());
        assertEquals(Items.DIAMOND,         GolemVariant.DIAMOND.dropItem());
        assertEquals(Items.NETHERITE_SCRAP, GolemVariant.NETHERITE.dropItem());
    }
}
