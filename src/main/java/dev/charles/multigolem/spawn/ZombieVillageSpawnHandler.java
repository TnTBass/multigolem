package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemSpawnOrigin;
import dev.charles.multigolem.attachment.GolemSpawnOriginAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.phys.AABB;

public final class ZombieVillageSpawnHandler {
    private final ZombieVillageSpawnResolver resolver;

    public ZombieVillageSpawnHandler(ZombieVillageSpawnResolver resolver) {
        this.resolver = resolver;
    }

    public static void maintain(ServerLevel level, BlockPos center) {
        try {
            ZombieVillageSpawnHandler handler = new ZombieVillageSpawnHandler(
                new ZombieVillageSpawnResolver(MultiGolem.config().zombieVillageSpawning()));
            handler.maintain(new RuntimeZombieVillageScan(level, center));
        } catch (Throwable t) {
            MultiGolem.LOG.error("Failed to maintain zombie village golems near {}", center, t);
        }
    }

    public int desiredSpawnAttempts(ZombieVillageScan scan) {
        int desired = resolver.desiredCount(
            scan.countZombieVillagers(),
            scan.countRegularZombies(),
            scan.countLiveZombieGolems());
        return Math.max(0, desired - scan.countLiveZombieGolems());
    }

    public void maintain(ZombieVillageScan scan) {
        int zombieVillagers = scan.countZombieVillagers();
        int regularZombies = scan.countRegularZombies();
        int currentZombieGolems = scan.countLiveZombieGolems();
        int desired = resolver.desiredCount(zombieVillagers, regularZombies, currentZombieGolems);
        while (currentZombieGolems < desired) {
            Optional<BlockPos> pos = scan.findSafeVillageSpawnPosition();
            if (pos.isEmpty()) break;
            if (!scan.spawnZombieGolem(pos.get())) break;
            int updatedZombieGolems = scan.countLiveZombieGolems();
            if (updatedZombieGolems <= currentZombieGolems) break;
            currentZombieGolems = updatedZombieGolems;
        }
    }

    public interface ZombieVillageScan {
        int countZombieVillagers();
        int countRegularZombies();
        int countLiveZombieGolems();
        Optional<BlockPos> findSafeVillageSpawnPosition();
        boolean spawnZombieGolem(BlockPos pos);
    }

    private static final class RuntimeZombieVillageScan implements ZombieVillageScan {
        private static final double SCAN_RANGE = 10.0;
        private final ServerLevel level;
        private final BlockPos center;
        private final AABB box;

        RuntimeZombieVillageScan(ServerLevel level, BlockPos center) {
            this.level = level;
            this.center = center;
            this.box = new AABB(center).inflate(SCAN_RANGE, SCAN_RANGE, SCAN_RANGE);
        }

        @Override
        public int countZombieVillagers() {
            return level.getEntitiesOfClass(ZombieVillager.class, box, entity -> entity.isAlive() && !entity.isRemoved()).size();
        }

        @Override
        public int countRegularZombies() {
            return level.getEntitiesOfClass(Zombie.class, box,
                entity -> !(entity instanceof ZombieVillager) && entity.isAlive() && !entity.isRemoved()).size();
        }

        @Override
        public int countLiveZombieGolems() {
            return level.getEntitiesOfClass(IronGolem.class, box,
                entity -> entity.isAlive()
                    && !entity.isRemoved()
                    && GolemVariantAttachment.get(entity) == GolemVariant.ZOMBIE).size();
        }

        @Override
        public Optional<BlockPos> findSafeVillageSpawnPosition() {
            return Optional.of(center);
        }

        @Override
        public boolean spawnZombieGolem(BlockPos pos) {
            Optional<IronGolem> spawned = SpawnUtil.trySpawnMob(
                EntityType.IRON_GOLEM,
                EntitySpawnReason.MOB_SUMMONED,
                level,
                pos,
                10,
                8,
                6,
                SpawnUtil.Strategy.LEGACY_IRON_GOLEM,
                false
            );
            spawned.ifPresent(golem -> {
                GolemVariantAttachment.set(golem, GolemVariant.ZOMBIE);
                GolemSpawnOriginAttachment.set(golem, GolemSpawnOrigin.VILLAGE);
                golem.setHealth(golem.getMaxHealth());
            });
            return spawned.isPresent();
        }
    }
}
