package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;

public final class ZombieGolemFaction {
    private ZombieGolemFaction() {}

    public static boolean shouldCancelTarget(IronGolem self, LivingEntity target) {
        GolemVariant selfVariant = GolemVariantAttachment.get(self);
        GolemVariant targetVariant = target instanceof IronGolem targetGolem
            ? GolemVariantAttachment.get(targetGolem)
            : GolemVariant.IRON;

        if (selfVariant == GolemVariant.ZOMBIE) {
            return !zombieGolemCanTarget(target, targetVariant, self.getLastHurtByMob());
        }
        return false;
    }

    public static boolean zombieGolemCanTarget(LivingEntity target, GolemVariant targetVariant) {
        return zombieGolemCanTarget(target, targetVariant, null);
    }

    public static boolean zombieGolemCanTarget(LivingEntity target, GolemVariant targetVariant, LivingEntity lastHurtByMob) {
        if (target == lastHurtByMob) {
            return true;
        }
        return zombieGolemCanTargetClass(target.getClass(), targetVariant);
    }

    public static boolean zombieGolemCanTargetClass(Class<? extends LivingEntity> targetClass, GolemVariant targetVariant) {
        if (ZombieVillager.class.isAssignableFrom(targetClass) || Zombie.class.isAssignableFrom(targetClass)) {
            return false;
        }
        if (IronGolem.class.isAssignableFrom(targetClass)) {
            return targetVariant != GolemVariant.ZOMBIE;
        }
        // Design intent: Zombie Golems hunt civilians, players, and village defenders, not ordinary hostile mobs.
        return Player.class.isAssignableFrom(targetClass)
            || Villager.class.isAssignableFrom(targetClass)
            || WanderingTrader.class.isAssignableFrom(targetClass);
    }

    public static boolean zombieGolemCanTargetClass(
        Class<? extends LivingEntity> targetClass,
        GolemVariant targetVariant,
        boolean recentAttacker
    ) {
        return recentAttacker || zombieGolemCanTargetClass(targetClass, targetVariant);
    }

    public static boolean nonZombieGolemCanTarget(GolemVariant selfVariant, GolemVariant targetVariant) {
        return selfVariant != GolemVariant.ZOMBIE && targetVariant == GolemVariant.ZOMBIE;
    }

    public static boolean zombieGoalCanTarget(IronGolem self, LivingEntity target) {
        if (GolemVariantAttachment.get(self) != GolemVariant.ZOMBIE) return false;
        GolemVariant targetVariant = target instanceof IronGolem otherGolem
            ? GolemVariantAttachment.get(otherGolem)
            : GolemVariant.IRON;
        return zombieGolemCanTarget(target, targetVariant, self.getLastHurtByMob());
    }

    public static boolean zombieGoalCanTargetClass(Class<? extends LivingEntity> targetClass, GolemVariant targetVariant) {
        return zombieGolemCanTargetClass(targetClass, targetVariant);
    }

    public static boolean defenderGoalCanTarget(IronGolem self, IronGolem target) {
        return defenderGoalCanTarget(GolemVariantAttachment.get(self), GolemVariantAttachment.get(target));
    }

    public static boolean defenderGoalCanTarget(GolemVariant selfVariant, GolemVariant targetVariant) {
        return nonZombieGolemCanTarget(selfVariant, targetVariant);
    }
}
