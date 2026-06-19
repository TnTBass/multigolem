package dev.charles.multigolem.identity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemIdentityMutationSourceTest {
    @Test
    void explicitMutationPathsUseIdentityAttachment() throws Exception {
        assertSourceContains("src/common/java/dev/charles/multigolem/spawn/GolemCreationHandler.java",
            "GolemIdentityAttachment.set(golem, identityFromMatchBodyStates(variant, match))");
        assertSourceContains("src/common/java/dev/charles/multigolem/spawn/VillageGolemSpawnHandler.java",
            "GolemIdentityAttachment.set(golem, GolemIdentity.ofIronVariant(variant))");
        assertSourceContains("src/common/java/dev/charles/multigolem/spawn/ZombieVillageSpawnHandler.java",
            "GolemIdentityAttachment.set(golem, GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE))");
        assertSourceContains("src/common/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java",
            "SpawnEggStacks.identityFrom(stack)");
        assertSourceContains("src/common/java/dev/charles/multigolem/spawn/SpawnEggVariantSpawner.java",
            "GolemIdentityAttachment.set(golem, identity.get())");
        assertSourceContains("src/common/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java",
            "SpawnerVariantMarker.readIdentity(entityTag)");
        assertSourceContains("src/common/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java",
            "GolemIdentityAttachment.set(golem, identity.get())");
        assertSourceContains("src/common/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java",
            "SpawnEggStacks.identityFrom(context.getItemInHand())");
        assertSourceContains("src/common/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java",
            "SpawnerVariantMarker.writeIdentity(data.getEntityToSpawn(), identity.get())");
    }

    @Test
    void spawnEggAndSpawnerMixinsUseMinecraft262Descriptors() throws Exception {
        assertSourceContains("src/common/java/dev/charles/multigolem/mixin/SpawnEggItemMixin.java",
            "spawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;ZZ)Lnet/minecraft/world/InteractionResult;");
        assertSourceContains("src/common/java/dev/charles/multigolem/mixin/BaseSpawnerMixin.java",
            "Lnet/minecraft/world/entity/EntityType;loadEntityRecursive(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;Lnet/minecraft/world/entity/EntityProcessor;)Lnet/minecraft/world/entity/Entity;");
    }

    private static void assertSourceContains(String path, String expected) throws Exception {
        String source = Files.readString(Path.of(path));
        assertTrue(source.contains(expected), path + " must contain " + expected);
    }
}
