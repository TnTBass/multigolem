package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.DiamondAbility;
import dev.charles.multigolem.ability.TargetFilter;
import dev.charles.multigolem.attachment.GolemAbilityState;
import dev.charles.multigolem.attachment.GolemAbilityStateAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
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
            if (!modePredicate.matches(target)) return;
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
            if (!MultiGolem.config().tier(GolemVariant.NETHERITE).netheriteFireImmune()) return;
            if (target instanceof IronGolem otherGolem
                    && GolemVariantAttachment.get(otherGolem) == GolemVariant.NETHERITE
                    && MultiGolem.config().tier(GolemVariant.NETHERITE).netheriteFireImmune()) {
                return;
            }
            int seconds = MultiGolem.config().tier(GolemVariant.NETHERITE).netheriteIgniteSeconds();
            if (seconds > 0) {
                target.igniteForSeconds(seconds);
            }
        }
    }
}
