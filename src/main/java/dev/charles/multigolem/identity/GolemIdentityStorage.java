package dev.charles.multigolem.identity;

import dev.charles.multigolem.GolemVariant;

import java.util.Optional;

public interface GolemIdentityStorage {
    Optional<GolemIdentity> rawIdentity();
    void setRawIdentity(GolemIdentity identity);
    void clearRawIdentity();
    Optional<GolemVariant> rawVariant();
    void setRawVariant(GolemVariant variant);
    void clearRawVariant();
}
