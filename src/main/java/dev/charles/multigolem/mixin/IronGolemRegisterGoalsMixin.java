package dev.charles.multigolem.mixin;

import dev.charles.multigolem.ability.ZombieGolemFaction;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolem.class)
public abstract class IronGolemRegisterGoalsMixin {
    @Shadow @Final protected GoalSelector targetSelector;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void multigolem$registerZombieFactionTargets(CallbackInfo ci) {
        IronGolem self = (IronGolem) (Object) this;
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
            self, Player.class, 10, true, false, (target, level) -> ZombieGolemFaction.zombieGoalCanTarget(self, target)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
            self, Villager.class, 10, true, false, (target, level) -> ZombieGolemFaction.zombieGoalCanTarget(self, target)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
            self, WanderingTrader.class, 10, true, false, (target, level) -> ZombieGolemFaction.zombieGoalCanTarget(self, target)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
            self, IronGolem.class, 10, true, false, (target, level) ->
                ZombieGolemFaction.zombieGoalCanTarget(self, target)
                    || (target instanceof IronGolem targetGolem
                        && ZombieGolemFaction.defenderGoalCanTarget(self, targetGolem))));
    }
}
