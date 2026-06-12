package dev.charles.multigolem.attachment;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemStorageTest {
    @Test
    void adapterUseBeforeLoaderRegistrationThrowsExplicitError() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, GolemStorage::adapter);

        assertEquals("MultiGolem storage adapter has not been registered by the active loader", thrown.getMessage());
    }

    @Test
    void fabricEntrypointRegistersStorageBeforeCommonInitializationAndEventWiring() throws IOException {
        String source = Files.readString(Path.of("src/fabric/java/dev/charles/multigolem/fabric/MultiGolemFabric.java"));

        int storage = source.indexOf("GolemStorage.register(FabricGolemAttachments.storageAdapter());");
        int attachments = source.indexOf("FabricGolemAttachments.register();");
        int initialize = source.indexOf("MultiGolem.initialize(");
        int events = source.indexOf("FabricMultiGolemEvents.register();");

        assertTrue(storage >= 0, "Fabric entrypoint must register the common storage adapter");
        assertTrue(attachments > storage, "Fabric attachment registration must happen after storage adapter registration");
        assertTrue(initialize > attachments, "Common initialization must happen after Fabric storage setup");
        assertTrue(events > initialize, "Fabric event wiring must happen after common initialization");
    }
}
