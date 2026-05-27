package dev.charles.multigolem.spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ZombieVillageSpawnHandlerTest {

    private final ZombieVillageSpawnHandler handler =
        new ZombieVillageSpawnHandler(new ZombieVillageSpawnResolver(ZombieVillageSpawningConfig.defaults()));

    @Test
    void oneZombieVillagerQualifiesButRegularZombiesAloneDoNot() {
        assertEquals(1, handler.desiredSpawnAttempts(scan(1, 0, 0)));
        assertEquals(0, handler.desiredSpawnAttempts(scan(0, 5, 0)));
    }

    @Test
    void existingZombieGolemsCountAgainstDesiredCount() {
        assertEquals(0, handler.desiredSpawnAttempts(scan(5, 3, 2)));
        assertEquals(1, handler.desiredSpawnAttempts(scan(5, 3, 1)));
    }

    @Test
    void recomputesLiveZombieGolemCountBeforeEverySpawnAttempt() {
        FakeZombieVillageScan scan = new FakeZombieVillageScan()
            .withZombieVillagers(5)
            .withRegularZombies(3)
            .withLiveZombieGolemCounts(0, 1, 2);

        handler.maintain(scan);

        assertEquals(List.of("count", "spawn", "count", "spawn", "count"), scan.events());
        assertEquals(2, scan.spawnAttempts());
    }

    @Test
    void conversionCreatedZombieVillagersCannotExceedMaxCap() {
        FakeZombieVillageScan scan = new FakeZombieVillageScan()
            .withZombieVillagers(1)
            .withRegularZombies(3)
            .withLiveZombieGolemCounts(2);

        handler.maintain(scan);

        assertEquals(0, scan.spawnAttempts());
    }

    @Test
    void runtimeScanCoversTheVillageArea() {
        assertEquals(32.0, ZombieVillageSpawnHandler.scanRange(), 0.0001);
    }

    private static FakeZombieVillageScan scan(int zombieVillagers, int regularZombies, int zombieGolems) {
        return new FakeZombieVillageScan()
            .withZombieVillagers(zombieVillagers)
            .withRegularZombies(regularZombies)
            .withLiveZombieGolemCounts(zombieGolems);
    }

    private static final class FakeZombieVillageScan implements ZombieVillageSpawnHandler.ZombieVillageScan {
        private int zombieVillagers;
        private int regularZombies;
        private final List<Integer> liveZombieGolemCounts = new ArrayList<>();
        private final List<String> events = new ArrayList<>();
        private int countCalls;
        private int spawnAttempts;

        FakeZombieVillageScan withZombieVillagers(int zombieVillagers) {
            this.zombieVillagers = zombieVillagers;
            return this;
        }

        FakeZombieVillageScan withRegularZombies(int regularZombies) {
            this.regularZombies = regularZombies;
            return this;
        }

        FakeZombieVillageScan withLiveZombieGolemCounts(int... counts) {
            liveZombieGolemCounts.clear();
            for (int count : counts) {
                liveZombieGolemCounts.add(count);
            }
            return this;
        }

        @Override
        public int countZombieVillagers() {
            return zombieVillagers;
        }

        @Override
        public int countRegularZombies() {
            return regularZombies;
        }

        @Override
        public int countLiveZombieGolems() {
            events.add("count");
            int index = Math.min(countCalls, liveZombieGolemCounts.size() - 1);
            countCalls++;
            return liveZombieGolemCounts.get(index);
        }

        @Override
        public Optional<BlockPos> findSafeVillageSpawnPosition() {
            return Optional.of(BlockPos.ZERO);
        }

        @Override
        public boolean spawnZombieGolem(BlockPos pos) {
            events.add("spawn");
            spawnAttempts++;
            return true;
        }

        List<String> events() {
            return List.copyOf(events);
        }

        int spawnAttempts() {
            return spawnAttempts;
        }
    }
}
