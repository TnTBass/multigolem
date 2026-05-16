package dev.charles.multigolem.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record GolemAbilityState(long nextDiamondAbilityGameTime) {

    public static final Codec<GolemAbilityState> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.LONG.optionalFieldOf("next_diamond_ability_game_time", 0L)
            .forGetter(GolemAbilityState::nextDiamondAbilityGameTime)
    ).apply(i, GolemAbilityState::new));

    public static GolemAbilityState fresh() {
        return new GolemAbilityState(0L);
    }

    public GolemAbilityState withDiamondCooldown(long nextGameTime) {
        return new GolemAbilityState(nextGameTime);
    }

    public boolean diamondCooldownReady(long currentGameTime) {
        return currentGameTime >= nextDiamondAbilityGameTime;
    }
}
