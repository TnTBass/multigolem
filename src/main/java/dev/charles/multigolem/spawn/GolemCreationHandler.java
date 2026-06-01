package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.attachment.GolemIdentityAttachment;
import dev.charles.multigolem.attachment.GolemVariantAttachment;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.permissions.MultiGolemPermissions;
import dev.charles.multigolem.permissions.PumpkinPlacementTracker;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class GolemCreationHandler {

    private static final Map<GolemVariant, BlockPattern> PATTERNS = new EnumMap<>(GolemVariant.class);

    private GolemCreationHandler() {}

    private static BlockPattern patternFor(GolemVariant variant) {
        return PATTERNS.computeIfAbsent(variant, v -> BlockPatternBuilder.start()
            .aisle("~^~", "###", "~#~")
            .where('^', BlockInWorld.hasState(s -> s.is(Blocks.CARVED_PUMPKIN) || s.is(Blocks.JACK_O_LANTERN)))
            .where('#', BlockInWorld.hasState(v::matchesBodyBlock))
            .where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
            .build());
    }

    /**
     * Returns true if a variant golem was spawned (caller should cancel vanilla logic).
     * Returns false if no MultiGolem T-pattern matched (caller should fall through to vanilla).
     */
    public static boolean trySpawnVariant(Level level, BlockPos topPos) {
        if (!(level instanceof ServerLevel server)) return false;

        for (GolemVariant variant : GolemVariant.multiGolemPlayerBuildableVariants()) {
            BlockPattern.BlockPatternMatch match = patternFor(variant).find(server, topPos);
            if (match == null) continue;

            // No player context means non-player or unsupported placement; ungated by design.
            Optional<ServerPlayer> responsiblePlayer = PumpkinPlacementTracker.currentServerPlayerFor(topPos);
            if (responsiblePlayer.isPresent()) {
                ServerPlayer player = responsiblePlayer.get();
                if (!MultiGolemPermissions.canCreate(player, variant)) {
                    MultiGolemPermissions.sendCreateDenied(player, variant);
                    return true;
                }
            }

            IronGolem golem = EntityType.IRON_GOLEM.create(server, EntitySpawnReason.TRIGGERED);
            if (golem == null) {
                MultiGolem.LOG.warn("Failed to create iron golem entity for variant {}", variant.id());
                return false;
            }
            golem.setPlayerCreated(true);
            GolemIdentityAttachment.set(golem, identityForBodyStates(variant, match));

            // Vanilla's spawn order: clear blocks first, then position+spawn, then trigger, then updateNeighbors.
            CarvedPumpkinBlock.clearPatternBlocks(server, match);
            BlockPos spawnPos = match.getBlock(1, 2, 0).getPos();
            golem.snapTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5, 0.0F, 0.0F);
            // Set HP to variant's configured max so it spawns at full health.
            golem.setHealth(MultiGolem.config().tier(variant).maxHealth());
            server.addFreshEntity(golem);

            for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
                CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
            }
            CarvedPumpkinBlock.updatePatternBlocks(server, match);
            return true;
        }
        return false;
    }

    static GolemIdentity identityForBodyStatesForTest(GolemVariant variant, List<net.minecraft.world.level.block.state.BlockState> bodyStates) {
        if (variant != GolemVariant.COPPER) return GolemIdentity.ofIronVariant(variant);
        return CopperSurfaceResolver.resolveBody(bodyStates)
            .map(surface -> GolemIdentity.ofIronVariant(GolemVariant.COPPER, surface))
            .orElseGet(() -> GolemIdentity.ofIronVariant(GolemVariant.COPPER));
    }

    private static GolemIdentity identityForBodyStates(GolemVariant variant, BlockPattern.BlockPatternMatch match) {
        return identityForBodyStatesForTest(variant, List.of(
            match.getBlock(0, 1, 0).getState(),
            match.getBlock(1, 1, 0).getState(),
            match.getBlock(2, 1, 0).getState(),
            match.getBlock(1, 2, 0).getState()
        ));
    }
}
