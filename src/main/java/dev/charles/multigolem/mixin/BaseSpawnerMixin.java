package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.spawn.SpawnerVariantMarker;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
    private static final ThreadLocal<Optional<GolemVariant>> MULTIGOLEM_SPAWNER_VARIANT =
        ThreadLocal.withInitial(Optional::empty);

    @Redirect(
        method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/TagValueInput;create(Lnet/minecraft/util/ProblemReporter;Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/storage/ValueInput;"
        )
    )
    private ValueInput multigolem$stashVariantForSpawnerEntity(ProblemReporter reporter, HolderLookup.Provider registries, CompoundTag entityTag) {
        MULTIGOLEM_SPAWNER_VARIANT.set(SpawnerVariantMarker.read(entityTag));
        return TagValueInput.create(reporter, registries, entityTag);
    }

    @Redirect(
        method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean multigolem$applyVariantBeforeSpawnerAdd(ServerLevel level, Entity entity) {
        try {
            Optional<GolemVariant> variant = MULTIGOLEM_SPAWNER_VARIANT.get();
            if (variant.isPresent() && entity instanceof IronGolem golem) {
                GolemVariantAttachment.set(golem, variant.get());
                VariantAttributes.apply(golem);
            }
            return level.tryAddFreshEntityWithPassengers(entity);
        } finally {
            MULTIGOLEM_SPAWNER_VARIANT.remove();
        }
    }
}
