package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnerVariantMarkerTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void writesMarkerIntoSpawnerEntityTagWithoutChangingEntityId() {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:iron_golem");

        SpawnerVariantMarker.write(entity, GolemVariant.DIAMOND);

        assertEquals("minecraft:iron_golem", entity.getStringOr("id", ""));
        assertEquals(GolemVariant.DIAMOND, SpawnerVariantMarker.read(entity).orElseThrow());
    }

    @Test
    void unmarkedSpawnerEntityTagReadsEmpty() {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:iron_golem");

        assertTrue(SpawnerVariantMarker.read(entity).isEmpty());
    }

    @Test
    void clearRemovesOnlyMultiGolemMarker() {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:iron_golem");
        entity.putString("CustomName", "\"Bob\"");

        SpawnerVariantMarker.write(entity, GolemVariant.GOLD);
        SpawnerVariantMarker.clear(entity);

        assertEquals("\"Bob\"", entity.getStringOr("CustomName", ""));
        assertTrue(SpawnerVariantMarker.read(entity).isEmpty());
    }
}
