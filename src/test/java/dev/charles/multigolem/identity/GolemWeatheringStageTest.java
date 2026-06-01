package dev.charles.multigolem.identity;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import dev.charles.multigolem.test.MinecraftBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertTrue(GolemWeatheringStage.EXPOSED.isAfter(GolemWeatheringStage.UNAFFECTED));
        assertTrue(GolemWeatheringStage.WEATHERED.isAfter(GolemWeatheringStage.EXPOSED));
        assertTrue(GolemWeatheringStage.OXIDIZED.isAfter(GolemWeatheringStage.WEATHERED));
        assertFalse(GolemWeatheringStage.WEATHERED.isAfter(GolemWeatheringStage.OXIDIZED));
        assertFalse(GolemWeatheringStage.OXIDIZED.isAfter(GolemWeatheringStage.OXIDIZED));
        assertFalse(GolemWeatheringStage.UNAFFECTED.isAfter(GolemWeatheringStage.OXIDIZED));
    }

    @Test
    void codecRoundTripsKnownStagesAndRejectsUnknownIds() {
        for (GolemWeatheringStage stage : GolemWeatheringStage.values()) {
            assertEquals(stage, GolemWeatheringStage.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(stage.id()))
                .getOrThrow());
        }
        assertTrue(GolemWeatheringStage.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("patinated")).isError());
    }

    @Test
    void streamCodecRoundTripsKnownStage() {
        ByteBuf buffer = Unpooled.buffer();

        GolemWeatheringStage.STREAM_CODEC.encode(buffer, GolemWeatheringStage.WEATHERED);

        assertEquals(GolemWeatheringStage.WEATHERED, GolemWeatheringStage.STREAM_CODEC.decode(buffer));
    }

    @Test
    void streamCodecRejectsUnknownStageInsteadOfDefaulting() {
        ByteBuf buffer = Unpooled.buffer();
        ByteBufCodecs.STRING_UTF8.encode(buffer, "patinated");

        assertThrows(DecoderException.class, () -> GolemWeatheringStage.STREAM_CODEC.decode(buffer));
    }
}
