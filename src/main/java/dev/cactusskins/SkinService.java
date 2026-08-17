package dev.cactusskins;

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
import java.util.concurrent.CompletableFuture;

/** Общение с Backend: скачивание скинов и загрузка своих. */
public final class SkinService {

    public static final class UploadResult {
        private final boolean ok;
        private final String token;
        private final boolean claimed;
        private final String error;

        public UploadResult(boolean ok, String token, boolean claimed, String error) {
            this.ok = ok;
            this.token = token;
            this.claimed = claimed;
            this.error = error;
        }

        public boolean ok() { return ok; }
        public String token() { return token; }
        public boolean claimed() { return claimed; }
        public String error() { return error; }
    }

    private static Path cacheDir;

    private SkinService() {}

    public static void fetch(GameProfile profile) {
        if (!CactusConfig.isConfigured()) return;

        String api = CactusConfig.get().apiUrl;
        String nick = CactusSkins.profileName(profile).toLowerCase();
        CompletableFuture.runAsync(() -> {
            try {
                String infoJson = httpGet(api + "/api/info/" + nick);
                if (infoJson == null) return;
                JsonObject info = parse(infoJson);
                if (!info.has("found") || !info.get("found").getAsBoolean()) return;

                boolean slim = "slim".equalsIgnoreCase(info.get("model").getAsString());

                Identifier skin = registerTexture(nick, "skins/" + nick + ".png", CactusSkins.profileId(profile) + "/skin");
                if (skin == null) return;
                Identifier cape = registerTexture(nick, "capes/" + nick + ".png", CactusSkins.profileId(profile) + "/cape");

                CactusSkins.OVERRIDES.put(CactusSkins.profileId(profile), new PlayerSkinData(skin, cape, slim));
            } catch (Exception ignored) {
            }
        });
    }

    private static Identifier registerTexture(String nick, String remotePath, String localKey) {
        try {
            Path cached = cache().resolve(nick + "." + localKey.replace('/', '_') + ".png");
            byte[] bytes = httpGetBytes(CactusConfig.get().apiUrl + "/" + remotePath);
            if (bytes == null) {
                if (!Files.isRegularFile(cached)) return null;
                bytes = Files.readAllBytes(cached);
            } else {
                Files.write(cached, bytes);
            }

            NativeImage image = NativeImage.read(new ByteArrayInputStream(bytes));
            Identifier id = id(localKey);
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
        /*return new NativeImageBackedTexture(() -> "cactusskins", image);
        *///?} else {
        return new NativeImageBackedTexture(image);
        //?}
    }

    private static Identifier id(String key) {
        //? if >=1.21.0 {
        /*return Identifier.of("cactusskins", key);
        *///?} else {
        return new Identifier("cactusskins", key);
        //?}
    }

    private static JsonObject parse(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }

    public static void clearDiskCache() {
        Path dir = cache();
        File[] files = dir.toFile().listFiles();
        if (files == null) return;
        for (File f : files) {
            try { Files.deleteIfExists(f.toPath()); } catch (Exception ignored) { }
        }
    }

    public static CompletableFuture<UploadResult> upload(
            String nick, String token, boolean slim, File skin, File cape) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String boundary = "----CactusSkins" + Long.toHexString(System.nanoTime());
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                part(bos, boundary, "name", nick);
                if (token != null && !token.isEmpty()) {
                    part(bos, boundary, "token", token);
                }
                part(bos, boundary, "model", slim ? "slim" : "classic");
                if (skin != null) {
                    filePart(bos, boundary, "skin", "skin.png", Files.readAllBytes(skin.toPath()));
                }
                if (cape != null) {
                    filePart(bos, boundary, "cape", "cape.png", Files.readAllBytes(cape.toPath()));
                }
                bos.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.ISO_8859_1));

                HttpURLConnection conn = (HttpURLConnection)
                        new URL(CactusConfig.get().apiUrl + "/api/skin").openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                OutputStream os = conn.getOutputStream();
                os.write(bos.toByteArray());
                os.close();

                int code = conn.getResponseCode();
                InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                ByteArrayOutputStream resp = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while (in != null && (n = in.read(buf)) > 0) {
                    resp.write(buf, 0, n);
                }
                conn.disconnect();

                JsonObject obj = parse(resp.toString("UTF-8"));
                if (code == 200) {
                    return new UploadResult(true,
                            obj.get("token").getAsString(),
                            obj.get("claimed").getAsBoolean(),
                            null);
                }
                return new UploadResult(false, null, false,
                        obj.has("error") ? obj.get("error").getAsString() : ("HTTP " + code));
            } catch (Exception e) {
                return new UploadResult(false, null, false, String.valueOf(e.getMessage()));
            }
        });
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

    private static String httpGet(String url) {
        byte[] b = httpGetBytes(url);
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    private static byte[] httpGetBytes(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != 200) {
                conn.disconnect();
                return null;
            }
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream resp = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                resp.write(buf, 0, n);
            }
            conn.disconnect();
            return resp.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
