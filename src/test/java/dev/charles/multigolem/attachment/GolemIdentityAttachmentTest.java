package dev.charles.multigolem.attachment;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemFamily;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemIdentityMigration;
import dev.charles.multigolem.identity.GolemIdentityStorage;
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
    void explicitIronFamilyWriteStoresNewIdentityAndOldVariant() {
        MapBackedIdentityStorage storage = new MapBackedIdentityStorage();

        GolemIdentityMigration.write(storage, GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE));

        assertEquals(Optional.of(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE)), storage.rawIdentity());
        assertEquals(Optional.of(GolemVariant.ZOMBIE), storage.rawVariant());
    }

    @Test
    void explicitDefaultIronSetClearsBothAttachments() {
        MapBackedIdentityStorage storage = oldAndNewStorage(
            GolemVariant.DIAMOND,
            GolemIdentity.ofIronVariant(GolemVariant.GOLD)
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
        return oldAndNewStorage(oldVariant, new GolemIdentity(null, GolemVariant.GOLD));
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
