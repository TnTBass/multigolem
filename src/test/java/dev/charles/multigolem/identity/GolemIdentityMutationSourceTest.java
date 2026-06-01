package dev.charles.multigolem.identity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemIdentityMutationSourceTest {
    @Test
    void explicitMutationPathsUseIdentityAttachment() throws Exception {
        assertSourceContains("src/main/java/dev/charles/multigolem/spawn/GolemCreationHandler.java",
            "GolemIdentityAttachment.set(golem, identityForBodyStates(variant, match))");
        assertSourceContains("src/main/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java",
            "GolemIdentityAttachment.set(golem, GolemIdentity.ofIronVariant(variant))");
        assertSourceContains("src/main/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java",
            "GolemIdentityAttachment.set(golem, GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE))");
        assertSourceContains("src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java",
            "SpawnEggStacks.identityFrom(stack)");
        assertSourceContains("src/main/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java",
            "GolemIdentityAttachment.set(golem, identity.get())");
        assertSourceContains("src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java",
            "SpawnerVariantMarker.readIdentity(entityTag)");
        assertSourceContains("src/main/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java",
            "GolemIdentityAttachment.set(golem, identity.get())");
        assertSourceContains("src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java",
            "SpawnEggStacks.identityFrom(context.getItemInHand())");
        assertSourceContains("src/main/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java",
            "SpawnerVariantMarker.writeIdentity(data.getEntityToSpawn(), identity.get())");
    }

    private static void assertSourceContains(String path, String expected) throws Exception {
        String source = Files.readString(Path.of(path));
        assertTrue(source.contains(expected), path + " must contain " + expected);
    }
}
