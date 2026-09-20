/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.voidquit.client;

import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
//#if NEOFORGE && MC < 12108
//$$ import net.minecraft.client.gui.screens.ProgressScreen;
//#endif
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
//#if MC < 11900
//$$ import net.minecraft.network.chat.TextComponent;
//#endif
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 虚空检测与自动退出（全版本唯一实现）
 * 全线 Mojang 官方映射，版本差异全部由 //#if 条件块消化；
 * 断代依据来自各版本 Mojang 官方映射（client.txt）的实际查证。
 * 注意：主项目 26.1.2 不过 preprocess、直接编译本文件原文，
 * 因此所有非注释行必须保证 26.1.2 可编译，其他版本的分支一律加 //$$
 */
public class VoidDetector {

    // 日志统一用 log4j：1.16.5 classpath 无 slf4j，log4j 全版本可用；
    // 不引用 VoidQuit.LOGGER，因为该类在 NeoForge 分支不存在（入口是 VoidQuitNeoforge）
    private static final Logger LOGGER = LogManager.getLogger("voidquit");

    private static long cooldownEndTime = 0;
    private static ResourceKey<Level> cachedDimension;
    private static GameType cachedGameMode;
    private static double cachedTriggerY;

    private VoidDetector() {}

    public static void setInitialCooldown() {
        cooldownEndTime = System.currentTimeMillis()
                + VoidQuitConfig.getInstance().cooldownSeconds * 1000L;
    }

    /**
     * 清空维度/模式/触发线缓存（/voidquit reload 后调用，让新 fallDepth 立即生效）
     */
    public static void resetCache() {
        cachedDimension = null;
        cachedGameMode = null;
        cachedTriggerY = 0;
    }

