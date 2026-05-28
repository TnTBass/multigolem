package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
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
            GolemVariant.EMERALD,
            GolemVariant.DIAMOND,
            GolemVariant.NETHERITE,
            GolemVariant.ZOMBIE
        )) {
            ItemStack stack = SpawnEggStacks.create(variant);

            assertTrue(stack.is(Items.IRON_GOLEM_SPAWN_EGG));
            assertEquals(variant, SpawnEggStacks.variantFrom(stack).orElseThrow());
            assertEquals("{multigolem:{variant:\"" + variant.id() + "\"}}", SpawnEggStacks.customDataSnbt(stack));
        }
    }

    @Test
    void markedStacksHaveVariantSpawnEggNames() {
        assertEquals("Copper Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.COPPER).get(DataComponents.ITEM_NAME).getString());
        assertEquals("Gold Golem Spawn Egg",
            SpawnEggStacks.create(GolemVariant.GOLD).get(DataComponents.ITEM_NAME).getString());
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
    void ironIsNotAMarkedV4EggVariant() {
        assertThrows(IllegalArgumentException.class, () -> SpawnEggStacks.create(GolemVariant.IRON));
    }
}
