package dev.charles.multigolem.ability;

import dev.charles.multigolem.config.TierStats;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ZombieGolemEffects {
    private ZombieGolemEffects() {}

    public static List<MobEffectInstance> effects(TierStats stats) {
        List<MobEffectInstance> effects = new ArrayList<>();
        addConfigured(effects, stats.zombieHungerEnabled(), stats.zombieHungerSeconds(),
            stats.zombieHungerAmplifier(), MobEffects.HUNGER);
        addConfigured(effects, stats.zombieNauseaEnabled(), stats.zombieNauseaSeconds(),
            stats.zombieNauseaAmplifier(), MobEffects.NAUSEA);
        addConfigured(effects, stats.zombiePoisonEnabled(), stats.zombiePoisonSeconds(),
            stats.zombiePoisonAmplifier(), MobEffects.POISON);
        return List.copyOf(effects);
    }

    private static void addConfigured(List<MobEffectInstance> effects, Boolean enabled, Integer seconds,
                                      Integer amplifier, Holder<MobEffect> effect) {
        if (!Boolean.TRUE.equals(enabled)) return;
        int duration = Math.max(0, seconds != null ? seconds : 0) * 20;
        if (duration <= 0) return;
        effects.add(new MobEffectInstance(effect, duration, Math.max(0, amplifier != null ? amplifier : 0)));
    }
}
