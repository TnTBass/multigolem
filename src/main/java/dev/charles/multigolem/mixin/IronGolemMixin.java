package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
