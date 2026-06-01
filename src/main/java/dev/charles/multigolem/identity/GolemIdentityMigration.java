package dev.charles.multigolem.identity;

public final class GolemIdentityMigration {
    private GolemIdentityMigration() {}

    public static GolemIdentity resolve(GolemIdentityStorage storage) {
        return storage.rawIdentity()
            .filter(GolemIdentity::isValidForPhase3)
            .orElseGet(() -> storage.rawVariant()
                .map(GolemIdentity::ofIronVariant)
                .orElse(GolemIdentity.defaultIron()));
    }

    public static void write(GolemIdentityStorage storage, GolemIdentity identity) {
        if (identity == null || !identity.isValidForPhase3() || identity.isDefaultIron()) {
            storage.clearRawIdentity();
            storage.clearRawVariant();
            return;
        }
        storage.setRawIdentity(identity);
        storage.setRawVariant(identity.variant());
    }
}
