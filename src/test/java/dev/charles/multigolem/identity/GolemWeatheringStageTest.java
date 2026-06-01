package dev.charles.multigolem.identity;

import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemWeatheringStageTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void weatheringStagesHaveStableIdsAndOrder() {
        assertEquals("unaffected", GolemWeatheringStage.UNAFFECTED.id());
        assertEquals("exposed", GolemWeatheringStage.EXPOSED.id());
        assertEquals("weathered", GolemWeatheringStage.WEATHERED.id());
        assertEquals("oxidized", GolemWeatheringStage.OXIDIZED.id());
        assertTrue(GolemWeatheringStage.OXIDIZED.isAfter(GolemWeatheringStage.WEATHERED));
    }
}
