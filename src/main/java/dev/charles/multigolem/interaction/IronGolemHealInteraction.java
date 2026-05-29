package dev.charles.multigolem.interaction;

import dev.charles.multigolem.GolemVariant;
import net.minecraft.world.InteractionResult;

public final class IronGolemHealInteraction {
    private IronGolemHealInteraction() {}

    public static InteractionResult result(
        boolean healingAllowed,
        GolemVariant targetVariant,
        GolemVariant heldVariant,
        boolean fullHealth,
        boolean permissionAllowed
    ) {
        if (heldVariant == null) return InteractionResult.PASS;
        if (!healingAllowed) return InteractionResult.FAIL;
        if (targetVariant == GolemVariant.IRON) return InteractionResult.PASS;
        if (heldVariant != targetVariant) return InteractionResult.FAIL;
        if (fullHealth) return InteractionResult.PASS;
        return permissionAllowed ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    public static boolean shouldRunCustomHeal(InteractionResult result) {
        return result == InteractionResult.SUCCESS;
    }
}
