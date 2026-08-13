package io.github.cbtc_59.voidquit.client;

import io.github.cbtc_59.voidquit.VoidQuit;
import io.github.cbtc_59.voidquit.config.VoidQuitConfig;

//#if MC < 12600
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.gui.screen.DisconnectedScreen;
//$$ import net.minecraft.client.gui.screen.TitleScreen;
//$$ import net.minecraft.server.integrated.IntegratedServer;
//$$ import net.minecraft.text.Text;
//$$ import net.minecraft.registry.RegistryKey;
//$$ import net.minecraft.world.World;
//$$ import net.minecraft.world.GameMode;
//#else
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
//#endif

public class VoidDetector {

    private static long cooldownEndTime = 0;

    //#if MC < 12600
    //$$ private static RegistryKey<World> cachedDimension;
    //$$ private static GameMode cachedGameMode;
    //#else
    private static ResourceKey<Level> cachedDimension;
    private static GameType cachedGameMode;
    //#endif
    private static double cachedTriggerY;

    private VoidDetector() {}

    static void setInitialCooldown() {
        cooldownEndTime = System.currentTimeMillis()
                + VoidQuitConfig.getInstance().cooldownSeconds * 1000L;
    }

    //#if MC < 12600
    //$$ public static void tick(MinecraftClient client) {
    //$$     VoidQuitConfig config = VoidQuitConfig.getInstance();
    //$$     if (client.isIntegratedServerRunning() ? !config.enabledSingleplayer : !config.enabledServer) {
    //$$         return;
    //$$     }
    //$$     if (client.player == null || client.player.isDead()) {
    //#else
    public static void tick(Minecraft client) {
        VoidQuitConfig config = VoidQuitConfig.getInstance();
        if (client.isSingleplayer() ? !config.enabledSingleplayer : !config.enabledServer) {
            return;
        }
        if (client.player == null || client.player.isDeadOrDying()) {
    //#endif
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

    //#if MC < 12600
    //$$ private static double getTriggerY(MinecraftClient client) {
    //$$     RegistryKey<World> dimension = client.world.getRegistryKey();
    //$$     GameMode mode = client.interactionManager.getCurrentGameMode();
    //#else
    private static double getTriggerY(Minecraft client) {
        ResourceKey<Level> dimension = client.player.level().dimension();
        GameType mode = client.gameMode.getPlayerMode();
    //#endif
        if (dimension == cachedDimension && mode == cachedGameMode) {
            return cachedTriggerY;
        }
        cachedDimension = dimension;
        cachedGameMode = mode;
        cachedTriggerY = computeTriggerY(dimension, mode);
        return cachedTriggerY;
    }

    //#if MC < 12600
    //$$ private static double computeTriggerY(RegistryKey<World> dimension, GameMode mode) {
    //$$     int worldMin = dimension == World.OVERWORLD ? -64 : 0;
    //$$     boolean isCreativeOrSpectator = mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR;
    //#else
    private static double computeTriggerY(ResourceKey<Level> dimension, GameType mode) {
        int worldMin = dimension == Level.OVERWORLD ? -64 : 0;
        boolean isCreativeOrSpectator = mode == GameType.CREATIVE || mode == GameType.SPECTATOR;
    //#endif
        int fallDepth = VoidQuitConfig.getInstance().fallDepth;
        if (isCreativeOrSpectator) {
            return Math.min(worldMin - 64.0, worldMin - fallDepth);
        }
        return worldMin - fallDepth;
    }

    //#if MC < 12600
    //$$ private static void triggerVoidQuit(MinecraftClient client) {
    //#else
    private static void triggerVoidQuit(Minecraft client) {
    //#endif
        VoidQuitConfig config = VoidQuitConfig.getInstance();

        VoidQuit.LOGGER.info("[VoidQuit] 触发虚空退出 - 玩家 Y={}，世界={}，fallDepth={}",
                (int) client.player.getY(),
                //#if MC < 12600
                //$$ client.world.getRegistryKey(),
                //#else
                client.player.level().dimension(),
                //#endif
                config.fallDepth);

        //#if MC < 12600
        //$$ client.execute(() -> {
        //$$     client.world.disconnect();
        //$$     if (!config.exitMessage.isEmpty()) {
        //$$         client.setScreen(new DisconnectedScreen(
        //$$                 new TitleScreen(),
        //$$                 Text.literal("VoidQuit"),
        //$$                 Text.literal(config.exitMessage)));
        //$$     } else {
        //$$         client.setScreen(new TitleScreen());
        //$$     }
        //$$ });
        //#else
        if (client.isSingleplayer()) {
            IntegratedServer server = client.getSingleplayerServer();
            if (server != null) { server.halt(true); }
        }
        client.disconnectFromWorld(Component.empty());
        if (!config.exitMessage.isEmpty()) {
            client.setScreen(new DisconnectedScreen(
                    new TitleScreen(),
                    Component.literal("VoidQuit"),
                    Component.literal(config.exitMessage)));
        } else {
            client.setScreen(new TitleScreen());
        }
        //#endif
    }
}
