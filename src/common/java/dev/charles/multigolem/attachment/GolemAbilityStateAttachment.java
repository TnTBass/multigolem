package dev.charles.multigolem.attachment;

import net.minecraft.world.entity.Entity;

public final class GolemAbilityStateAttachment {
    private GolemAbilityStateAttachment() {}

    public static GolemAbilityState get(Entity entity) {
        return GolemStorage.adapter().abilityState(entity);
    }

    public static void set(Entity entity, GolemAbilityState state) {
        GolemStorage.adapter().setAbilityState(entity, state);
    }
}
