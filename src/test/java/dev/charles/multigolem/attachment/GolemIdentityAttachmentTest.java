package dev.charles.multigolem.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemIdentityMigration;
import dev.charles.multigolem.identity.GolemIdentityStorage;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemIdentityAttachmentTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void missingIdentityAndMissingOldVariantResolvesToDefaultIron() {
        MapBackedIdentityStorage storage = new MapBackedIdentityStorage();

        assertEquals(GolemIdentity.defaultIron(), GolemIdentityMigration.resolve(storage));
        assertTrue(storage.rawIdentity().isEmpty());
        assertTrue(storage.rawVariant().isEmpty());
    }

    @Test
    void oldOnlyVariantResolvesToIronFamilyIdentityWithoutClearingOldData() {
        MapBackedIdentityStorage storage = oldOnlyStorage(GolemVariant.DIAMOND);

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND), GolemIdentityMigration.resolve(storage));
        assertEquals(Optional.of(GolemVariant.DIAMOND), storage.rawVariant());
        assertTrue(storage.rawIdentity().isEmpty());
    }

    @Test
    void newIdentityWinsOverConflictingOldVariant() {
        MapBackedIdentityStorage storage = oldAndNewStorage(
            GolemVariant.DIAMOND,
            GolemIdentity.ofIronVariant(GolemVariant.GOLD)
        );

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.GOLD), GolemIdentityMigration.resolve(storage));
        assertEquals(Optional.of(GolemVariant.DIAMOND), storage.rawVariant());
    }

    @Test
    void invalidNewIdentityFallsBackToOldVariantWithoutClearingEitherAttachment() {
        MapBackedIdentityStorage storage = invalidNewAndOldStorage(GolemVariant.EMERALD);

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.EMERALD), GolemIdentityMigration.resolve(storage));
        assertTrue(storage.rawIdentity().isPresent());
        assertEquals(Optional.of(GolemVariant.EMERALD), storage.rawVariant());
    }

    @Test
    void phaseTwoIdentityWithMissingSurfaceStillResolves() {
        MapBackedIdentityStorage storage = new MapBackedIdentityStorage();
        storage.setRawIdentity(new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.ZOMBIE, Optional.empty()));

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE), GolemIdentityMigration.resolve(storage));
    }

    @Test
    void validCopperSurfaceIdentityWinsOverConflictingOldVariant() {
        GolemIdentity copper = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.WEATHERED, true));
        MapBackedIdentityStorage storage = oldAndNewStorage(GolemVariant.GOLD, copper);

        assertEquals(copper, GolemIdentityMigration.resolve(storage));
        assertEquals(Optional.of(GolemVariant.GOLD), storage.rawVariant());
    }

    @Test
    void invalidNonCopperSurfaceFallsBackToOldVariantWithoutMutating() {
        GolemSurfaceState surface = new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false);
        MapBackedIdentityStorage storage = oldAndNewStorage(
            GolemVariant.EMERALD,
            new GolemIdentity(GolemFamily.IRON_GOLEM, GolemVariant.GOLD, Optional.of(surface))
        );

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.EMERALD), GolemIdentityMigration.resolve(storage));
        assertEquals(Optional.of(GolemVariant.EMERALD), storage.rawVariant());
    }

    @Test
    void reservedCopperGolemFamilyFallsBackToOldVariantWithoutBeingTreatedAsRuntimeValid() {
        MapBackedIdentityStorage storage = oldAndNewStorage(
            GolemVariant.NETHERITE,
            new GolemIdentity(GolemFamily.COPPER_GOLEM, GolemVariant.COPPER, Optional.empty())
        );

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.NETHERITE), GolemIdentityMigration.resolve(storage));
    }

    @Test
    void explicitIronFamilyWriteStoresNewIdentityAndOldVariant() {
        MapBackedIdentityStorage storage = new MapBackedIdentityStorage();

        GolemIdentityMigration.write(storage, GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE));

        assertEquals(Optional.of(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE)), storage.rawIdentity());
        assertEquals(Optional.of(GolemVariant.ZOMBIE), storage.rawVariant());
    }

    @Test
    void writingCopperSurfaceDualWritesOldCopperVariantOnly() {
        MapBackedIdentityStorage storage = new MapBackedIdentityStorage();
        GolemIdentity identity = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false));

        GolemIdentityMigration.write(storage, identity);

        assertEquals(Optional.of(identity), storage.rawIdentity());
        assertEquals(Optional.of(GolemVariant.COPPER), storage.rawVariant());
    }

    @Test
    void reservedCopperGolemFamilyDoesNotEnterRuntimeAsSupportedIdentity() {
        MapBackedIdentityStorage storage = new MapBackedIdentityStorage();

        GolemIdentityMigration.write(storage,
            new GolemIdentity(GolemFamily.COPPER_GOLEM, GolemVariant.COPPER, Optional.empty()));

        assertEquals(GolemIdentity.defaultIron(), GolemIdentityMigration.resolve(storage));
        assertTrue(storage.rawIdentity().isEmpty());
        assertTrue(storage.rawVariant().isEmpty());
    }

    @Test
    void explicitDefaultIronSetClearsBothAttachments() {
        MapBackedIdentityStorage storage = oldAndNewStorage(
            GolemVariant.DIAMOND,
            GolemIdentity.ofIronVariant(GolemVariant.COPPER, GolemSurfaceState.DEFAULT)
        );

        GolemIdentityMigration.write(storage, GolemIdentity.defaultIron());

        assertTrue(storage.rawIdentity().isEmpty());
        assertTrue(storage.rawVariant().isEmpty());
    }

    private static MapBackedIdentityStorage oldOnlyStorage(GolemVariant variant) {
        MapBackedIdentityStorage storage = new MapBackedIdentityStorage();
        storage.setRawVariant(variant);
        return storage;
    }

    private static MapBackedIdentityStorage oldAndNewStorage(GolemVariant oldVariant, GolemIdentity identity) {
        MapBackedIdentityStorage storage = oldOnlyStorage(oldVariant);
        storage.setRawIdentity(identity);
        return storage;
    }

    private static MapBackedIdentityStorage invalidNewAndOldStorage(GolemVariant oldVariant) {
        return oldAndNewStorage(oldVariant, new GolemIdentity(null, GolemVariant.GOLD, Optional.empty()));
    }

    private static final class MapBackedIdentityStorage implements GolemIdentityStorage {
        private GolemIdentity identity;
        private GolemVariant variant;

        @Override
        public Optional<GolemIdentity> rawIdentity() {
            return Optional.ofNullable(identity);
        }

        @Override
        public void setRawIdentity(GolemIdentity identity) {
            this.identity = identity;
        }

        @Override
        public void clearRawIdentity() {
            this.identity = null;
        }

        @Override
        public Optional<GolemVariant> rawVariant() {
            return Optional.ofNullable(variant);
        }

        @Override
        public void setRawVariant(GolemVariant variant) {
            this.variant = variant;
        }

        @Override
        public void clearRawVariant() {
            this.variant = null;
        }
    }
}
