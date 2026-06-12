package dev.charles.multigolem.attachment;

import java.util.Objects;

public final class GolemStorage {
    private static volatile GolemStorageAdapter adapter;

    private GolemStorage() {}

    public static void register(GolemStorageAdapter storageAdapter) {
        adapter = Objects.requireNonNull(storageAdapter, "storageAdapter");
    }

    static GolemStorageAdapter adapter() {
        GolemStorageAdapter current = adapter;
        if (current == null) {
            throw new IllegalStateException("MultiGolem storage adapter has not been registered by the active loader");
        }
        return current;
    }
}
