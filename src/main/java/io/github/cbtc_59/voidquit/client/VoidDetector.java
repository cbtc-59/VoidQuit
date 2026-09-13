package io.github.cbtc_59.voidquit.client;

import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
//#if MC < 12600 && !NEOFORGE
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
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
//#endif
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoidDetector {

    // 模组日志统一用 voidquit 名称；不引用 VoidQuit.LOGGER，
    // 因为该类在 NeoForge 分支不存在（入口是 VoidQuitNeoforge）
    private static final Logger LOGGER = LoggerFactory.getLogger("voidquit");

    private static long cooldownEndTime = 0;

    //#if MC < 12600 && !NEOFORGE
    //$$ private static RegistryKey<World> cachedDimension;
    //$$ private static GameMode cachedGameMode;
    //#else
    private static ResourceKey<Level> cachedDimension;
    private static GameType cachedGameMode;
    //#endif
    private static double cachedTriggerY;

    private VoidDetector() {}

    public static void setInitialCooldown() {
        cooldownEndTime = System.currentTimeMillis()
                + VoidQuitConfig.getInstance().cooldownSeconds * 1000L;
    }

    //#if MC < 12600 && !NEOFORGE
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

    //#if MC < 12600 && !NEOFORGE
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

    //#if MC < 12600 && !NEOFORGE
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

    //#if MC < 12600 && !NEOFORGE
    //$$ private static void triggerVoidQuit(MinecraftClient client) {
    //#else
    private static void triggerVoidQuit(Minecraft client) {
    //#endif
        VoidQuitConfig config = VoidQuitConfig.getInstance();

        LOGGER.info("[VoidQuit] 触发虚空退出 - 玩家 Y={}，世界={}，fallDepth={}",
                (int) client.player.getY(),
                //#if MC < 12600 && !NEOFORGE
                //$$ client.world.getRegistryKey(),
                //#else
                client.player.level().dimension(),
                //#endif
                config.fallDepth);

        //#if MC < 12600 && !NEOFORGE
        //$$ client.execute(() -> {
        //#if MC >= 12109 && MC < 12111
        //$$     // 1.21.9+ 的 F3 界面在断开屏/主菜单仍会渲染（vanilla 行为变化），断开前关闭
        //$$     client.debugHudEntryList.setF3Enabled(false);
        //#endif
        //#if MC >= 12111
        //$$     // 1.21.11 起 API 由 setF3Enabled 改名 setOverlayVisible
        //$$     client.debugEntries.setOverlayVisible(false);
        //#endif
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
        //#if MC >= 12109
        // 1.21.9+ 的 F3 调试界面在断开屏/主菜单仍会渲染（vanilla 渲染条件不再检查世界状态），
        // 退出前关闭保持界面干净；1.21.11 起 API 由 setF3Visible 改名 setOverlayVisible
        //#if MC < 12111
        //$$ client.debugEntries.setF3Visible(false);
        //#else
        client.debugEntries.setOverlayVisible(false);
        //#endif
        //#endif
        //#if NEOFORGE && MC < 12106
        //$$ // 与 vanilla 暂停菜单「保存并退出」一致的两步：先断网，再同步等待服务器保存停止，
        //$$ // 完成后才显示断开界面；否则服务器停止流程会再弹一个原版断开屏，形成两层按钮
        //$$ boolean singleplayer = client.isLocalServer();
        //$$ client.level.disconnect();
        //$$ if (singleplayer) {
        //$$     client.disconnect(new ProgressScreen(true));
        //$$ } else {
        //$$     client.disconnect();
        //$$ }
        //#else
        boolean singleplayer = client.isSingleplayer();
        client.level.disconnect(Component.translatable("multiplayer.status.quitting"));
        client.disconnectWithProgressScreen();
        //#endif
        if (!config.exitMessage.isEmpty()) {
            client.setScreen(new DisconnectedScreen(
                    singleplayer ? new TitleScreen() : new JoinMultiplayerScreen(new TitleScreen()),
                    Component.literal("VoidQuit"),
                    Component.literal(config.exitMessage),
                    singleplayer ? Component.translatable("gui.toTitle") : Component.translatable("gui.toMenu")));
        } else {
            client.setScreen(new TitleScreen());
        }
        //#endif
    }
}
