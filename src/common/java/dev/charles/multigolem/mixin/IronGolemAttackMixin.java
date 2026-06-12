package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.DiamondAbility;
import dev.charles.multigolem.ability.GolemCombatRules;
import dev.charles.multigolem.ability.TargetFilter;
import dev.charles.multigolem.ability.ZombieGolemConversion;
import dev.charles.multigolem.ability.ZombieGolemEffects;
import dev.charles.multigolem.attachment.GolemAbilityState;
import dev.charles.multigolem.attachment.GolemAbilityStateAttachment;
import dev.charles.multigolem.attachment.GolemSpawnOriginAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Single attack-hook mixin for V2. Dispatches diamond on-attack lightning (Task 16)
 * and netherite ignite-on-hit (Task 15) after vanilla doHurtTarget returns true.
 */
@Mixin(IronGolem.class)
public abstract class IronGolemAttackMixin {

    @Inject(method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$zombieCivilianConversion(ServerLevel level, Entity target,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (target == null || !target.isAlive() || target.isRemoved()) return;
        IronGolem self = (IronGolem) (Object) this;
        if (GolemVariantAttachment.get(self) != GolemVariant.ZOMBIE) return;
        TierStats stats = MultiGolem.config().tier(GolemVariant.ZOMBIE);
        try {
            if (ZombieGolemConversion.handle(level, target, stats, level.getRandom().nextDouble())) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            MultiGolem.LOG.error("IronGolemAttackMixin zombie conversion failed", t);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doHurtTarget(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("TAIL"))
    private void multigolem$variantAttackEffects(ServerLevel level, Entity target,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (target == null || !target.isAlive() || target.isRemoved()) return;

        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        if (variant == GolemVariant.IRON) return;

        try {
            dispatchVariantEffect(level, self, target, variant);
        } catch (Throwable t) {
            MultiGolem.LOG.error("IronGolemAttackMixin variant effect failed for variant {}", variant.id(), t);
        }
    }

    private static void dispatchVariantEffect(ServerLevel level, IronGolem self, Entity target, GolemVariant variant) {
        if (variant == GolemVariant.DIAMOND) {
            var stats = MultiGolem.config().tier(GolemVariant.DIAMOND);
            GolemAbilityState ability = GolemAbilityStateAttachment.get(self);
            long now = level.getGameTime();
            GolemAbilityState clamped = ability.clampDiamondCooldown(now, DiamondAbility.maxCooldownTicks(stats));
            if (clamped != ability) {
                GolemAbilityStateAttachment.set(self, clamped);
                ability = clamped;
            }
            if (!ability.diamondCooldownReady(now)) return;
            var modePredicate = TargetFilter.DiamondTargetPredicate.of(stats.diamondTargetMode());
            if (!matchesDiamondTarget(modePredicate, target)) return;
            var excludeFilter = TargetFilter.fromIgnoredList(stats.ignoredTargetTypes());
            if (excludeFilter.isExcluded(target)) return;

            var bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.setPos(target.getX(), target.getY(), target.getZ());
                level.addFreshEntity(bolt);
            }
            long min = Math.max(0, stats.diamondCooldownMinSeconds()) * 20L;
            long max = Math.max(min, stats.diamondCooldownMaxSeconds()) * 20L;
            long span = Math.max(1, max - min + 1);
            long nextAt = now + min + Math.floorMod(level.getRandom().nextLong(), span);
            GolemAbilityStateAttachment.set(self, ability.withDiamondCooldown(nextAt));
            return;
        }

        if (variant == GolemVariant.NETHERITE) {
            TierStats stats = MultiGolem.config().tier(GolemVariant.NETHERITE);
            if (!stats.netheriteFireImmune()) return;
            if (target instanceof IronGolem otherGolem
                    && GolemVariantAttachment.get(otherGolem) == GolemVariant.NETHERITE) {
                return;
            }
            int seconds = netheriteIgniteSeconds(stats, self);
            if (seconds > 0) {
                target.igniteForSeconds(seconds);
            }
        }

        if (variant == GolemVariant.ZOMBIE && target instanceof Player player) {
            for (var effect : ZombieGolemEffects.effects(MultiGolem.config().tier(GolemVariant.ZOMBIE))) {
                player.addEffect(effect);
            }
        }
    }

    private static int netheriteIgniteSeconds(TierStats stats, IronGolem self) {
        return GolemCombatRules.netheriteIgniteSeconds(stats, GolemSpawnOriginAttachment.get(self));
    }

    private static boolean matchesDiamondTarget(TargetFilter.DiamondTargetPredicate predicate, Entity target) {
        if (target instanceof IronGolem golem) {
            return predicate.matchesGolemVariant(GolemVariantAttachment.get(golem));
        }
        return predicate.matches(target);
    }
}
