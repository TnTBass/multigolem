package dev.charles.multigolem.mixin;

import dev.charles.multigolem.spawn.SpawnEggVariantSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpawnEggItem.class)
public abstract class SpawnEggItemMixin {
    @Redirect(
        method = "spawnMob(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZZ)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"
        )
    )
    private static Entity multigolem$spawnMarkedVariantOrVanilla(
        EntityType<?> type,
        ServerLevel level,
        ItemStack stack,
        LivingEntity user,
        BlockPos pos,
        EntitySpawnReason reason,
        boolean alignPosition,
        boolean invertY
    ) {
        return SpawnEggVariantSpawner.spawnMarkedOrVanilla(type, level, stack, user, pos, reason, alignPosition, invertY);
    }
}
