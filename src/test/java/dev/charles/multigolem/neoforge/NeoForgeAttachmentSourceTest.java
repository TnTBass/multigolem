package dev.charles.multigolem.neoforge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NeoForgeAttachmentSourceTest {
    @Test
    void neoforgeEntrypointRegistersStorageBeforeOtherGameplayAdapters() throws IOException {
        String source = Files.readString(Path.of("src/neoforge/java/dev/charles/multigolem/neoforge/MultiGolemNeoForge.java"));

        int attachments = source.indexOf("NeoForgeGolemAttachments.register(modBus);");
        int storage = source.indexOf("GolemStorage.register(NeoForgeGolemAttachments.storageAdapter());");
        int status = source.indexOf("MultiGolemStatus.initializeVersion");

        assertTrue(attachments >= 0, "NeoForge entrypoint must register attachment types on the mod bus");
        assertTrue(storage > attachments, "Common storage adapter registration must follow attachment registry setup");
        assertTrue(status > storage, "Status/version setup must happen after NeoForge storage is available");
    }

    @Test
    void neoforgeAttachmentTypesPreserveStoragePersistenceAndSyncContract() throws IOException {
        String source = Files.readString(Path.of("src/neoforge/java/dev/charles/multigolem/neoforge/attachment/NeoForgeGolemAttachments.java"));

        assertTrue(source.contains("DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MultiGolem.MOD_ID)"));
        assertTrue(source.contains("register(\"identity\""));
        assertTrue(source.contains("register(\"variant\""));
        assertTrue(source.contains("register(\"spawn_origin\""));
        assertTrue(source.contains("register(\"ability_state\""));
        assertTrue(source.contains("GolemIdentity.CODEC.fieldOf(\"value\")"));
        assertTrue(source.contains("GolemIdentity.STREAM_CODEC"));
        assertTrue(source.contains("GolemVariant.CODEC.fieldOf(\"value\")"));
        assertTrue(source.contains("GolemVariant.STREAM_CODEC"));
        assertTrue(source.contains("GolemSpawnOrigin.CODEC.fieldOf(\"value\")"));
        assertTrue(source.contains("GolemAbilityState.CODEC.fieldOf(\"value\")"));
        assertTrue(source.contains("private static final GolemAbilityState FRESH_ABILITY_STATE = GolemAbilityState.fresh();"));
        assertTrue(source.contains("state -> !FRESH_ABILITY_STATE.equals(state)"));
        assertTrue(source.contains("entity.syncData(IDENTITY);"));
        assertTrue(source.contains("entity.syncData(VARIANT);"));
    }

    @Test
    void neoforgeStorageAdapterMatchesCommonDefaultAndClearSemantics() throws IOException {
        String source = Files.readString(Path.of("src/neoforge/java/dev/charles/multigolem/neoforge/attachment/NeoForgeGolemAttachments.java"));

        assertTrue(source.contains("entity.getExistingData(IDENTITY)"));
        assertTrue(source.contains("entity.removeData(IDENTITY);"));
        assertTrue(source.contains("entity.getExistingData(VARIANT)"));
        assertTrue(source.contains("entity.removeData(VARIANT);"));
        assertTrue(source.contains("return entity.getExistingData(SPAWN_ORIGIN).orElse(GolemSpawnOrigin.UNKNOWN);"));
        assertTrue(source.contains("if (origin == GolemSpawnOrigin.UNKNOWN)"));
        assertTrue(source.contains("return entity.getExistingData(ABILITY_STATE).orElseGet(GolemAbilityState::fresh);"));
    }
}
