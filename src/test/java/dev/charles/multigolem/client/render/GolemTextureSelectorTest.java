package dev.charles.multigolem.client.render;

import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.MultiGolem;
import dev.charles.multigolem.identity.GolemIdentity;
import dev.charles.multigolem.identity.GolemSurfaceState;
import dev.charles.multigolem.identity.GolemWeatheringStage;
import dev.charles.multigolem.test.MinecraftBootstrap;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemTextureSelectorTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void copperSurfaceTexturesUseFamilyFolderAndWeatheringSuffixes() {
        assertEquals(id("textures/entity/iron_golem/copper_golem.png"),
            GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.COPPER)));
        assertEquals(id("textures/entity/iron_golem/copper_golem_exposed.png"),
            GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.COPPER,
                new GolemSurfaceState(GolemWeatheringStage.EXPOSED, false))));
        assertEquals(id("textures/entity/iron_golem/copper_golem_waxed_oxidized.png"),
            GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.COPPER,
                new GolemSurfaceState(GolemWeatheringStage.OXIDIZED, true))));
    }

    @Test
    void nonCopperVariantsKeepExistingFlatTexturePaths() {
        assertEquals(id("textures/entity/diamond_golem.png"),
            GolemTextureSelector.get(GolemIdentity.ofIronVariant(GolemVariant.DIAMOND)));
    }

    @Test
    void invalidOrMissingCopperSurfaceFallsBackToCurrentCopperTexture() {
        assertEquals(id("textures/entity/iron_golem/copper_golem.png"),
            GolemTextureSelector.copperFallbackForTest());
    }

    @Test
    void selectedCopperSurfaceTextureAssetsExist() {
        for (String fileName : List.of(
            "copper_golem.png",
            "copper_golem_exposed.png",
            "copper_golem_weathered.png",
            "copper_golem_oxidized.png",
            "copper_golem_waxed.png",
            "copper_golem_waxed_exposed.png",
            "copper_golem_waxed_weathered.png",
            "copper_golem_waxed_oxidized.png"
        )) {
            assertTrue(Files.exists(Path.of(
                "src/main/resources/assets/multigolem/textures/entity/iron_golem", fileName)), fileName);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MultiGolem.MOD_ID, path);
    }
}
