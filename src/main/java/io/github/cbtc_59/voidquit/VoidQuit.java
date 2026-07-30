package io.github.cbtc_59.voidquit;

import io.github.cbtc_59.voidquit.config.VoidQuitConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * VoidQuit 主入口
 * 轻量级客户端模组，玩家掉入虚空时自动退出，防止死亡丢物品
 */
public class VoidQuit implements ModInitializer {

    public static final String MOD_ID = "voidquit";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 触发配置加载（首次运行会自动创建默认配置文件）
        VoidQuitConfig.getInstance();
        LOGGER.info("[VoidQuit] 模组已加载，虚空退出保护已就绪");
    }
}
