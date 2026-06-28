package dev.charles.multigolem.ability;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;

public final class LapisAbility {

    private LapisAbility() {}

    public static void onTick(ServerLevel world) {
    }

    public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        return true;
    }

    public static boolean allowEffectApplication(MobEffectInstance effect, LivingEntity entity) {
        return true;
    }

    public static boolean shouldScan(long now, long lastScan, int intervalTicks) {
        return now - lastScan >= Math.max(1, intervalTicks);
    }

    public static boolean isMagicDamage(DamageSource source) {
        return source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    public static boolean shouldProtectTargetClass(
        Class<? extends LivingEntity> targetClass,
        GolemVariant targetVariant,
        boolean playersEnabled
    ) {
        if (Player.class.isAssignableFrom(targetClass)) {
            return playersEnabled;
        }
        if (Villager.class.isAssignableFrom(targetClass)
            || WanderingTrader.class.isAssignableFrom(targetClass)) {
            return true;
        }
        if (IronGolem.class.isAssignableFrom(targetClass)) {
            return targetVariant != GolemVariant.ZOMBIE;
        }
        return false;
    }

    public static boolean isConfiguredEffect(MobEffectInstance effect, TierStats stats) {
        if (effect == null || stats == null || stats.lapisWardEffectIds() == null) {
            return false;
        }
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        return id != null && stats.lapisWardEffectIds().contains(id.toString());
    }
}
