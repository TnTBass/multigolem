package dev.charles.multigolem;

import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiGolemRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        System.setProperty("net.bytebuddy.experimental", "true");
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
        String multiGolem = Files.readString(Path.of("src/common/java/dev/charles/multigolem/MultiGolem.java"));
        String creation = Files.readString(Path.of("src/common/java/dev/charles/multigolem/spawn/GolemCreationHandler.java"));

        assertTrue(multiGolem.contains("GolemVariant.spawnEggVariants()"));
        assertTrue(multiGolem.contains("GolemVariant.lootVariants()"));
        assertTrue(multiGolem.contains("HasGolemVariantLootCondition.builder(variant)"));
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

    @Test
    void variantDropCountsPreserveInclusiveIntegerBounds() {
        LootContext context = mock(LootContext.class);
        when(context.getRandom()).thenReturn(RandomSource.create(263L));
        for (GolemVariant variant : MultiGolem.lootVariants()) {
            MultiGolem.VariantLootDrop drop = MultiGolem.lootDropFor(variant);
            var provider = MultiGolem.variantDropCount(drop.min(), drop.max()).value();
            var observed = new java.util.HashSet<Integer>();
            for (int i = 0; i < 256; i++) {
                int count = provider.getIntUnsafe(context);
                assertTrue(count >= drop.min() && count <= drop.max(), variant.name());
                observed.add(count);
            }
            assertTrue(observed.contains(drop.min()), variant + " minimum never sampled");
            assertTrue(observed.contains(drop.max()), variant + " maximum never sampled");
        }
    }
}
