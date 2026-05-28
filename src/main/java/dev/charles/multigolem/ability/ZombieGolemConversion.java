package dev.charles.multigolem.ability;

import dev.charles.multigolem.config.TierStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;

public final class ZombieGolemConversion {
    private ZombieGolemConversion() {}

    public enum Outcome {
        CONVERT,
        FAILED_ROLL_NO_DAMAGE
    }

    public static Outcome roll(double chance, double roll) {
        return roll < chance ? Outcome.CONVERT : Outcome.FAILED_ROLL_NO_DAMAGE;
    }

    public static Outcome roll(boolean enabled, double chance, double roll) {
        return enabled ? roll(chance, roll) : Outcome.FAILED_ROLL_NO_DAMAGE;
    }

    public static boolean suppressesNormalDamage(Object target) {
        return target instanceof Villager || target instanceof WanderingTrader;
    }

    public static boolean handle(ServerLevel level, Object target, TierStats stats, double randomRoll) {
        if (target instanceof Villager villager) {
            if (roll(Boolean.TRUE.equals(stats.zombieConvertVillagersEnabled()),
                    stats.zombieVillagerConversionChance(), randomRoll) == Outcome.CONVERT) {
                convertVillager(level, villager);
            }
            return true;
        }
        if (target instanceof WanderingTrader trader) {
            if (roll(Boolean.TRUE.equals(stats.zombieConvertWanderingTradersEnabled()),
                    stats.zombieWanderingTraderConversionChance(), randomRoll) == Outcome.CONVERT) {
                convertTrader(level, trader);
            }
            return true;
        }
        return false;
    }

    private static void convertVillager(ServerLevel level, Villager villager) {
        MerchantOffers offers = villager.getOffers().copy();
        int xp = villager.getVillagerXp();
        // Capture before convertTo invalidates the source entity state used by the conversion callback.
        boolean sourceWasBaby = villager.isBaby();
        villager.convertTo(EntityType.ZOMBIE_VILLAGER, ConversionParams.single(villager, false, false), zombie -> {
            zombie.setVillagerData(villager.getVillagerData());
            zombie.setTradeOffers(offers);
            zombie.setVillagerXp(xp);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.CONVERSION, null);
            finalizeConvertedZombieState(zombie, sourceWasBaby);
        });
    }

    private static void convertTrader(ServerLevel level, WanderingTrader trader) {
        MerchantOffers offers = trader.getOffers().copy();
        trader.convertTo(EntityType.ZOMBIE_VILLAGER, ConversionParams.single(trader, false, false), zombie -> {
            zombie.setTradeOffers(offers);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.CONVERSION, null);
            finalizeConvertedZombieState(zombie, false);
        });
    }

    static void finalizeConvertedZombieState(Mob zombie, boolean baby) {
        finalizeConvertedZombieState(new ConvertedZombieAggression() {
            @Override
            public void setTarget(LivingEntity target) {
                zombie.setTarget(target);
            }

            @Override
            public void setLastHurtByMob(LivingEntity attacker) {
                zombie.setLastHurtByMob(attacker);
            }

            @Override
            public void setLastHurtMob(Entity target) {
                zombie.setLastHurtMob(target);
            }

            @Override
            public void setBaby(boolean baby) {
                zombie.setBaby(baby);
            }

            @Override
            public void eraseMemory(MemoryModuleType<?> memoryType) {
                zombie.getBrain().eraseMemory(memoryType);
            }
        }, baby);
    }

    static void finalizeConvertedZombieState(ConvertedZombieAggression zombie, boolean baby) {
        zombie.setTarget(null);
        zombie.setLastHurtByMob(null);
        zombie.setLastHurtMob(null);
        zombie.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        zombie.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        zombie.eraseMemory(MemoryModuleType.HURT_BY);
        zombie.setBaby(baby);
    }

    interface ConvertedZombieAggression {
        void setTarget(LivingEntity target);
        void setLastHurtByMob(LivingEntity attacker);
        void setLastHurtMob(Entity target);
        void setBaby(boolean baby);
        void eraseMemory(MemoryModuleType<?> memoryType);
    }
}
