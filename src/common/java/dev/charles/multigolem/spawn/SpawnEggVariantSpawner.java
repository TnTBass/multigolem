package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemIdentityAttachment;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.permissions.MultiGolemPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class SpawnEggVariantSpawner {
    private SpawnEggVariantSpawner() {}

    public static Entity spawnMarkedOrVanilla(
        EntityType<?> type,
        ServerLevel level,
        ItemStack stack,
        LivingEntity user,
        BlockPos pos,
        EntitySpawnReason reason,
        boolean alignPosition,
        boolean invertY
    ) {
        Optional<GolemIdentity> identity = SpawnEggStacks.identityFrom(stack);
        if (identity.isEmpty() || type != EntityTypes.IRON_GOLEM) {
            return type.spawn(level, stack, user, pos, reason, alignPosition, invertY);
        }

        GolemIdentity requested = identity.get();
        GolemVariant variant = requested.variant();
        if (!GolemAvailabilityGuards.canCreate(MultiGolem.config(), requested)) {
            if (user instanceof ServerPlayer player) {
                MultiGolemPermissions.sendCreateDenied(player, variant);
            }
            return null;
        }

        if (user instanceof ServerPlayer player && !MultiGolemPermissions.canCreate(player, variant)) {
            MultiGolemPermissions.sendCreateDenied(player, variant);
            return null;
        }

        Entity spawned = type.spawn(level, stack, user, pos, reason, alignPosition, invertY);
        if (spawned instanceof IronGolem golem) {
            GolemIdentityAttachment.set(golem, requested);
            VariantAttributes.fillFreshSpawnHealth(golem);
        }
        return spawned;
    }
}
