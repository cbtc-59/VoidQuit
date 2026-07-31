package io.github.cbtc_59.voidquit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * VoidQuit 配置管理类
 * 负责读取和保存 config/voidquit.json
 */
public class VoidQuitConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("voidquit.json");
    private static VoidQuitConfig instance;

    @SerializedName("fallDepth")
    public int fallDepth = 24;

    @SerializedName("cooldownSeconds")
    public int cooldownSeconds = 5;

    @SerializedName("toastMessage")
    public String toastMessage = "VoidQuit: 已自动退出，防止虚空死亡";

    private VoidQuitConfig() {
    }

    /**
     * 获取配置单例
     */
    public static VoidQuitConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /**
     * 从文件加载配置，文件不存在时使用默认值并自动创建
     */
    private static VoidQuitConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, VoidQuitConfig.class);
            } catch (Exception e) {
                System.err.println("[VoidQuit] 读取配置文件失败，使用默认配置: " + e.getMessage());
            }
        }
        // 配置文件不存在或读取失败，创建默认配置
        VoidQuitConfig config = new VoidQuitConfig();
        config.save();
        return config;
    }

    /**
     * 保存当前配置到文件
     */
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[VoidQuit] 保存配置文件失败: " + e.getMessage());
        }
    }
}
