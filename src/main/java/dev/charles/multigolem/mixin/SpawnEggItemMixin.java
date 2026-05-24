package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.permissions.MultiGolemPermissions;
import dev.charles.multigolem.spawn.SpawnEggStacks;
import dev.charles.multigolem.spawn.SpawnEggVariantSpawner;
import dev.charles.multigolem.spawn.SpawnerVariantMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(SpawnEggItem.class)
public abstract class SpawnEggItemMixin {
    @Inject(
        method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Spawner;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V"
        ),
        cancellable = true
    )
    private void multigolem$denyMarkedSpawnerUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Optional<GolemVariant> variant = SpawnEggStacks.variantFrom(context.getItemInHand());
        if (variant.isEmpty()) {
            return;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }

        if (!MultiGolemPermissions.canCreate(player, variant.get())) {
            MultiGolemPermissions.sendCreateDenied(player, variant.get());
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Spawner;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V",
            shift = At.Shift.AFTER
        )
    )
    private void multigolem$markSpawnerSpawnData(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SpawnerBlockEntity blockEntity)) {
            return;
        }

        BaseSpawner spawner = blockEntity.getSpawner();
        SpawnData data = ((BaseSpawnerAccessor) spawner).multigolem$getNextSpawnData();
        if (data == null) {
            return;
        }

        Optional<GolemVariant> variant = SpawnEggStacks.variantFrom(context.getItemInHand());
        if (variant.isPresent()) {
            // Vanilla setEntityId and this marker write run synchronously inside the same server useOn call.
            SpawnerVariantMarker.write(data.getEntityToSpawn(), variant.get());
        } else if (SpawnEggItem.getType(context.getItemInHand()) == EntityType.IRON_GOLEM) {
            SpawnerVariantMarker.clear(data.getEntityToSpawn());
        }
    }

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
