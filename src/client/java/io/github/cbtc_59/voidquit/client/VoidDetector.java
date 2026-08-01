package io.github.cbtc_59.voidquit.client;

import io.github.cbtc_59.voidquit.VoidQuit;
import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;

/**
 * 虚空检测核心逻辑
 * 每个客户端 tick 检测玩家 Y 坐标，掉入虚空时触发退出。
 *
 * 触发 Y 按需缓存：只在维度或游戏模式变化时重算。
 */
public class VoidDetector {

    private static long cooldownEndTime = 0;
    public static boolean shouldShowToast = false;

    /** 缓存：上次计算触发 Y 时的维度 */
    private static ResourceKey<Level> cachedDimension;
    /** 缓存：上次计算触发 Y 时的游戏模式 */
    private static GameType cachedGameMode;
    /** 缓存：上次计算出的触发 Y */
    private static double cachedTriggerY;

    private VoidDetector() {
    }

    /** 设置初始冷却（进世界时调用，防止出生在虚空立即触发） */
    static void setInitialCooldown() {
        cooldownEndTime = System.currentTimeMillis()
                + VoidQuitConfig.getInstance().cooldownSeconds * 1000L;
    }

    public static void tick(Minecraft client) {
        // 检查功能开关
        VoidQuitConfig config = VoidQuitConfig.getInstance();
        if (client.isSingleplayer() ? !config.enabledSingleplayer : !config.enabledServer) {
            return;
        }

        if (client.player == null || client.player.isDeadOrDying()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < cooldownEndTime) {
            return;
        }

        // 获取触发 Y（维度/模式未变时直接读缓存）
        double triggerY = getTriggerY(client);

        if (client.player.getY() < triggerY) {
            cooldownEndTime = now + VoidQuitConfig.getInstance().cooldownSeconds * 1000L;
            triggerVoidQuit(client);
        }
    }

    /**
     * 获取触发 Y 阈值，维度/游戏模式未变时使用缓存。
     */
    private static double getTriggerY(Minecraft client) {
        ResourceKey<Level> dimension = client.player.level().dimension();
        GameType mode = client.gameMode.getPlayerMode();

        // 维度和模式都没变 → 直接返回缓存
        if (dimension == cachedDimension && mode == cachedGameMode) {
            return cachedTriggerY;
        }

        // 缓存失效，重新计算
        cachedDimension = dimension;
        cachedGameMode = mode;
        cachedTriggerY = computeTriggerY(dimension, mode);
        return cachedTriggerY;
    }

    private static double computeTriggerY(ResourceKey<Level> dimension, GameType mode) {
        int worldMin = dimension == Level.OVERWORLD ? -64 : 0;
        int fallDepth = VoidQuitConfig.getInstance().fallDepth;
        boolean isCreativeOrSpectator = mode == GameType.CREATIVE || mode == GameType.SPECTATOR;

        if (isCreativeOrSpectator) {
            return Math.min(worldMin - 64.0, worldMin - fallDepth);
        } else {
            return worldMin - fallDepth;
        }
    }

    private static void triggerVoidQuit(Minecraft client) {
        VoidQuitConfig config = VoidQuitConfig.getInstance();

        VoidQuit.LOGGER.info("[VoidQuit] 触发虚空退出 - 玩家 Y={}，世界={}，fallDepth={}",
                (int) client.player.getY(),
                client.player.level().dimension(),
                config.fallDepth);

        if (!config.toastMessage.isEmpty()) {
            Gui gui = client.gui;
            gui.setOverlayMessage(Component.literal(config.toastMessage), false);
        }

        shouldShowToast = true;

        if (client.isSingleplayer()) {
            IntegratedServer server = client.getSingleplayerServer();
            if (server != null) {
                server.halt(true);
            }
            client.disconnectFromWorld(Component.empty());
            client.setScreen(new TitleScreen());
        } else {
            client.disconnectFromWorld(Component.translatable("disconnect.disconnected"));
        }
    }
}
