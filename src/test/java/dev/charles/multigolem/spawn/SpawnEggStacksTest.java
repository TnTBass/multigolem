package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SpawnEggStacksTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void markedStacksUseVanillaIronGolemSpawnEggAndExactCustomData() {
        for (GolemVariant variant : List.of(
            GolemVariant.COPPER,
            GolemVariant.GOLD,
            GolemVariant.REDSTONE,
            GolemVariant.LAPIS,
            GolemVariant.EMERALD,
            GolemVariant.DIAMOND,
            GolemVariant.NETHERITE,
            GolemVariant.ZOMBIE
        )) {
            ItemStack stack = SpawnEggStacks.create(variant);

            assertTrue(stack.is(Items.IRON_GOLEM_SPAWN_EGG));
            assertEquals(variant, SpawnEggStacks.variantFrom(stack).orElseThrow());
            assertEquals(GolemIdentity.ofIronVariant(variant), SpawnEggStacks.identityFrom(stack).orElseThrow());
            assertEquals("{multigolem:{family:\"iron_golem\",variant:\"" + variant.id() + "\"}}",
                SpawnEggStacks.customDataSnbt(stack));
        }
    }

    @Test
    void markedStacksHaveVariantSpawnEggNames() {
        assertEquals("Copper Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.COPPER).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Gold Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.GOLD).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Redstone Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.REDSTONE).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Lapis Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.LAPIS).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Emerald Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.EMERALD).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Diamond Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.DIAMOND).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Netherite Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.NETHERITE).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Zombie Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.ZOMBIE).get(DataComponents.ITEM_NAME).getString());
    }

    @Test
    void unmarkedIronGolemEggHasNoVariantMarker() {
        assertTrue(SpawnEggStacks.variantFrom(new ItemStack(Items.IRON_GOLEM_SPAWN_EGG)).isEmpty());
    }

    @Test
    void factoryReturnsFreshCopies() {
        ItemStack first = SpawnEggStacks.create(GolemVariant.DIAMOND);
        ItemStack second = SpawnEggStacks.create(GolemVariant.DIAMOND);

        first.setCount(17);

        assertEquals(1, second.getCount());
        assertEquals(GolemVariant.DIAMOND, SpawnEggStacks.variantFrom(second).orElseThrow());
    }

    @Test
    void manuallyMarkedIronVariantIsNotAV4Egg() {
        ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
        CompoundTag root = new CompoundTag();
        CompoundTag multigolem = new CompoundTag();
        multigolem.putString("variant", "iron");
        root.put("multigolem", multigolem);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        assertTrue(SpawnEggStacks.variantFrom(stack).isEmpty());
    }

    @Test
    void oldVariantOnlyMarkerReadsAsIronFamilyIdentity() {
        ItemStack stack = markedEgg("diamond", null);

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND), SpawnEggStacks.identityFrom(stack).orElseThrow());
    }

    @Test
    void spawnEggSurfaceMarkerRoundTripsFullIdentity() {
        GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true));

        ItemStack stack = SpawnEggStacks.create(identity);

        assertEquals(identity, SpawnEggStacks.identityFrom(stack).orElseThrow());
        assertEquals("{multigolem:{family:\"iron_golem\",surface:{waxed:1b,weathering_stage:\"weathered\"},variant:\"copper\"}}",
            SpawnEggStacks.customDataSnbt(stack));
    }

    @Test
    void oldVariantOnlyCopperMarkerReadsAsSurfaceEmptyCopper() {
        ItemStack stack = markedEgg("copper", null);

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER), SpawnEggStacks.identityFrom(stack).orElseThrow());
    }

    @Test
    void surfaceEmptyCopperSpawnEggWritesNoSurfaceMarker() {
        ItemStack stack = SpawnEggStacks.create(GolemIdentity.ofIronVariant(GolemVariant.COPPER));

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.COPPER), SpawnEggStacks.identityFrom(stack).orElseThrow());
        assertFalse(SpawnEggStacks.customDataSnbt(stack).contains("surface"));
    }

    @Test
    void invalidSurfaceMarkerReadsEmptyInsteadOfWrongGolem() {
        ItemStack stack = markedEgg("gold", "iron_golem");
        CompoundTag multigolem = stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompoundOrEmpty("multigolem");
        CompoundTag surface = new CompoundTag();
        surface.putString("weathering_stage", "oxidized");
        surface.putBoolean("waxed", true);
        multigolem.put("surface", surface);
        CompoundTag root = new CompoundTag();
        root.put("multigolem", multigolem);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        assertTrue(SpawnEggStacks.identityFrom(stack).isEmpty());
    }

    @Test
    void unknownSurfaceWeatheringStageReadsEmpty() {
        ItemStack stack = markedEgg("copper", "iron_golem");
        CompoundTag multigolem = stack.get(DataComponents.CUSTOM_DATA).copyTag().getCompoundOrEmpty("multigolem");
        CompoundTag surface = new CompoundTag();
        surface.putString("weathering_stage", "patinated");
        surface.putBoolean("waxed", true);
        multigolem.put("surface", surface);
        CompoundTag root = new CompoundTag();
        root.put("multigolem", multigolem);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));

        assertTrue(SpawnEggStacks.identityFrom(stack).isEmpty());
    }

    @Test
    void oldIronVariantMarkerReadsEmptyBecauseIronIsDefault() {
        ItemStack stack = markedEgg("iron", null);

        assertTrue(SpawnEggStacks.identityFrom(stack).isEmpty());
    }

    @Test
    void familyVariantMarkerReadsAsIdentity() {
        ItemStack stack = markedEgg("zombie", "iron_golem");

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE), SpawnEggStacks.identityFrom(stack).orElseThrow());
    }

    @Test
    void unknownFamilyOrUnknownVariantMarkerReadsEmpty() {
        assertTrue(SpawnEggStacks.identityFrom(markedEgg("diamond", "copper_golem")).isEmpty());
        assertTrue(SpawnEggStacks.identityFrom(markedEgg("obsidian", "iron_golem")).isEmpty());
    }

    @Test
    void ironIsNotAMarkedV4EggVariant() {
        assertThrows(IllegalArgumentException.class, () -> SpawnEggStacks.create(GolemVariant.IRON));
    }

    private static ItemStack markedEgg(String variant, String family) {
        ItemStack stack = new ItemStack(Items.IRON_GOLEM_SPAWN_EGG);
        CompoundTag root = new CompoundTag();
        CompoundTag multigolem = new CompoundTag();
        if (family != null) {
            multigolem.putString("family", family);
        }
        multigolem.putString("variant", variant);
        root.put("multigolem", multigolem);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }
}
