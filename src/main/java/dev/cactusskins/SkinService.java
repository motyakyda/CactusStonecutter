package dev.cactusskins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/** Общение с Backend: привязка аккаунта, библиотека скинов, раздача текстур. */
public final class SkinService {

    // ─────────────────────────────────────────────────────────── результаты

    /** Универсальный ответ: ок/ошибка + текст. */
    public static final class Result {
        private final boolean ok;
        private final String error;

        public Result(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public boolean ok() { return ok; }
        public String error() { return error; }

        public static Result success() { return new Result(true, null); }
        public static Result fail(String e) { return new Result(false, e); }
    }

    /** Ответ {@code /api/pair/start}. */
    public static final class PairStart {
        public final String code;
        public final String verifyUrl;
        public final String error;

        PairStart(String code, String verifyUrl, String error) {
            this.code = code;
            this.verifyUrl = verifyUrl;
            this.error = error;
        }

        public boolean ok() { return error == null; }
    }

    /** Ответ {@code /api/pair/poll}. */
    public static final class PairPoll {
        public final boolean pending;
        public final boolean linked;
        public final String nick;
        public final String token;
        public final String error;

        PairPoll(boolean pending, boolean linked, String nick, String token, String error) {
            this.pending = pending;
            this.linked = linked;
            this.nick = nick;
            this.token = token;
            this.error = error;
        }
    }

    private static Path cacheDir;
    private static final AtomicInteger PREVIEW_SEQ = new AtomicInteger();

    private SkinService() {}

    // ─────────────────────────────────────────────────────── привязка аккаунта

