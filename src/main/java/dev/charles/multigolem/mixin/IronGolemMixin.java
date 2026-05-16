package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(IronGolem.class)
public abstract class IronGolemMixin {

    private static final Identifier HEALTH_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant_health");
    private static final Identifier DAMAGE_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant_attack_damage");

    // Vanilla iron golem base values from IronGolem#createAttributes (see docs/26.1.2-mojang-targets.md)
    private static final double IRON_BASE_HEALTH = 100.0;
    private static final double IRON_BASE_ATTACK = 15.0;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void multigolem$ensureVariantModifiers(CallbackInfo ci) {
        IronGolem self = (IronGolem) (Object) this;
        GolemVariant variant = GolemVariantAttachment.get(self);
        TierStats stats = MultiGolem.config().tier(variant);

        applyDelta(self.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID, stats.maxHealth() - IRON_BASE_HEALTH);
        applyDelta(self.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID, stats.attackDamage() - IRON_BASE_ATTACK);
    }

    private static void applyDelta(AttributeInstance instance, Identifier id, double delta) {
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == delta) return;
        instance.removeModifier(id);
        if (delta != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    @Inject(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private void multigolem$healWithMatchingIngot(Player player, InteractionHand hand,
                                                   CallbackInfoReturnable<InteractionResult> cir) {
        if (!MultiGolem.config().allowGolemHealing()) {
            // Healing disabled globally — cancel ALL heal interactions (including vanilla iron→iron)
            // so the iron-ingot heal doesn't sneak through.
            ItemStack stack = player.getItemInHand(hand);
            if (GolemVariant.fromIngot(stack.getItem()).isPresent()) {
                cir.setReturnValue(InteractionResult.PASS);
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
