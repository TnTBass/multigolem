package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemIdentityAttachment;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.spawn.SpawnerVariantMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
    private static final ThreadLocal<Optional<GolemIdentity>> MULTIGOLEM_SPAWNER_IDENTITY =
        ThreadLocal.withInitial(Optional::empty);

    @Redirect(
        method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/TagValueInput;create(Lnet/minecraft/util/ProblemReporter;Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/storage/ValueInput;"
        ),
        require = 1
    )
    private ValueInput multigolem$stashVariantForSpawnerEntity(ProblemReporter reporter, HolderLookup.Provider registries, CompoundTag entityTag) {
        MULTIGOLEM_SPAWNER_IDENTITY.set(SpawnerVariantMarker.readIdentity(entityTag));
        return TagValueInput.create(reporter, registries, entityTag);
    }

    @Redirect(
        method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"
        ),
        require = 1
    )
    private boolean multigolem$applyVariantBeforeSpawnerAdd(ServerLevel level, Entity entity) {
        try {
            Optional<GolemIdentity> identity = MULTIGOLEM_SPAWNER_IDENTITY.get();
            if (identity.isPresent() && entity instanceof IronGolem golem) {
                GolemIdentityAttachment.set(golem, identity.get());
                VariantAttributes.fillFreshSpawnHealth(golem);
            }
            return level.tryAddFreshEntityWithPassengers(entity);
        } finally {
            MULTIGOLEM_SPAWNER_IDENTITY.remove();
        }
    }

    @Inject(
        method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
        at = @At("RETURN")
    )
    private void multigolem$clearSpawnerVariantOnExit(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        MULTIGOLEM_SPAWNER_IDENTITY.remove();
    }

    @Redirect(
        method = "getOrCreateDisplayEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/entity/Entity;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;loadEntityRecursive(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/EntityProcessor;)Lnet/minecraft/world/entity/Entity;"
        ),
        require = 1
    )
    private Entity multigolem$applyPreviewIdentity(
        CompoundTag entityTag,
        Level level,
        EntitySpawnReason reason,
        EntityProcessor processor
    ) {
        Entity displayEntity = EntityType.loadEntityRecursive(entityTag, level, reason, processor);
        if (level.isClientSide() && displayEntity instanceof IronGolem golem) {
            SpawnerVariantMarker.previewIdentity(entityTag)
                .ifPresent(identity -> GolemIdentityAttachment.set(golem, identity));
        }
        return displayEntity;
    }
}
