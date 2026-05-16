package dev.charles.multigolem.attachment;

import dev.charles.multigolem.MultiGolem;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

public final class GolemAbilityStateAttachment {

    public static final AttachmentType<GolemAbilityState> TYPE = AttachmentRegistry
        .<GolemAbilityState>builder()
        .persistent(GolemAbilityState.CODEC)
        .initializer(GolemAbilityState::fresh)
        .buildAndRegister(Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, "ability_state"));

    private GolemAbilityStateAttachment() {}

    public static GolemAbilityState get(Entity entity) {
        GolemAbilityState s = entity.getAttached(TYPE);
        return s != null ? s : GolemAbilityState.fresh();
    }

    public static void set(Entity entity, GolemAbilityState state) {
        entity.setAttached(TYPE, state);
    }

    public static void touch() {
        // Force class load + TYPE registration when called from MultiGolem.onInitialize.
    }
}
