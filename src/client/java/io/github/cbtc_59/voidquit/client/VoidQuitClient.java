package io.github.cbtc_59.voidquit.client;

import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/**
 * VoidQuit 客户端入口
 * 注册 tick 事件和屏幕事件
 */
public class VoidQuitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册客户端 tick 事件：每 tick 检测虚空
        ClientTickEvents.END_CLIENT_TICK.register(VoidDetector::tick);

        // 加入世界时清除残留的 Toast 标记，设置初始冷却（防止出生在虚空）
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            VoidDetector.shouldShowToast = false;
            VoidDetector.setInitialCooldown();
        });

        // 注册屏幕初始化事件：退出后回到任意界面时显示 Toast
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (VoidDetector.shouldShowToast) {
                VoidDetector.shouldShowToast = false;
                VoidQuitConfig config = VoidQuitConfig.getInstance();
                if (!config.toastMessage.isEmpty()) {
                    showToast(client, config.toastMessage);
                }
            }
        });
    }

    /**
     * 在标题画面显示 Toast 通知
     */
    private static void showToast(Minecraft client, String message) {
        client.getToastManager().addToast(
                SystemToast.multiline(
                        client,
                        SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                        Component.literal("VoidQuit"),
                        Component.literal(message)
                )
        );
    }
}
