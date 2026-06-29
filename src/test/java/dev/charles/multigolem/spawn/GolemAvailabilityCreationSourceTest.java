package dev.charles.multigolem.spawn;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemAvailabilityCreationSourceTest {
    @Test
    void spawnEggSpawnerChecksAvailabilityBeforePermissions() throws IOException {
        String source = read("src/common/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java");

        assertBefore(source,
            "GolemAvailabilityGuards.canCreate(MultiGolem.config(), requested)",
            "MultiGolemPermissions.canCreate(player, variant)");
    }

    @Test
    void spawnEggSpawnerMarkingChecksAvailabilityBeforePermissions() throws IOException {
        String source = read("src/common/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java");

        assertBefore(source,
            "GolemAvailabilityGuards.canCreate(MultiGolem.config(), requested)",
            "MultiGolemPermissions.canCreate(player, variant)");
    }

    @Test
    void baseSpawnerRejectsUnavailableMarkedIdentityBeforeAttachment() throws IOException {
        String source = read("src/common/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java");

        assertBefore(source,
            "GolemAvailabilityGuards.canCreate(MultiGolem.config(), identity.get())",
            "GolemIdentityAttachment.set(golem, identity.get())");
    }

    @Test
    void playerConstructionChecksAvailabilityBeforePermissions() throws IOException {
        String source = read("src/common/java/dev/charles/multigolem/spawn/GolemCreationHandler.java");

        assertBefore(source,
            "GolemAvailabilityGuards.canCreate(MultiGolem.config(), identity)",
            "MultiGolemPermissions.canCreate(player, variant)");
    }

    @Test
    void zombieVillageSkipsScansWhenZombieGolemUnavailable() throws IOException {
        String source = read("src/common/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java");

        assertBefore(source,
            "GolemAvailabilityGuards.canCreate(MultiGolem.config(), GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE))",
            "new ZombieVillageSpawnResolver");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path)).replace("\r\n", "\n");
    }

    private static void assertBefore(String source, String earlier, String later) {
        int earlierIndex = source.indexOf(earlier);
        int laterIndex = source.indexOf(later);
        assertTrue(earlierIndex >= 0, "missing source fragment: " + earlier);
        assertTrue(laterIndex >= 0, "missing source fragment: " + later);
        assertTrue(earlierIndex < laterIndex, earlier + " should appear before " + later);
    }
}
