package dev.charles.multigolem;

import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultiGolemRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void creativeSpawnEggVariantsIncludeEveryNonIronVariant() {
        assertIterableEquals(
            GolemVariant.nonIronVariants(),
            MultiGolem.creativeSpawnEggVariants()
        );
    }

    @Test
    void zombieLootDropsRottenFlesh() {
        MultiGolem.VariantLootDrop drop = MultiGolem.lootDropFor(GolemVariant.ZOMBIE);

        assertEquals(Items.ROTTEN_FLESH, drop.item());
        assertEquals(3, drop.min());
        assertEquals(5, drop.max());
    }

    @Test
    void ironLootUsesVanillaTableOnly() {
        assertThrows(IllegalArgumentException.class, () -> MultiGolem.lootDropFor(GolemVariant.IRON));
    }
}