    public static CompletableFuture<PairStart> pairStart() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = httpSend("POST", api("/api/pair/start"), null, null, null);
                if (body == null) return new PairStart(null, null, "Backend не отвечает");
                JsonObject o = parse(body);
                if (o.has("error")) return new PairStart(null, null, o.get("error").getAsString());
                return new PairStart(
                        o.get("code").getAsString(),
                        o.get("verify_url").getAsString(),
                        null);
            } catch (Exception e) {
                return new PairStart(null, null, msg(e));
            }
        });
    }

    public static CompletableFuture<PairPoll> pairPoll(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = httpGet(api("/api/pair/poll?code=" + code));
                if (body == null) return new PairPoll(true, false, null, null, null);
                JsonObject o = parse(body);
                if (o.has("pending")) return new PairPoll(true, false, null, null, null);
                if (o.has("ok")) {
                    return new PairPoll(false, true,
                            o.get("nick").getAsString(),
                            o.get("token").getAsString(), null);
                }
                String err = o.has("error") ? o.get("error").getAsString() : "Код истёк";
                return new PairPoll(false, false, null, null, err);
            } catch (Exception e) {
                return new PairPoll(true, false, null, null, null);
            }
        });
    }

    // ────────────────────────────────────────────────────────────── библиотека

    public static CompletableFuture<SkinEntry.Library> library() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = httpSend("GET", api("/api/me"), null, null, token());
                if (body == null) return null;
                JsonObject o = parse(body);
                if (!o.has("skins")) return null;

                List<SkinEntry> list = new ArrayList<>();
                JsonArray arr = o.getAsJsonArray("skins");
                for (JsonElement el : arr) {
                    JsonObject s = el.getAsJsonObject();
                    list.add(new SkinEntry(
                            s.get("id").getAsString(),
                            s.has("name") ? s.get("name").getAsString() : "Скин",
                            "slim".equalsIgnoreCase(str(s, "model", "classic")),
                            s.has("cape") && s.get("cape").getAsBoolean(),
                            s.has("ts") ? s.get("ts").getAsLong() : 0L));
                }
                return new SkinEntry.Library(
                        str(o, "nick", CactusConfig.get().nick),
                        o.has("active") && !o.get("active").isJsonNull()
                                ? o.get("active").getAsString() : null,
                        list);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public static CompletableFuture<Result> activate(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String payload = "{\"id\":\"" + id + "\"}";
                String body = httpSend("POST", api("/api/skins/activate"),
                        payload.getBytes(StandardCharsets.UTF_8), "application/json", token());
                return checkOk(body);
            } catch (Exception e) {
                return Result.fail(msg(e));
            }
        });
    }

    public static CompletableFuture<Result> rename(String id, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject o = new JsonObject();
                o.addProperty("id", id);
                o.addProperty("name", name);
                String body = httpSend("POST", api("/api/skins/rename"),
                        o.toString().getBytes(StandardCharsets.UTF_8), "application/json", token());
                return checkOk(body);
            } catch (Exception e) {
                return Result.fail(msg(e));
            }
        });
    }

    public static CompletableFuture<Result> delete(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = httpSend("DELETE", api("/api/skins?id=" + id), null, null, token());
                return checkOk(body);
            } catch (Exception e) {
                return Result.fail(msg(e));
            }
        });
    }

    /** Добавляет скин в библиотеку и сразу надевает его. */
    public static CompletableFuture<Result> addSkin(String name, boolean slim, File skin, File cape) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String boundary = "----CactusSkins" + Long.toHexString(System.nanoTime());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                part(bos, boundary, "name", name);
                part(bos, boundary, "model", slim ? "slim" : "classic");
                part(bos, boundary, "activate", "1");
                if (skin != null) {
                    filePart(bos, boundary, "skin", "skin.png", Files.readAllBytes(skin.toPath()));
                }
                if (cape != null) {
                    filePart(bos, boundary, "cape", "cape.png", Files.readAllBytes(cape.toPath()));
                }
                bos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1));

                String body = httpSend("POST", api("/api/skins"), bos.toByteArray(),
                        "multipart/form-data; boundary=" + boundary, token());
                return checkOk(body);
            } catch (Exception e) {
                return Result.fail(msg(e));
            }
        });
    }

    // ─────────────────────────────────────────────────── превью для библиотеки

    /** Скачивает превью скина из библиотеки и регистрирует текстуру. */
    public static CompletableFuture<Identifier> preview(String nick, String id) {
        return CompletableFuture.supplyAsync(() -> {
            byte[] bytes = httpGetBytes(api("/library/" + nick + "/" + id + ".png"));
            if (bytes == null) return null;
            return registerImage("preview/" + id + "_" + PREVIEW_SEQ.incrementAndGet(), bytes);
        });
    }

    // ────────────────────────────────────────────── раздача скинов игрокам

    public static void fetch(GameProfile profile) {
        if (!CactusConfig.isConfigured()) return;

        String nick = CactusSkins.profileName(profile).toLowerCase();
        CompletableFuture.runAsync(() -> {
            try {
                String infoJson = httpGet(api("/api/info/" + nick));
                if (infoJson == null) return;
                JsonObject info = parse(infoJson);
                if (!info.has("found") || !info.get("found").getAsBoolean()) return;

                boolean slim = "slim".equalsIgnoreCase(str(info, "model", "classic"));
                boolean hasCape = info.has("cape") && info.get("cape").getAsBoolean();

                Identifier skin = cachedTexture(nick, "/skins/" + nick + ".png",
                        CactusSkins.profileId(profile) + "/skin");
                if (skin == null) return;

                Identifier cape = hasCape
                        ? cachedTexture(nick, "/capes/" + nick + ".png",
                                CactusSkins.profileId(profile) + "/cape")
                        : null;

                CactusSkins.OVERRIDES.put(CactusSkins.profileId(profile),
                        new PlayerSkinData(skin, cape, slim));
            } catch (Exception ignored) {
            }
        });
    }

    private static Identifier cachedTexture(String nick, String remotePath, String localKey) {
        try {
            Path cached = cache().resolve(nick + "." + localKey.replace('/', '_') + ".png");
            byte[] bytes = httpGetBytes(api(remotePath));
            if (bytes == null) {
                if (!Files.isRegularFile(cached)) return null;
                bytes = Files.readAllBytes(cached);
            } else {
                Files.write(cached, bytes);
            }
            return registerImage(localKey, bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static Identifier registerImage(String key, byte[] bytes) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            Identifier id = id(key.toLowerCase());
            MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance()
                    .getTextureManager()
                    .registerTexture(id, nativeTexture(image)));
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    private static NativeImageBackedTexture nativeTexture(NativeImage image) {
        //? if >=1.21.0 {
        return new NativeImageBackedTexture(() -> "cactusskins", image);
        //?} else {
        /*return new NativeImageBackedTexture(image);
        *///?}
    }

    private static Identifier id(String key) {
        //? if >=1.21.0 {
        return Identifier.of("cactusskins", key);
        //?} else {
        /*return new Identifier("cactusskins", key);
        *///?}
    }

    public static void clearDiskCache() {
        File[] files = cache().toFile().listFiles();
        if (files == null) return;
        for (File f : files) {
            try { Files.deleteIfExists(f.toPath()); } catch (Exception ignored) { }
        }
    }

    // ───────────────────────────────────────────────────────────── HTTP слой

    private static String api(String path) {
        return CactusConfig.get().apiUrl + path;
    }

    private static String token() {
        String t = CactusConfig.get().authToken;
        return t.isEmpty() ? null : t;
    }

    private static Result checkOk(String body) {
        if (body == null) return Result.fail("Backend не отвечает");
        try {
            JsonObject o = parse(body);
            if (o.has("ok") && o.get("ok").getAsBoolean()) return Result.success();
            return Result.fail(o.has("error") ? o.get("error").getAsString() : "Ошибка сервера");
        } catch (Exception e) {
            return Result.fail("Некорректный ответ сервера");
        }
    }

    private static String httpGet(String url) {
        byte[] b = httpGetBytes(url);
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    private static byte[] httpGetBytes(String url) {
        try {
            HttpURLConnection conn = open(url, "GET", null);
            int code = conn.getResponseCode();
            byte[] out = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
            conn.disconnect();
            return code == 200 ? out : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Один универсальный запрос; возвращает тело даже при 4xx (там JSON с error). */
    private static String httpSend(String method, String url, byte[] payload,
                                   String contentType, String bearer) {
        try {
            HttpURLConnection conn = open(url, method, bearer);
            if (contentType != null) conn.setRequestProperty("Content-Type", contentType);
            if (payload != null) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(payload);
                os.close();
            }
            int code = conn.getResponseCode();
            byte[] out = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
            conn.disconnect();
            return out == null ? null : new String(out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static HttpURLConnection open(String url, String method, String bearer)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "CactusSkins-Mod");
        if (bearer != null) conn.setRequestProperty("Authorization", "Bearer " + bearer);
        return conn;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        if (in == null) return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        in.close();
        return bos.toByteArray();
    }

    private static void part(ByteArrayOutputStream bos, String boundary, String name, String value)
            throws IOException {
        bos.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void filePart(ByteArrayOutputStream bos, String boundary, String name,
                                 String filename, byte[] png) throws IOException {
        bos.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                + "\"; filename=\"" + filename + "\"\r\nContent-Type: image/png\r\n\r\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        bos.write(png);
        bos.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
    }

    private static JsonObject parse(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    private static String str(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }

    private static String msg(Exception e) {
        String m = e.getMessage();
        return m == null || m.isEmpty() ? e.getClass().getSimpleName() : m;
    }

    private static Path cache() {
        if (cacheDir == null) {
            cacheDir = FabricLoader.getInstance().getGameDir().resolve("cactusskins-cache");
            try {
                Files.createDirectories(cacheDir);
            } catch (Exception ignored) {
            }
        }
        return cacheDir;
    }
}
