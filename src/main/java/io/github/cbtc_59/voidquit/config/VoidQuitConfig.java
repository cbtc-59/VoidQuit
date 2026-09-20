/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.voidquit.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
//#if NEOFORGE
//$$ import net.neoforged.fml.loading.FMLPaths;
//#else
import net.fabricmc.loader.api.FabricLoader;
//#endif

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * VoidQuit 配置管理类
 * 负责读取和保存 config/voidquit.json
 */
public class VoidQuitConfig {

    // 日志统一用 log4j：1.16.5 classpath 无 slf4j，log4j 全版本可用
    private static final Logger LOGGER = LogManager.getLogger("voidquit");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String DEFAULT_EXIT_MESSAGE = "已自动退出，防止虚空死亡";
//#if NEOFORGE
//$$ private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("voidquit.json");
//#else
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("voidquit.json");
//#endif
    private static VoidQuitConfig instance;

    @SerializedName("fallDepth")
    public int fallDepth = 24;

    @SerializedName("cooldownSeconds")
    public int cooldownSeconds = 5;

    @SerializedName("enabledSingleplayer")
    public boolean enabledSingleplayer = true;

    @SerializedName("enabledServer")
    public boolean enabledServer = true;

    @SerializedName("exitMessage")
    public String exitMessage = DEFAULT_EXIT_MESSAGE;

    private VoidQuitConfig() {
    }

    public static VoidQuitConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * 重新读取配置文件（/voidquit reload 命令入口），改 json 后无需重启游戏
     */
    public static void reload() {
        instance = load();
    }

    private static VoidQuitConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                VoidQuitConfig config = GSON.fromJson(reader, VoidQuitConfig.class);
                // JSON 顶层为字面 null 时 Gson 不抛异常而是返回 null：
                // 不拦住的话 instance 会存进 null，getInstance 每 tick 重读文件并最终 NPE 崩溃循环
                if (config != null) {
                    if (config.normalize()) {
                        // 有字段被修复，回写文件自愈，用户打开看到的已是合法值
                        config.save();
                    }
                    return config;
                }
                LOGGER.error("[VoidQuit] 配置文件内容为空，使用默认配置");
            } catch (Exception e) {
                LOGGER.error("[VoidQuit] 读取配置文件失败，使用默认配置: {}", e.getMessage());
            }
        }
        VoidQuitConfig config = new VoidQuitConfig();
        config.save();
        return config;
    }

    /**
     * 修复手编配置产生的非法字段值。Gson 对显式 null 不报错而是静默置 null
     * （如 "exitMessage": null 会让 exitMessage.isEmpty() 崩溃），对越界数字照单全收。
     *
     * @return 是否有字段被修复，由调用方决定是否回写文件
     */
    private boolean normalize() {
        boolean changed = false;
        if (exitMessage == null) {
            exitMessage = DEFAULT_EXIT_MESSAGE;
            changed = true;
        }
        if (cooldownSeconds < 0) {
            cooldownSeconds = 5;
            changed = true;
        }
        if (fallDepth < 0) {
            fallDepth = 24;
            changed = true;
        }
        return changed;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[VoidQuit] 保存配置文件失败: {}", e.getMessage());
        }
    }
}
