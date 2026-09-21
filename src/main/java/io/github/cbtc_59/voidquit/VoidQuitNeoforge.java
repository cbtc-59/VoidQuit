//#if NEOFORGE || FORGE
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
//#if NEOFORGE
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
//#if MC >= 12005
import net.neoforged.neoforge.client.event.ClientTickEvent;
//#else
//$$ import net.neoforged.neoforge.event.TickEvent;
//#endif
//#else
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
//$$ import net.minecraftforge.client.event.RegisterClientCommandsEvent;
//$$ import net.minecraftforge.common.MinecraftForge;
//$$ import net.minecraftforge.event.TickEvent;
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.fml.loading.FMLEnvironment;
//#endif
//#if FORGE && MC < 11900
//$$ // 1.19 起文本组件重构，TextComponent 仅 1.18.x 存在（client.txt 实证）
//$$ import net.minecraft.network.chat.TextComponent;
//#endif
import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VoidQuit 主入口（NeoForge / Forge）
 * 轻量级客户端模组，玩家掉入虚空时自动退出，防止死亡丢物品
 *
 * NeoForge：构造器注入 (IEventBus, ModContainer, Dist) 在全部 NeoForge 版本可用，
 * 不使用 @Mod(dist=...) 属性（1.20.4 及更早没有该属性）。
 * Forge（1.18.2~1.20.1）：javafml 仅支持无参构造器实例化（各分支 FMLModContainer 源码
 * 实证），端判断用 FMLEnvironment.dist；登录事件 1.19.2 起叫 LoggingIn，
 * 1.18.2 叫 LoggedInEvent（Forge 各分支源码实证）。
 * Fabric 节点预处理时该文件输出为空；主项目 26.1.2 不过 preprocess，
 * 由 common.gradle 的 sourceSets exclude 排除本文件
 */
@Mod("voidquit")
public class VoidQuitNeoforge {

    public static final Logger LOGGER = LoggerFactory.getLogger("voidquit");

    //#if NEOFORGE
    public VoidQuitNeoforge(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        // 触发配置加载（首次运行会自动创建默认配置文件）
        VoidQuitConfig.getInstance();
        LOGGER.info("[VoidQuit] 模组已加载，虚空退出保护已就绪");

        if (!dist.isClient()) {
            return;
        }
    //#else
    //$$ public VoidQuitNeoforge() {
    //$$     VoidQuitConfig.getInstance();
    //$$     LOGGER.info("[VoidQuit] 模组已加载，虚空退出保护已就绪");
    //$$
    //$$     if (!FMLEnvironment.dist.isClient()) {
    //$$         return;
    //$$     }
    //#endif

        // 每 tick 检查玩家高度；NeoForge 1.20.5 起 tick 事件拆分为 Pre/Post 子类，
        // Forge 全区间（1.18.2~1.20.1）与老 NeoForge 用带 Phase 的 TickEvent
        //#if MC < 12005
        gameBus().addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                VoidDetector.tick(Minecraft.getInstance());
            }
        });
        //#else
        gameBus().addListener((ClientTickEvent.Post event) -> {
            VoidDetector.tick(Minecraft.getInstance());
        });
        //#endif

        // 进服冷却；Forge 1.18.2 的登录事件叫 LoggedInEvent，1.19.2 起改名 LoggingIn
        //#if FORGE && MC < 11902
        //$$ gameBus().addListener((ClientPlayerNetworkEvent.LoggedInEvent event) -> {
        //#else
        gameBus().addListener((ClientPlayerNetworkEvent.LoggingIn event) -> {
        //#endif
            VoidDetector.setInitialCooldown();
        });

        // /voidquit reload 客户端命令；RegisterClientCommandsEvent 全 NeoForge 与
        // Forge 1.18.2~1.20.1 可用（Forge 1.18.2 javadoc 实证；NeoForge 侧本地 sources 查证）
        gameBus().addListener((RegisterClientCommandsEvent event) -> {
            event.getDispatcher().register(
                    Commands.literal("voidquit")
                            .then(Commands.literal("reload").executes(ctx -> {
                                VoidQuitConfig.reload();
                                VoidDetector.resetCache();
                                // 聊天反馈断代：26.x 起 ChatComponent.addMessage(Component) 改名 addClientSystemMessage；
                                // 文本工厂断代：1.19 起用 Component.literal，1.18.x 用 new TextComponent；
                                // Gui.getChat() 与 addMessage 在 Forge 区间（1.18.2~1.20.1）client.txt 实证存在
                                //#if MC >= 12600
                                Minecraft.getInstance().gui.getChat().addClientSystemMessage(
                                        Component.literal("VoidQuit 配置已重载"));
                                //#elseif MC >= 11900
                                //$$ Minecraft.getInstance().gui.getChat().addMessage(
                                //$$         Component.literal("VoidQuit 配置已重载"));
                                //#else
                                //$$ Minecraft.getInstance().gui.getChat().addMessage(
                                //$$         new TextComponent("VoidQuit 配置已重载"));
                                //#endif
                                return Command.SINGLE_SUCCESS;
                            })));
        });
    }

    // 游戏总线引用两侧包名不同，收敛到一个 helper：NeoForge 是 NeoForge.EVENT_BUS，
    // Forge 是 MinecraftForge.EVENT_BUS；返回类型各自的全限定名不进公共 import
    //#if NEOFORGE
    private static net.neoforged.bus.api.IEventBus gameBus() {
        return NeoForge.EVENT_BUS;
    }
    //#else
    //$$ private static net.minecraftforge.eventbus.api.IEventBus gameBus() {
    //$$     return MinecraftForge.EVENT_BUS;
    //$$ }
    //#endif
}
//#endif
