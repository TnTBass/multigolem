package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.attribute.VariantAttributes;
import dev.charles.multigolem.permissions.MultiGolemPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
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
        Optional<GolemVariant> variant = SpawnEggStacks.variantFrom(stack);
        if (variant.isEmpty() || type != EntityType.IRON_GOLEM) {
            return type.spawn(level, stack, user, pos, reason, alignPosition, invertY);
        }

        if (user instanceof ServerPlayer player && !MultiGolemPermissions.canCreate(player, variant.get())) {
            MultiGolemPermissions.sendCreateDenied(player, variant.get());
            return null;
        }

        Entity spawned = type.spawn(level, stack, user, pos, reason, alignPosition, invertY);
        if (spawned instanceof IronGolem golem) {
            GolemVariantAttachment.set(golem, variant.get());
            VariantAttributes.apply(golem);
        }
        return spawned;
    }
}
