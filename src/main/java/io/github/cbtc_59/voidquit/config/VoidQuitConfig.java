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
    public String exitMessage = "已自动退出，防止虚空死亡";

    private VoidQuitConfig() {
    }

    public static VoidQuitConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static VoidQuitConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, VoidQuitConfig.class);
            } catch (Exception e) {
                LOGGER.error("[VoidQuit] 读取配置文件失败，使用默认配置: {}", e.getMessage());
            }
        }
        VoidQuitConfig config = new VoidQuitConfig();
        config.save();
        return config;
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
