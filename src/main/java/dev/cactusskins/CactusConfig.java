package dev.cactusskins;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфиг мода: {@code .minecraft/config/cactusskins.json}.
 * Адрес Backend меняется в игре (кнопка "Skins"), пересборка не нужна.
 */
public final class CactusConfig {

    public String apiUrl = "https://cactusskins.motimotikydaprete.workers.dev";
    public String authToken = "";

    private static final Gson GSON = new Gson();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("cactusskins.json");
    private static CactusConfig instance;

    private CactusConfig() {}

    public static CactusConfig get() {
        if (instance == null) {
            try {
                if (Files.isRegularFile(FILE)) {
                    instance = GSON.fromJson(new String(Files.readAllBytes(FILE), java.nio.charset.StandardCharsets.UTF_8), CactusConfig.class);
                }
            } catch (Exception ignored) {
            }
            if (instance == null) instance = new CactusConfig();
            if (instance.apiUrl == null || instance.apiUrl.trim().isEmpty()) {
                instance.apiUrl = new CactusConfig().apiUrl;
            }
            while (instance.apiUrl.endsWith("/")) {
                instance.apiUrl = instance.apiUrl.substring(0, instance.apiUrl.length() - 1);
            }
            if (instance.authToken == null) instance.authToken = "";
        }
        return instance;
    }

    public static void save() {
        try {
            Files.write(FILE, GSON.toJson(get()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static boolean isConfigured() {
        String url = get().apiUrl;
        return !url.trim().isEmpty() && !url.contains("ТВОЙ");
    }
}
