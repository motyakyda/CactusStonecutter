package dev.cactusskins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Конфиг мода: {@code .minecraft/config/cactusskins.json}.
 *
 * Токен — деталь реализации: юзер его не видит и не вводит. Он приходит один раз
 * при привязке аккаунта через сайт и дальше живёт здесь.
 */
public final class CactusConfig {

    /** Адрес Backend. Меняется только в режиме разработчика. */
    public String apiUrl = "https://cactusskins.motimotikydaprete.workers.dev";

    /** Привязанный ник (то, что видит юзер). Пусто = аккаунт не подключён. */
    public String nick = "";

    /** Секрет для API. Юзеру не показывается. */
    public String authToken = "";

    /** Режим разработчика: адрес Backend, диагностика, сырые ответы API. */
    public boolean devMode = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("cactusskins.json");

    private static CactusConfig instance;

    private CactusConfig() {}

    public static CactusConfig get() {
        if (instance == null) {
            try {
                if (Files.isRegularFile(FILE)) {
                    String raw = new String(Files.readAllBytes(FILE), StandardCharsets.UTF_8);
                    instance = GSON.fromJson(raw, CactusConfig.class);
                }
            } catch (Exception ignored) {
            }
            if (instance == null) instance = new CactusConfig();
            normalize(instance);
        }
        return instance;
    }

    private static void normalize(CactusConfig c) {
        if (c.apiUrl == null || c.apiUrl.trim().isEmpty()) {
            c.apiUrl = new CactusConfig().apiUrl;
        }
        c.apiUrl = c.apiUrl.trim();
        while (c.apiUrl.endsWith("/")) {
            c.apiUrl = c.apiUrl.substring(0, c.apiUrl.length() - 1);
        }
        if (c.nick == null) c.nick = "";
        if (c.authToken == null) c.authToken = "";
    }

    public static void save() {
        try {
            CactusConfig c = get();
            normalize(c);
            Files.createDirectories(FILE.getParent());
            Files.write(FILE, GSON.toJson(c).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    /** Backend доступен для запросов. */
    public static boolean isConfigured() {
        String url = get().apiUrl;
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /** Аккаунт подключён — есть ник и токен. */
    public static boolean isLinked() {
        CactusConfig c = get();
        return !c.nick.isEmpty() && !c.authToken.isEmpty();
    }

    public static void link(String nick, String token) {
        CactusConfig c = get();
        c.nick = nick == null ? "" : nick.trim();
        c.authToken = token == null ? "" : token.trim();
        save();
    }

    public static void unlink() {
        CactusConfig c = get();
        c.nick = "";
        c.authToken = "";
        save();
    }

    /** Замаскированный токен — только для режима разработчика. */
    public static String maskedToken() {
        String t = get().authToken;
        if (t.isEmpty()) return "—";
        if (t.length() <= 8) return "****";
        return t.substring(0, 4) + "…" + t.substring(t.length() - 4);
    }
}
