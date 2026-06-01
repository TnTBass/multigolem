package dev.charles.multigolem.identity;

import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemSurfaceStateTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void surfaceStateDefaultIsUnwaxedUnaffected() {
        assertEquals(new GolemSurfaceState(GolemWeatheringStage.UNAFFECTED, false), GolemSurfaceState.DEFAULT);
        assertTrue(GolemSurfaceState.DEFAULT.isDefault());
        assertFalse(new GolemSurfaceState(GolemWeatheringStage.UNAFFECTED, true).isDefault());
        assertFalse(new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, false).isDefault());
    }

    @Test
    void surfaceStateRequiresWeatheringStage() {
        assertThrows(NullPointerException.class, () -> new GolemSurfaceState(null, false));
    }
}
