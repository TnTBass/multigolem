package dev.charles.multigolem.spawn;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.identity.GolemIdentity;
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
        assertEquals("iron_golem", entity.getCompoundOrEmpty("multigolem").getStringOr("family", ""));
        assertEquals(GolemVariant.DIAMOND, SpawnerVariantMarker.read(entity).orElseThrow());
        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND), SpawnerVariantMarker.readIdentity(entity).orElseThrow());
    }

    @Test
    void zombieSpawnerMarkerRoundTrips() {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:iron_golem");

        SpawnerVariantMarker.write(entity, GolemVariant.ZOMBIE);

        assertEquals("minecraft:iron_golem", entity.getStringOr("id", ""));
        assertEquals(GolemVariant.ZOMBIE, SpawnerVariantMarker.read(entity).orElseThrow());
        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE), SpawnerVariantMarker.readIdentity(entity).orElseThrow());
    }

    @Test
    void unmarkedSpawnerEntityTagReadsEmpty() {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:iron_golem");

        assertTrue(SpawnerVariantMarker.read(entity).isEmpty());
        assertTrue(SpawnerVariantMarker.readIdentity(entity).isEmpty());
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

    @Test
    void familyVariantMarkerReadsAsIdentity() {
        CompoundTag entity = new CompoundTag();
        SpawnerVariantMarker.writeIdentity(entity, GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE));

        assertEquals("iron_golem", entity.getCompoundOrEmpty("multigolem").getStringOr("family", ""));
        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.ZOMBIE), SpawnerVariantMarker.readIdentity(entity).orElseThrow());
    }

    @Test
    void markerIdentityCanBeReadForPreviewApplication() {
        CompoundTag entity = new CompoundTag();
        entity.putString("id", "minecraft:iron_golem");
        SpawnerVariantMarker.write(entity, GolemVariant.DIAMOND);

        assertEquals(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND),
            SpawnerVariantMarker.previewIdentity(entity).orElseThrow());
    }

    @Test
    void ironDefaultOrInvalidMarkersReadEmpty() {
        assertTrue(marked("iron", null).isEmpty());
        assertTrue(marked("diamond", "copper_golem").isEmpty());
        assertTrue(marked("obsidian", "iron_golem").isEmpty());
    }

    private static java.util.Optional<GolemIdentity> marked(String variant, String family) {
        CompoundTag entity = new CompoundTag();
        CompoundTag multigolem = new CompoundTag();
        if (family != null) {
            multigolem.putString("family", family);
        }
        multigolem.putString("variant", variant);
        entity.put("multigolem", multigolem);
        return SpawnerVariantMarker.readIdentity(entity);
    }
}
