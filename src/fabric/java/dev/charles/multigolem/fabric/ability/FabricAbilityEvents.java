package dev.charles.multigolem.fabric.ability;

import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.CopperAbility;
import dev.charles.multigolem.ability.DiamondAbility;
import dev.charles.multigolem.ability.EmeraldAbility;
import dev.charles.multigolem.ability.GoldAbility;
import dev.charles.multigolem.ability.NetheriteAbility;
import dev.charles.multigolem.ability.RedstoneAbility;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class FabricAbilityEvents {
    private FabricAbilityEvents() {}

    public static void register() {
        ServerTickEvents.START_LEVEL_TICK.register(GoldAbility::onTick);
        ServerTickEvents.START_LEVEL_TICK.register(EmeraldAbility::onTick);
        ServerTickEvents.START_LEVEL_TICK.register(DiamondAbility::onTick);
        ServerTickEvents.START_LEVEL_TICK.register(RedstoneAbility::onTick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CopperAbility::allowDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(NetheriteAbility::allowDamage);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(DiamondAbility::allowDamage);
        MultiGolem.LOG.debug("FabricAbilityEvents: wired all V2 abilities");
    }
}
