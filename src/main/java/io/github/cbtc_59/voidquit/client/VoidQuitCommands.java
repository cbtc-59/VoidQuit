//#if !NEOFORGE
/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.voidquit.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
//#if MC >= 12600
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
//#elseif MC >= 11904
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
//#else
//$$ import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
//#endif
//#if MC >= 11904
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
//#else
//$$ import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
//#endif
import net.minecraft.network.chat.Component;
//#if MC < 11900
//$$ import net.minecraft.network.chat.TextComponent;
//#endif

/**
 * /voidquit reload 客户端命令：重新读取配置并清空触发线缓存，改 json 后无需重启游戏
 *
 * 注册方式断代（查证依据：各节点 fabric-api jar 实际内容，2026-09-20）：
 * - 1.16.5~1.18.2：command-api-v1，无注册回调，直接向 ClientCommandManager.DISPATCHER 注册
 * - 1.19.4~1.21.11：command-api-v2，ClientCommandRegistrationCallback 注册
 * - 26.1.2：v2 但 ClientCommandManager 改名 ClientCommands
 * 根 src 为 26.1.2 形态（主项目不过 preprocess 直接编译）
 */
public final class VoidQuitCommands {

    private VoidQuitCommands() {
    }

    public static void register() {
        //#if MC >= 11904
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildTree());
        });
        //#else
        //$$ ClientCommandManager.DISPATCHER.register(buildTree());
        //#endif
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildTree() {
        //#if MC >= 12600
        return ClientCommands.literal("voidquit")
                .then(ClientCommands.literal("reload").executes(VoidQuitCommands::reloadConfig));
        //#else
        //$$ return ClientCommandManager.literal("voidquit")
        //$$         .then(ClientCommandManager.literal("reload").executes(VoidQuitCommands::reloadConfig));
        //#endif
    }

    private static int reloadConfig(CommandContext<FabricClientCommandSource> ctx) {
        VoidQuitConfig.reload();
        VoidDetector.resetCache();
        //#if MC >= 11900
        ctx.getSource().sendFeedback(Component.literal("VoidQuit 配置已重载"));
        //#else
        //$$ ctx.getSource().sendFeedback(new TextComponent("VoidQuit 配置已重载"));
        //#endif
        return Command.SINGLE_SUCCESS;
    }
}
//#endif
