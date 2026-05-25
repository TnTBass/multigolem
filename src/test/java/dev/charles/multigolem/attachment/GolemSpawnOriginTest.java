package dev.charles.multigolem.attachment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemSpawnOriginTest {

    @Test
    void parsesKnownOriginIds() {
        assertEquals(GolemSpawnOrigin.UNKNOWN, GolemSpawnOrigin.fromId("unknown").orElseThrow());
        assertEquals(GolemSpawnOrigin.VILLAGE, GolemSpawnOrigin.fromId("village").orElseThrow());
    }

    @Test
    void rejectsUnknownOriginIds() {
        assertTrue(GolemSpawnOrigin.fromId("").isEmpty());
        assertTrue(GolemSpawnOrigin.fromId("spawn_egg").isEmpty());
    }
}
