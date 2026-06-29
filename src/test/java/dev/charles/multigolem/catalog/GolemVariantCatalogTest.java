package dev.charles.multigolem.catalog;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.GolemAvailability;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static dev.charles.multigolem.GolemVariant.COPPER;
import static dev.charles.multigolem.GolemVariant.DIAMOND;
import static dev.charles.multigolem.GolemVariant.EMERALD;
import static dev.charles.multigolem.GolemVariant.GOLD;
import static dev.charles.multigolem.GolemVariant.NETHERITE;
import static dev.charles.multigolem.GolemVariant.REDSTONE;
import static dev.charles.multigolem.GolemVariant.ZOMBIE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemVariantCatalogTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void everyVariantHasExactlyOneCatalogEntry() {
        assertEquals(EnumSet.allOf(GolemVariant.class), GolemVariantCatalog.variants());
        assertEquals(GolemVariant.values().length, GolemVariantCatalog.entries().size());
    }

    @Test
    void everyCatalogEntryDeclaresRequiredSurfaceIntent() {
        for (GolemVariantSpec spec : GolemVariantCatalog.entries()) {
            assertEquals(GolemFamily.IRON_GOLEM, spec.family(), spec.variant().id());
            assertNotNull(spec.bodyBlockMatcherSupplier(), spec.variant().id());
            assertNotNull(spec.healItem(), spec.variant().id());
            assertNotNull(spec.dropItem(), spec.variant().id());
            assertNotNull(spec.permissionSuffix(), spec.variant().id());
            assertTrue(spec.lootMin() <= spec.lootMax(), spec.variant().id());
            if (spec.spawnEggEnabled()) {
                assertFalse(spec.spawnEggModelPath().contains("/"), "spawnEggModelPath must be a bare filename");
                assertFalse(spec.spawnEggTexturePath().contains("/"), "spawnEggTexturePath must be a bare filename");
                assertTrue(spec.spawnEggModelPath().endsWith("_golem_spawn_egg.json"));
                assertTrue(spec.spawnEggTexturePath().endsWith("_golem_spawn_egg.png"));
            }
            if (spec.renderable()) {
                assertFalse(spec.entityTexturePath().contains("/"), "entityTexturePath must be a bare filename");
                assertTrue(spec.entityTexturePath().endsWith("_golem.png"));
            }
        }
    }

    @Test
    void catalogDerivedSetsMatchCurrentIntent() {
        List<GolemVariant> customIronFamily = List.of(COPPER, REDSTONE, GOLD, EMERALD, DIAMOND, NETHERITE, ZOMBIE);

        assertEquals(customIronFamily, GolemVariant.spawnEggVariants());
        assertEquals(customIronFamily, GolemVariant.lootVariants());
        assertEquals(customIronFamily, GolemVariant.multiGolemPlayerBuildableVariants());
        assertEquals(customIronFamily, GolemVariant.nonIronVariants());
        assertFalse(Arrays.stream(GolemVariant.values())
            .anyMatch(v -> v.id().equals("lapis")));
    }

    @Test
    void catalogEntriesExposeFamilyVariantIdentity() {
        assertEquals(
            GolemIdentity.ofIronVariant(GolemVariant.COPPER),
            GolemVariantCatalog.require(GolemVariant.COPPER).identity()
        );
        assertEquals(
            new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.ZOMBIE),
            GolemVariantCatalog.require(GolemVariant.ZOMBIE).identity()
        );
    }

    @Test
    void catalogCanFilterAvailableIdentitiesWithoutFlatteningFamilies() {
        GolemAvailability availability = GolemAvailability.defaults()
            .withVariant(GolemFamily.IRON_GOLEM, GolemVariant.DIAMOND, false);

        List<GolemIdentity> identities = GolemVariantCatalog.identitiesWhereAvailable(availability);

        assertTrue(identities.contains(GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
        assertFalse(identities.contains(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
    }

    @Test
    void redstoneCatalogEntryMatchesDesignDefaults() {
        GolemVariantSpec redstone = GolemVariantCatalog.require(REDSTONE);

        assertEquals(GolemFamily.IRON_GOLEM, redstone.family());
        assertEquals(3, redstone.lootMin());
        assertEquals(5, redstone.lootMax());
        assertTrue(redstone.spawnEggEnabled());
        assertTrue(redstone.lootEnabled());
        assertTrue(redstone.playerBuildable());
        assertTrue(redstone.permissionEnabled());
        assertTrue(redstone.renderable());
        assertEquals("redstone", redstone.permissionSuffix());
    }

    @Test
    void ironDeclaresVanillaOwnedCatalogSurfaces() {
        GolemVariantSpec iron = GolemVariantCatalog.require(GolemVariant.IRON);

        assertFalse(iron.spawnEggEnabled());
        assertFalse(iron.lootEnabled());
        assertFalse(iron.playerBuildable());
        assertTrue(iron.healEnabled());
        assertTrue(iron.permissionEnabled());
        assertFalse(iron.renderable());
    }
}
