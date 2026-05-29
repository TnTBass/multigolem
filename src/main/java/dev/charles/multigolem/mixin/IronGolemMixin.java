package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.ability.GolemCombatRules;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.interaction.IronGolemHealInteraction;
import dev.charles.multigolem.permissions.MultiGolemPermissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(IronGolem.class)
public abstract class IronGolemMixin {

    @Inject(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$healWithMatchingIngot(Player player, InteractionHand hand,
                                                   CallbackInfoReturnable<InteractionResult> cir) {
        IronGolem self = (IronGolem) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return;

        GolemVariant variant = GolemVariantAttachment.get(self);
        Optional<GolemVariant> held = GolemVariant.fromIngot(stack.getItem());
        GolemVariant heldVariant = held.orElse(null);
        boolean fullHealth = self.getHealth() >= self.getMaxHealth();
        if (!MultiGolem.config().allowGolemHealing()) {
            // Healing disabled globally — cancel ALL heal interactions (including vanilla iron→iron)
            // so the iron-ingot heal doesn't sneak through.
            InteractionResult disabled = IronGolemHealInteraction.result(
                false, variant, heldVariant, fullHealth, true);
            if (disabled != InteractionResult.PASS) cir.setReturnValue(disabled);
            return;
        }

        boolean permissionRequired = variant != GolemVariant.IRON
            && heldVariant == variant
            && !fullHealth;
        if (self.level().isClientSide()) {
            InteractionResult clientResult = IronGolemHealInteraction.result(
                true, variant, heldVariant, fullHealth, true);
            if (clientResult != InteractionResult.PASS) cir.setReturnValue(clientResult);
            return;
        }

        boolean permissionAllowed = !permissionRequired || MultiGolemPermissions.canHeal(player, variant);
        InteractionResult result = IronGolemHealInteraction.result(
            true, variant, heldVariant, fullHealth, permissionAllowed);
        if (result == InteractionResult.PASS) {
            return;
        }

        if (!permissionAllowed) {
            MultiGolemPermissions.sendHealDenied(player, variant);
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (!IronGolemHealInteraction.shouldRunCustomHeal(result)) {
            cir.setReturnValue(result);
            return;
        }

        float before = self.getHealth();
        self.heal(GolemCombatRules.healAmount(variant, MultiGolem.config().tier(variant)));
        if (self.getHealth() == before) {
            // already full HP — do nothing (mimics vanilla PASS behavior)
            return;
        }
        float pitch = 1.0F + (self.getRandom().nextFloat() - self.getRandom().nextFloat()) * 0.2F;
        self.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
        stack.consume(1, player);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
