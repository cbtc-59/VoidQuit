package io.github.cbtc_59.voidquit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class VoidQuitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(VoidDetector::tick);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            VoidDetector.setInitialCooldown();
        });
    }
}
