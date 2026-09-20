//#if NEOFORGE
/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.voidquit;

import io.github.cbtc_59.voidquit.client.VoidDetector;
import com.mojang.brigadier.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
//#if MC < 12005
//$$ import net.neoforged.neoforge.event.TickEvent;
//#else
import net.neoforged.neoforge.client.event.ClientTickEvent;
//#endif
import net.neoforged.neoforge.common.NeoForge;
import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VoidQuit 主入口（NeoForge）
 * 轻量级客户端模组，玩家掉入虚空时自动退出，防止死亡丢物品
 *
 * 构造器注入 (IEventBus, ModContainer, Dist) 在全部 NeoForge 版本可用，
 * 不使用 @Mod(dist=...) 属性（1.20.4 及更早没有该属性）。
 * Fabric 节点预处理时该文件输出为空；主项目 26.1.2 不过 preprocess，
 * 由 common.gradle 的 sourceSets exclude 排除本文件
 */
@Mod("voidquit")
public class VoidQuitNeoforge {

    public static final Logger LOGGER = LoggerFactory.getLogger("voidquit");

    public VoidQuitNeoforge(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        // 触发配置加载（首次运行会自动创建默认配置文件）
        VoidQuitConfig.getInstance();
        LOGGER.info("[VoidQuit] 模组已加载，虚空退出保护已就绪");

        if (!dist.isClient()) {
            return;
        }

        // 每 tick 检查玩家高度；1.20.5 起 tick 事件拆分为 Pre/Post 子类，之前是带 Phase 的单事件
        //#if MC < 12005
        //$$ NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
        //$$     if (event.phase == TickEvent.Phase.END) {
        //$$         VoidDetector.tick(Minecraft.getInstance());
        //$$     }
        //$$ });
        //#else
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            VoidDetector.tick(Minecraft.getInstance());
        });
        //#endif

        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
            VoidDetector.setInitialCooldown();
        });

        // /voidquit reload 客户端命令；RegisterClientCommandsEvent 签名在 1.20.4~26.1.2 一致（本地 sources 查证）
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> {
            event.getDispatcher().register(
                    Commands.literal("voidquit")
                            .then(Commands.literal("reload").executes(ctx -> {
                                VoidQuitConfig.reload();
                                VoidDetector.resetCache();
                                // 聊天反馈断代：26.x 起 ChatComponent.addMessage(Component) 改名 addClientSystemMessage
                                //#if MC >= 12600
                                Minecraft.getInstance().gui.getChat().addClientSystemMessage(
                                        Component.literal("VoidQuit 配置已重载"));
                                //#else
                                //$$ Minecraft.getInstance().gui.getChat().addMessage(
                                //$$         Component.literal("VoidQuit 配置已重载"));
                                //#endif
                                return Command.SINGLE_SUCCESS;
                            })));
        });
    }
}
//#endif