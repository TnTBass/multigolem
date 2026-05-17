package dev.charles.multigolem.attribute;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.config.TierStats;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;

public final class VariantAttributes {

    private static final Identifier HEALTH_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant_health");
    private static final Identifier DAMAGE_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant_attack_damage");
    private static final Identifier SPEED_MODIFIER_ID =
        Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "variant_speed");

    // Vanilla iron golem base values from IronGolem#createAttributes (see docs/26.1.2-mojang-targets.md)
    private static final double IRON_BASE_HEALTH = 100.0;
    private static final double IRON_BASE_ATTACK = 15.0;

    private VariantAttributes() {}

    public static void apply(IronGolem golem) {
        GolemVariant variant = GolemVariantAttachment.get(golem);
        TierStats stats = MultiGolem.config().tier(variant);
        applyDelta(golem.getAttribute(Attributes.MAX_HEALTH), HEALTH_MODIFIER_ID, stats.maxHealth() - IRON_BASE_HEALTH);
        applyDelta(golem.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_MODIFIER_ID, stats.attackDamage() - IRON_BASE_ATTACK);

        // Gold speed modifier: ADD_MULTIPLIED_TOTAL so it stacks cleanly with other sources
        double speedDelta = (variant == GolemVariant.GOLD)
            ? (MultiGolem.config().tier(GolemVariant.GOLD).goldSpeedMultiplier() - 1.0)
            : 0.0;
        applyMultipliedTotal(golem.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_MODIFIER_ID, speedDelta);
    }

    private static void applyDelta(AttributeInstance instance, Identifier id, double delta) {
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == delta) return;
        instance.removeModifier(id);
        if (delta != 0.0) {
            instance.addPermanentModifier(new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void applyMultipliedTotal(AttributeInstance instance, Identifier id, double delta) {
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == delta) return;
        instance.removeModifier(id);
        if (delta != 0.0) {
            instance.addPermanentModifier(new AttributeModifier(id, delta, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
