package dev.charles.multigolem.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GolemAbilityState(long nextDiamondAbilityGameTime, long nextDiamondScanGameTime) {

    public static final Codec<GolemAbilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.optionalFieldOf("next_diamond_ability_game_time", 0L)
            .forGetter(GolemAbilityState::nextDiamondAbilityGameTime),
        Codec.LONG.optionalFieldOf("next_diamond_scan_game_time", 0L)
            .forGetter(GolemAbilityState::nextDiamondScanGameTime)
    ).apply(i, GolemAbilityState::new));

    public static GolemAbilityState fresh() {
        return new GolemAbilityState(0L, 0L);
    }

    public GolemAbilityState withDiamondCooldown(long nextGameTime) {
        return new GolemAbilityState(nextGameTime, nextDiamondScanGameTime);
    }

    public GolemAbilityState withDiamondScanBackoff(long nextScan) {
        return new GolemAbilityState(nextDiamondAbilityGameTime, nextScan);
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
}