    public static void tick(Minecraft client) {
        VoidQuitConfig config = VoidQuitConfig.getInstance();
        // isSingleplayer 1.19.4 起才有，更早版本用 isLocalServer（语义相同）
        //#if MC >= 11900
        boolean inSingleplayer = client.isSingleplayer();
        //#else
        //$$ boolean inSingleplayer = client.isLocalServer();
        //#endif
        if (inSingleplayer ? !config.enabledSingleplayer : !config.enabledServer) {
            return;
        }
        if (client.player == null || client.player.isDeadOrDying()) {
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

    private static double getTriggerY(Minecraft client) {
        //#if NEOFORGE || MC >= 12600
        ResourceKey<Level> dimension = client.player.level().dimension();
        //#else
        //$$ ResourceKey<Level> dimension = client.level.dimension();
        //#endif
        GameType mode = client.gameMode.getPlayerMode();
        if (dimension == cachedDimension && mode == cachedGameMode) {
            return cachedTriggerY;
        }
        cachedDimension = dimension;
        cachedGameMode = mode;
        cachedTriggerY = computeTriggerY(dimension, mode);
        return cachedTriggerY;
    }

    private static double computeTriggerY(ResourceKey<Level> dimension, GameType mode) {
        //#if MC >= 11800
        // 1.18 洞穴更新后主世界底界降为 -64（其余维度仍为 0）
        int worldMin = dimension == Level.OVERWORLD ? -64 : 0;
        //#else
        //$$ // 1.16.5/1.17.1 所有维度底界均为 0
        //$$ int worldMin = 0;
        //#endif
        boolean isCreativeOrSpectator = mode == GameType.CREATIVE || mode == GameType.SPECTATOR;
        // fallDepth 防呆：负数会变成"边界以上就触发"；上限 64 是虚空伤害线深度，
        // fallDepth 超过 64 时触发线落在死亡线之下，玩家必死在退出前，保护失效
        int fallDepth = Math.max(0, Math.min(VoidQuitConfig.getInstance().fallDepth, 64));
        if (isCreativeOrSpectator) {
            return Math.min(worldMin - 64.0, worldMin - fallDepth);
        }
        return worldMin - fallDepth;
    }

    private static void triggerVoidQuit(Minecraft client) {
        VoidQuitConfig config = VoidQuitConfig.getInstance();

        LOGGER.info("[VoidQuit] 触发虚空退出 - 玩家 Y={}，世界={}，fallDepth={}",
                (int) client.player.getY(),
                //#if NEOFORGE || MC >= 12600
                client.player.level().dimension(),
                //#else
                //$$ client.level.dimension(),
                //#endif
                config.fallDepth);

        //#if MC >= 12109 && MC < 12111
        //$$ // 1.21.9~1.21.10 的 F3 调试界面在断开屏/主菜单仍会渲染
        //$$ // （vanilla 渲染条件不再检查世界状态），断开前关闭保持界面干净
        //$$ client.debugEntries.setF3Visible(false);
        //#elseif MC >= 12111
        // 1.21.11 起 API 由 setF3Visible 改名 setOverlayVisible
        client.debugEntries.setOverlayVisible(false);
        //#endif

        // 单人判断：isSingleplayer 1.19.4 起才有，更早版本用 isLocalServer（语义相同）
        //#if MC >= 11900
        boolean singleplayer = client.isSingleplayer();
        //#else
        //$$ boolean singleplayer = client.isLocalServer();
        //#endif

        //#if MC >= 12101 && MC < 12600 && !NEOFORGE
        //$$ // Fabric 1.21.1~1.21.11：退出流程包一层 execute，在主线程任务队列中执行
        //$$ client.execute(() -> {
        //#endif
            //#if MC >= 12108
            // 1.21.8+：先断网（带退出文案），再同步等待服务器保存停止，完成后才显示断开界面
            client.level.disconnect(Component.translatable("multiplayer.status.quitting"));
            client.disconnectWithProgressScreen();
            //#elseif NEOFORGE
            //$$ // NeoForge 1.20.4~1.21.5：与 vanilla 暂停菜单「保存并退出」一致的两步：
            //$$ // 先断网，再同步等待服务器保存停止；否则服务器停止流程会再弹一个原版断开屏
            //$$ client.level.disconnect();
            //$$ if (singleplayer) {
            //$$     client.disconnect(new ProgressScreen(true));
            //$$ } else {
            //$$     client.disconnect();
            //$$ }
            //#elseif MC >= 12002
            //$$ // Minecraft.disconnect 由 clearLevel 改名而来（1.20.2）
            //$$ client.level.disconnect();
            //$$ client.disconnect();
            //#else
            //$$ client.level.disconnect();
            //$$ client.clearLevel();
            //#endif

            if (!config.exitMessage.isEmpty()) {
                //#if MC >= 12000
                // 1.20+ 断开屏多一个返回按钮文案，区分「回主菜单」/「回服务器列表」
                client.setScreen(new DisconnectedScreen(
                        singleplayer ? new TitleScreen() : new JoinMultiplayerScreen(new TitleScreen()),
                        Component.literal("VoidQuit"),
                        Component.literal(config.exitMessage),
                        singleplayer ? Component.translatable("gui.toTitle") : Component.translatable("gui.toMenu")));
                //#elseif MC >= 11900
                //$$ client.setScreen(new DisconnectedScreen(
                //$$         singleplayer ? new TitleScreen() : new JoinMultiplayerScreen(new TitleScreen()),
                //$$         Component.literal("VoidQuit"),
                //$$         Component.literal(config.exitMessage)));
                //#else
                //$$ // 1.19 前无 Component 静态工厂，用 TextComponent 构造
                //$$ client.setScreen(new DisconnectedScreen(
                //$$         singleplayer ? new TitleScreen() : new JoinMultiplayerScreen(new TitleScreen()),
                //$$         new TextComponent("VoidQuit"),
                //$$         new TextComponent(config.exitMessage)));
                //#endif
            } else {
                client.setScreen(new TitleScreen());
            }
        //#if MC >= 12101 && MC < 12600 && !NEOFORGE
        //$$ });
        //#endif
    }
}
