package dev.charles.multigolem.spawn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.charles.multigolem.GolemVariant;
import dev.charles.multigolem.catalog.GolemVariantCatalog;
import dev.charles.multigolem.catalog.GolemVariantSpec;
import dev.charles.multigolem.test.MinecraftBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnEggItemDefinitionTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    @Test
    void ironGolemSpawnEggItemDefinitionMapsEveryMarkedVariantToCustomModel() throws Exception {
        Path definition = Path.of("src/main/resources/assets/minecraft/items/iron_golem_spawn_egg.json");
        var root = JsonParser.parseString(Files.readString(definition)).getAsJsonObject();
        var cases = root.getAsJsonObject("model").getAsJsonArray("cases");

        assertEquals(GolemVariant.spawnEggVariants().size(), cases.size());
        List<String> actualWhenValues = new ArrayList<>();
        for (var element : cases) {
            actualWhenValues.add(element.getAsJsonObject().get("when").getAsString());
        }
        for (GolemVariant variant : GolemVariant.spawnEggVariants()) {
            String expectedWhen = "{multigolem:{variant:\"" + variant.id() + "\"}}";
            String expectedModel = "multigolem:item/" + variant.id() + "_golem_spawn_egg";
            boolean found = false;
            for (var element : cases) {
                var entry = element.getAsJsonObject();
                if (expectedWhen.equals(entry.get("when").getAsString())) {
                    assertFalse(found, "duplicate item-model case for " + variant.id() + ": " + expectedWhen);
                    assertEquals(expectedModel, entry.getAsJsonObject("model").get("model").getAsString());
                    assertSpawnEggModelAssetsExist(expectedModel);
                    found = true;
                }
            }
            assertTrue(found, "missing item-model case for " + variant.id() + "; actual when values: " + actualWhenValues);
        }
    }

    private static void assertSpawnEggModelAssetsExist(String modelId) throws Exception {
        Path modelPath = assetPath(modelId, "models", ".json")
            .orElseThrow(() -> new AssertionError("invalid model id " + modelId));
        assertTrue(Files.isRegularFile(modelPath), "missing spawn egg model asset " + modelPath);

        JsonObject model = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
        JsonObject textures = model.getAsJsonObject("textures");
        assertNotNull(textures, "missing textures block in " + modelPath);
        JsonElement layer0 = textures.get("layer0");
        assertNotNull(layer0, "missing layer0 texture in " + modelPath);
        String textureId = layer0.getAsString();
        Path texturePath = assetPath(textureId, "textures", ".png")
            .orElseThrow(() -> new AssertionError("invalid texture id " + textureId));
        assertTrue(Files.isRegularFile(texturePath), "missing spawn egg texture asset " + texturePath);
    }

    @Test
    void catalogSpawnEggAndEntityAssetPathsExist() {
        for (GolemVariant variant : GolemVariant.spawnEggVariants()) {
            GolemVariantSpec spec = GolemVariantCatalog.require(variant);

            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/multigolem/models/item").resolve(spec.spawnEggModelPath())));
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/multigolem/textures/item").resolve(spec.spawnEggTexturePath())));
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/multigolem/textures/entity").resolve(spec.entityTexturePath())));
        }
    }

    private static Optional<Path> assetPath(String id, String assetKind, String extension) {
        String[] parts = id.split(":", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(Path.of("src/main/resources/assets", parts[0], assetKind, parts[1] + extension));
    }
}
