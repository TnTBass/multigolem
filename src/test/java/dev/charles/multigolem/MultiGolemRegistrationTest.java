package dev.charles.multigolem;

import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiGolemRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void creativeSpawnEggVariantsIncludeEveryNonIronVariant() {
        assertIterableEquals(
            GolemVariant.spawnEggVariants(),
            MultiGolem.creativeSpawnEggVariants()
        );
    }

    @Test
    void productionRegistrationUsesCatalogDerivedVariantSets() throws Exception {
        String multiGolem = Files.readString(Path.of("src/main/java/dev/charles/multigolem/MultiGolem.java"));
        String creation = Files.readString(Path.of("src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java"));

        assertTrue(multiGolem.contains("GolemVariant.spawnEggVariants()"));
        assertTrue(multiGolem.contains("GolemVariant.lootVariants()"));
        assertTrue(creation.contains("GolemVariant.multiGolemPlayerBuildableVariants()"));
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
