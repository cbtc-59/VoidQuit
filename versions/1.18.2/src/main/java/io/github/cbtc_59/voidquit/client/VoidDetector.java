package io.github.cbtc_59.voidquit.client;

import io.github.cbtc_59.voidquit.VoidQuit;
import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class VoidDetector {

    private static long cooldownEndTime = 0;
    private static RegistryKey<World> cachedDimension;
    private static GameMode cachedGameMode;
    private static double cachedTriggerY;

    private VoidDetector() {}

    static void setInitialCooldown() {
        cooldownEndTime = System.currentTimeMillis()
                + VoidQuitConfig.getInstance().cooldownSeconds * 1000L;
    }

    public static void tick(MinecraftClient client) {
        VoidQuitConfig config = VoidQuitConfig.getInstance();
        if (client.isIntegratedServerRunning() ? !config.enabledSingleplayer : !config.enabledServer) {
            return;
        }
        if (client.player == null || client.player.isDead()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < cooldownEndTime) { return; }

        double triggerY = getTriggerY(client);
        if (client.player.getY() < triggerY) {
            cooldownEndTime = now + config.cooldownSeconds * 1000L;
            triggerVoidQuit(client);
        }
    }

    private static double getTriggerY(MinecraftClient client) {
        RegistryKey<World> dimension = client.world.getRegistryKey();
        GameMode mode = client.interactionManager.getCurrentGameMode();
        if (dimension == cachedDimension && mode == cachedGameMode) {
            return cachedTriggerY;
        }
        cachedDimension = dimension;
        cachedGameMode = mode;
        cachedTriggerY = computeTriggerY(dimension, mode);
        return cachedTriggerY;
    }

    private static double computeTriggerY(RegistryKey<World> dimension, GameMode mode) {
        int worldMin = dimension == World.OVERWORLD ? -64 : 0;
        boolean isCreativeOrSpectator = mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
        int fallDepth = VoidQuitConfig.getInstance().fallDepth;
        if (isCreativeOrSpectator) {
            return Math.min(worldMin - 64.0, worldMin - fallDepth);
        }
        return worldMin - fallDepth;
    }

    private static void triggerVoidQuit(MinecraftClient client) {
        VoidQuitConfig config = VoidQuitConfig.getInstance();

        VoidQuit.LOGGER.info("[VoidQuit] 触发虚空退出 - 玩家 Y={}，世界={}，fallDepth={}",
                (int) client.player.getY(),
                client.world.getRegistryKey(),
                config.fallDepth);

        // tick 回调已在渲染线程，直接同步执行退出流程
        boolean singleplayer = client.isIntegratedServerRunning();
        client.world.disconnect();
        client.disconnect();
        if (!config.exitMessage.isEmpty()) {
            client.setScreen(new DisconnectedScreen(
                    singleplayer ? new TitleScreen() : new MultiplayerScreen(new TitleScreen()),
                    new LiteralText("VoidQuit"),
                    new LiteralText(config.exitMessage)));
        } else {
            client.setScreen(new TitleScreen());
        }
    }
}
