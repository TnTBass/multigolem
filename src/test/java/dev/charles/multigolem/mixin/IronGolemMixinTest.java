package dev.charles.multigolem.mixin;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.ability.GolemCombatRules;
import dev.charles.multigolem.config.TierStats;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.interaction.IronGolemHealInteraction;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.world.InteractionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronGolemMixinTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void customGolemRejectsKnownWrongHealItemInsteadOfPassingToVanilla() {
        assertEquals(InteractionResult.FAIL,
            IronGolemHealInteraction.result(
                true, GolemVariant.DIAMOND, GolemVariant.IRON, false, true));
    }

    @Test
    void wrongHealItemResultDoesNotRunServerHeal() {
        assertFalse(IronGolemHealInteraction.shouldRunCustomHeal(InteractionResult.FAIL));
    }

    @Test
    void successResultRunsServerHeal() {
        assertTrue(IronGolemHealInteraction.shouldRunCustomHeal(InteractionResult.SUCCESS));
    }

    @Test
    void disabledHealingBlocksVanillaIronRepair() {
        assertEquals(InteractionResult.FAIL,
            IronGolemHealInteraction.result(
                false, GolemVariant.IRON, GolemVariant.IRON, false, true));
    }

    @Test
    void customGolemAcceptsMatchingHealItemForClientPrediction() {
        GolemIdentity oxidizedCopper = GolemIdentity.ofIronVariant(GolemVariant.COPPER,
            new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true));

        assertEquals(InteractionResult.SUCCESS,
            IronGolemHealInteraction.result(
                true, oxidizedCopper.variant(), GolemVariant.COPPER, false, true));
    }

    @Test
    void permissionDeniedCustomHealFails() {
        assertEquals(InteractionResult.FAIL,
            IronGolemHealInteraction.result(
                true, GolemVariant.DIAMOND, GolemVariant.DIAMOND, false, false));
    }

    @Test
    void fullHealthCustomGolemConsumesNoItemAndDoesNotPretendHeal() {
        assertEquals(InteractionResult.PASS,
            IronGolemHealInteraction.result(
                true, GolemVariant.DIAMOND, GolemVariant.DIAMOND, true, true));
    }

    @Test
    void vanillaIronGolemAlwaysPassesToVanillaRepairLogic() {
        assertEquals(InteractionResult.PASS,
            IronGolemHealInteraction.result(
                true, GolemVariant.IRON, GolemVariant.DIAMOND, false, true));
    }

    @Test
    void healAmountUsesZombieRottenFleshConfig() {
        TierStats zombie = new TierStats(100, 15.0, true, List.of(),
            null, null,
            null, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null,
            40.0, true, 12, 0,
            true, 4, 0,
            true, 4, 0,
            true, 1.0, true, 1.0);

        assertEquals(40.0F, GolemCombatRules.healAmount(GolemVariant.ZOMBIE, zombie), 0.0001F);
    }

    @Test
    void healAmountDefaultsOtherVariantsToTwentyFive() {
        TierStats gold = new TierStats(130, 22.5, true, List.of("CREEPERS"),
            null, null,
            1.75, true, true,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null);

        assertEquals(25.0F, GolemCombatRules.healAmount(GolemVariant.GOLD, gold), 0.0001F);
    }
}
