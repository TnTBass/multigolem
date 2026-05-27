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
            return !zombieGolemCanTarget(target, targetVariant);
        }
        return false;
    }

    public static boolean zombieGolemCanTarget(LivingEntity target, GolemVariant targetVariant) {
        return zombieGolemCanTargetClass(target.getClass(), targetVariant);
    }

    public static boolean zombieGolemCanTargetClass(Class<? extends LivingEntity> targetClass, GolemVariant targetVariant) {
        if (ZombieVillager.class.isAssignableFrom(targetClass) || Zombie.class.isAssignableFrom(targetClass)) {
            return false;
        }
        if (IronGolem.class.isAssignableFrom(targetClass)) {
            return targetVariant != GolemVariant.ZOMBIE;
        }
        return Player.class.isAssignableFrom(targetClass)
            || Villager.class.isAssignableFrom(targetClass)
            || WanderingTrader.class.isAssignableFrom(targetClass);
    }

    public static boolean nonZombieGolemCanTarget(GolemVariant selfVariant, GolemVariant targetVariant) {
        return selfVariant != GolemVariant.ZOMBIE && targetVariant == GolemVariant.ZOMBIE;
    }
}
