package dev.charles.multigolem.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GolemAbilityState(
    long nextDiamondAbilityGameTime,
    long nextDiamondScanGameTime,
    long redstoneOverchargeActiveUntilGameTime,
    long redstoneOverchargeCooldownUntilGameTime,
    boolean redstoneWasBelowThreshold
) {

    public static final Codec<GolemAbilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.optionalFieldOf("next_diamond_ability_game_time", 0L)
            .forGetter(GolemAbilityState::nextDiamondAbilityGameTime),
        Codec.LONG.optionalFieldOf("next_diamond_scan_game_time", 0L)
            .forGetter(GolemAbilityState::nextDiamondScanGameTime),
        Codec.LONG.optionalFieldOf("redstone_overcharge_active_until_game_time", 0L)
            .forGetter(GolemAbilityState::redstoneOverchargeActiveUntilGameTime),
        Codec.LONG.optionalFieldOf("redstone_overcharge_cooldown_until_game_time", 0L)
            .forGetter(GolemAbilityState::redstoneOverchargeCooldownUntilGameTime),
        Codec.BOOL.optionalFieldOf("redstone_was_below_threshold", false)
            .forGetter(GolemAbilityState::redstoneWasBelowThreshold)
    ).apply(i, GolemAbilityState::new));

    public GolemAbilityState(long nextDiamondAbilityGameTime, long nextDiamondScanGameTime) {
        this(nextDiamondAbilityGameTime, nextDiamondScanGameTime, 0L, 0L, false);
    }

    public static GolemAbilityState fresh() {
        return new GolemAbilityState(0L, 0L, 0L, 0L, false);
    }

    public GolemAbilityState withDiamondCooldown(long nextGameTime) {
        return new GolemAbilityState(nextGameTime, nextDiamondScanGameTime,
            redstoneOverchargeActiveUntilGameTime, redstoneOverchargeCooldownUntilGameTime, redstoneWasBelowThreshold);
    }

    public GolemAbilityState withDiamondScanBackoff(long nextScan) {
        return new GolemAbilityState(nextDiamondAbilityGameTime, nextScan,
            redstoneOverchargeActiveUntilGameTime, redstoneOverchargeCooldownUntilGameTime, redstoneWasBelowThreshold);
    }

    public GolemAbilityState clampDiamondCooldown(long currentGameTime, long maxCooldownTicks) {
        long maxNextGameTime = currentGameTime + Math.max(0L, maxCooldownTicks);
        if (nextDiamondAbilityGameTime <= maxNextGameTime) {
            return this;
        }
        return withDiamondCooldown(maxNextGameTime);
    }

    public boolean diamondCooldownReady(long currentGameTime) {
        return currentGameTime >= nextDiamondAbilityGameTime;
    }

    public boolean diamondScanReady(long currentGameTime) {
        return currentGameTime >= nextDiamondScanGameTime;
    }

    public boolean redstoneOverchargeActive(long currentGameTime) {
        return currentGameTime < redstoneOverchargeActiveUntilGameTime;
    }

    public boolean redstoneCooldownReady(long currentGameTime) {
        return currentGameTime >= redstoneOverchargeCooldownUntilGameTime;
    }

    public GolemAbilityState withRedstoneOvercharge(long untilGameTime) {
        return new GolemAbilityState(nextDiamondAbilityGameTime, nextDiamondScanGameTime,
            untilGameTime, redstoneOverchargeCooldownUntilGameTime, redstoneWasBelowThreshold);
    }

    public GolemAbilityState withRedstoneCooldown(long untilGameTime) {
        return new GolemAbilityState(nextDiamondAbilityGameTime, nextDiamondScanGameTime,
            redstoneOverchargeActiveUntilGameTime, untilGameTime, redstoneWasBelowThreshold);
    }

    public GolemAbilityState withRedstoneWasBelowThreshold(boolean below) {
        return new GolemAbilityState(nextDiamondAbilityGameTime, nextDiamondScanGameTime,
            redstoneOverchargeActiveUntilGameTime, redstoneOverchargeCooldownUntilGameTime, below);
    }
}
