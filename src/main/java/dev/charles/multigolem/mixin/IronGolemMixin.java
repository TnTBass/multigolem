package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
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
        if (!MultiGolem.config().allowGolemHealing()) {
            // Healing disabled globally — cancel ALL heal interactions (including vanilla iron→iron)
            // so the iron-ingot heal doesn't sneak through.
            ItemStack stack = player.getItemInHand(hand);
            if (GolemVariant.fromIngot(stack.getItem()).isPresent()) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
            return;
        }

        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        if (variant == GolemVariant.IRON) return; // vanilla handles iron→iron heal

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) return;
        Optional<GolemVariant> held = GolemVariant.fromIngot(stack.getItem());
        if (held.isEmpty() || held.get() != variant) return;

        float before = self.getHealth();
        self.heal(25.0F);
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
