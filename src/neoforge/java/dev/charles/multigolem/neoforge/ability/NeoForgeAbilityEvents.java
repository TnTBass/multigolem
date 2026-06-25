package dev.charles.multigolem.neoforge.ability;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.CopperAbility;
import dev.charles.multigolem.ability.DiamondAbility;
import dev.charles.multigolem.ability.EmeraldAbility;
import dev.charles.multigolem.ability.GoldAbility;
import dev.charles.multigolem.ability.NetheriteAbility;
import dev.charles.multigolem.ability.RedstoneAbility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class NeoForgeAbilityEvents {
    private NeoForgeAbilityEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NeoForgeAbilityEvents::onLevelTick);
        NeoForge.EVENT_BUS.addListener(NeoForgeAbilityEvents::onLivingIncomingDamage);
        MultiGolem.LOG.debug("NeoForgeAbilityEvents: wired all V2 abilities");
    }

    private static void onLevelTick(LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        GoldAbility.onTick(level);
        EmeraldAbility.onTick(level);
        DiamondAbility.onTick(level);
        RedstoneAbility.onTick(level);
    }

    private static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        Entity entity = event.getEntity();
        DamageSource source = event.getSource();
        float amount = event.getAmount();
        boolean cancelDamage = !CopperAbility.allowDamage(entity, source, amount)
            | !NetheriteAbility.allowDamage(entity, source, amount)
            | !DiamondAbility.allowDamage(entity, source, amount);
        if (cancelDamage) {
            event.setCanceled(true);
        }
    }
}
