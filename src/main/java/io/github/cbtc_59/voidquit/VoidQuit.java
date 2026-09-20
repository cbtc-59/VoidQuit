//#if !NEOFORGE
/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.voidquit;

import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * VoidQuit 主入口（Fabric）
 * 轻量级客户端模组，玩家掉入虚空时自动退出，防止死亡丢物品
 */
public class VoidQuit implements ModInitializer {

    public static final String MOD_ID = "voidquit";
    // 日志统一用 log4j：1.16.5 classpath 无 slf4j，log4j 全版本可用
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 触发配置加载（首次运行会自动创建默认配置文件）
        VoidQuitConfig.getInstance();
        LOGGER.info("[VoidQuit] 模组已加载，虚空退出保护已就绪");
    }
}
//#endif