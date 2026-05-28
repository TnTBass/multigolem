package dev.charles.multigolem.ability;

import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.Test;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombieGolemConversionTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void conversionRollNeverDealsNormalDamage() {
        assertEquals(ZombieGolemConversion.Outcome.CONVERT, ZombieGolemConversion.roll(1.0, 0.99));
        assertEquals(ZombieGolemConversion.Outcome.FAILED_ROLL_NO_DAMAGE, ZombieGolemConversion.roll(0.0, 0.0));
        assertEquals(ZombieGolemConversion.Outcome.FAILED_ROLL_NO_DAMAGE, ZombieGolemConversion.roll(0.25, 0.25));
    }

    @Test
    void disabledConversionStillSuppressesNormalDamage() {
        assertEquals(ZombieGolemConversion.Outcome.FAILED_ROLL_NO_DAMAGE,
            ZombieGolemConversion.roll(false, 1.0, 0.0));
    }

    @Test
    void convertedAdultVillagersDoNotKeepAggressionAndStayAdult() {
        FakeConvertedZombie zombie = new FakeConvertedZombie();

        ZombieGolemConversion.finalizeConvertedZombieState(zombie, false);

        assertTrue(zombie.targetCleared);
        assertTrue(zombie.lastHurtByMobCleared);
        assertTrue(zombie.lastHurtMobCleared);
        assertEquals(List.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.HURT_BY
        ), zombie.erasedMemories);
        assertEquals(Boolean.FALSE, zombie.baby);
    }

    @Test
    void convertedBabyVillagersStayBabyZombieVillagers() {
        FakeConvertedZombie zombie = new FakeConvertedZombie();

        ZombieGolemConversion.finalizeConvertedZombieState(zombie, true);

        assertTrue(zombie.targetCleared);
        assertTrue(zombie.lastHurtByMobCleared);
        assertTrue(zombie.lastHurtMobCleared);
        assertEquals(List.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.HURT_BY
        ), zombie.erasedMemories);
        assertEquals(Boolean.TRUE, zombie.baby);
    }

    private static final class FakeConvertedZombie implements ZombieGolemConversion.ConvertedZombieAggression {
        boolean targetCleared;
        boolean lastHurtByMobCleared;
        boolean lastHurtMobCleared;
        List<MemoryModuleType<?>> erasedMemories = new ArrayList<>();
        Boolean baby;

        @Override
        public void setTarget(LivingEntity target) {
            targetCleared = target == null;
        }

        @Override
        public void setLastHurtByMob(LivingEntity attacker) {
            lastHurtByMobCleared = attacker == null;
        }

        @Override
        public void setLastHurtMob(Entity target) {
            lastHurtMobCleared = target == null;
        }

        @Override
        public void setBaby(boolean baby) {
            this.baby = baby;
        }

        @Override
        public void eraseMemory(MemoryModuleType<?> memoryType) {
            erasedMemories.add(memoryType);
        }
    }
}
