package dev.charles.multigolem.mixin;

import dev.charles.multigolem.spawn.VillageGolemSpawnHandler;
import dev.charles.multigolem.spawn.ZombieVillageSpawnHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Villager.class)
public abstract class VillagerMixin {

    @Inject(method = "spawnGolemIfNeeded", at = @At("HEAD"))
    private void multigolem$maintainZombieVillageGolems(ServerLevel level, long timestamp,
                                                        int villagersNeededToAgree, CallbackInfo ci) {
        ZombieVillageSpawnHandler.maintain(level, ((Villager) (Object) this).blockPosition());
    }

    @Redirect(
        method = "spawnGolemIfNeeded",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;Z)Ljava/util/Optional;"
        )
    )
    private Optional<IronGolem> multigolem$captureVillageGolem(
        EntityType<IronGolem> entityType,
        EntitySpawnReason spawnReason,
        ServerLevel level,
        BlockPos start,
        int spawnAttempts,
        int spawnRangeXZ,
        int spawnRangeY,
        SpawnUtil.Strategy strategy,
        boolean checkCollisions
    ) {
        Optional<IronGolem> spawned = SpawnUtil.trySpawnMob(
            entityType,
            spawnReason,
            level,
            start,
            spawnAttempts,
            spawnRangeXZ,
            spawnRangeY,
            strategy,
            checkCollisions
        );
        spawned.ifPresent(VillageGolemSpawnHandler::applyVillageRoll);
        return spawned;
    }
}
