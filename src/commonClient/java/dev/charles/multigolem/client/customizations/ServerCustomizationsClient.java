package dev.charles.multigolem.client.customizations;

public final class ServerCustomizationsClient {
    private static final int TIMEOUT_TICKS = 100;
    private static final ServerCustomizationsClientState STATE = new ServerCustomizationsClientState(TIMEOUT_TICKS);

    private ServerCustomizationsClient() {
    }

    public static ServerCustomizationsClientState state() {
        return STATE;
    }
}
