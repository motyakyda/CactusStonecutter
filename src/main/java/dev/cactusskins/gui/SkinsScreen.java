package dev.cactusskins.gui;

import dev.cactusskins.CactusConfig;
import dev.cactusskins.CactusSkins;
import dev.cactusskins.SkinEntry;
import dev.cactusskins.SkinService;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
//? if >=1.20.2 {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.util.math.MatrixStack;
*///?}

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Единственный экран мода.
 *
 * Не подключён → одна кнопка «Подключить аккаунт» (код + браузер, токен юзер не видит).
 * Подключён    → библиотека скинов с превью: клик = надеть, ПКМ = удалить.
 */
public class SkinsScreen extends Screen {

    private static final int C_TITLE = 0xFFFFFF;
    private static final int C_DIM = 0x8B939F;
    private static final int C_HINT = 0x6E7681;
    private static final int C_OK = 0x6FDF97;
    private static final int C_ERR = 0xF07A7A;
    private static final int C_BUSY = 0xE8C86A;

    private static final int TILE = 56;
    private static final int TILE_GAP = 6;
    private static final int GRID_TOP = 78;
    private static final int GRID_COLS = 6;

    private final Screen parent;

    private final List<SkinEntry> skins = new ArrayList<>();
    private String activeId;

    private String status = "";
    private int statusColor = C_HINT;
    private boolean busy;

    // pairing
    private String pairCode;
    private String pairUrl;
    private int pollTicks;

    // dev mode
    private boolean devOpen;
    private TextFieldWidget apiField;

    public SkinsScreen(Screen parent) {
        super(GuiCompat.text("CactusSkins"));
        this.parent = parent;
    }

    // ───────────────────────────────────────────────────────────────── init

    @Override
    protected void init() {
        //? if >=1.17.1 {
        this.clearChildren();
        //?} else {
        /*this.children.clear();
        this.buttons.clear();
        *///?}

        if (!CactusConfig.isLinked()) {
            this.initOnboarding();
        } else {
            this.initLibrary();
        }

        // Режим разработчика — маленькая шестерёнка в углу, обычному юзеру не мешает.
        ButtonWidget dev = GuiCompat.button(this.width - 24, 6, 18, 18,
                GuiCompat.text("\u2699"), b -> {
                    this.devOpen = !this.devOpen;
                    CactusConfig.get().devMode = this.devOpen;
                    CactusConfig.save();
                    this.init();
                });
        GuiCompat.tooltip(dev, "Режим разработчика");
        this.add(dev);

        if (this.devOpen) this.initDevPanel();
    }

    private void initOnboarding() {
        int cx = this.width / 2;

        if (this.pairCode == null) {
            ButtonWidget connect = GuiCompat.button(cx - 90, 108, 180, 22,
                    GuiCompat.text("Подключить аккаунт"), b -> this.startPairing());
            GuiCompat.tooltip(connect, "Откроется браузер — подтверди ник, и всё");
            this.add(connect);
        } else {
            ButtonWidget again = GuiCompat.button(cx - 90, 148, 180, 20,
                    GuiCompat.text("Открыть браузер ещё раз"), b -> this.openUrl(this.pairUrl));
            this.add(again);

            ButtonWidget cancel = GuiCompat.button(cx - 90, 172, 180, 20,
                    GuiCompat.text("Отменить"), b -> {
                        this.pairCode = null;
                        this.pairUrl = null;
                        this.setStatus("", C_HINT);
                        this.init();
                    });
            this.add(cancel);
        }

        this.add(GuiCompat.button(cx - 90, this.height - 28, 180, 20,
                GuiCompat.text("Назад"), b -> this.goBack()));
    }

    private void initLibrary() {
        int cx = this.width / 2;

        ButtonWidget add = GuiCompat.button(cx - 152, this.height - 52, 150, 20,
                GuiCompat.text("Добавить скин"), b -> this.pickAndUpload());
        GuiCompat.tooltip(add, "PNG 64x64. Можно просто перетащить файл в окно.");
        this.add(add);

        ButtonWidget refresh = GuiCompat.button(cx + 2, this.height - 52, 150, 20,
                GuiCompat.text("Обновить"), b -> this.loadLibrary());
        this.add(refresh);

        ButtonWidget site = GuiCompat.button(cx - 152, this.height - 28, 100, 20,
                GuiCompat.text("Сайт"), b -> this.openUrl(CactusConfig.get().apiUrl + "/manage"));
        GuiCompat.tooltip(site, "Управлять скинами в браузере");
        this.add(site);

        ButtonWidget out = GuiCompat.button(cx - 48, this.height - 28, 100, 20,
                GuiCompat.text("Отключить"), b -> {
                    CactusConfig.unlink();
                    this.skins.clear();
                    this.activeId = null;
                    CactusSkins.resetSession();
                    this.setStatus("Аккаунт отключён", C_HINT);
                    this.init();
                });
        GuiCompat.tooltip(out, "Забыть аккаунт на этом компьютере");
        this.add(out);

        this.add(GuiCompat.button(cx + 56, this.height - 28, 96, 20,
                GuiCompat.text("Готово"), b -> this.goBack()));

        if (this.skins.isEmpty()) this.loadLibrary();
    }

    private void initDevPanel() {
        int y = this.height - 76;
        this.apiField = new TextFieldWidget(this.textRenderer,
                this.width / 2 - 152, y, 234, 18, GuiCompat.text("Backend"));
        this.apiField.setMaxLength(256);
        this.apiField.setText(CactusConfig.get().apiUrl);
        GuiCompat.placeholder(this.apiField, "https://...");
        this.add(this.apiField);

        this.add(GuiCompat.button(this.width / 2 + 86, y - 1, 66, 20,
                GuiCompat.text("Сохранить"), b -> {
                    String url = this.apiField.getText().trim();
                    while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
                    if (!url.startsWith("http")) {
                        this.setStatus("Адрес должен начинаться с http", C_ERR);
                        return;
                    }
                    CactusConfig.get().apiUrl = url;
                    CactusConfig.save();
                    SkinService.clearDiskCache();
                    CactusSkins.resetSession();
                    this.setStatus("Backend сохранён", C_OK);
                }));
    }

    // ──────────────────────────────────────────────────────────── pairing

    private void startPairing() {
        this.busy = true;
        this.setStatus("Получаю код...", C_BUSY);

        SkinService.pairStart().thenAccept(res -> ui(() -> {
            this.busy = false;
            if (!res.ok()) {
                this.setStatus("Не вышло: " + res.error, C_ERR);
                return;
            }
            this.pairCode = res.code;
            this.pairUrl = res.verifyUrl;
            this.pollTicks = 0;
            this.setStatus("Подтверди в браузере", C_BUSY);
            this.openUrl(res.verifyUrl);
            this.init();
        }));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pairCode == null || this.busy) return;

        // опрос раз в секунду
        if (++this.pollTicks % 20 != 0) return;

        SkinService.pairPoll(this.pairCode).thenAccept(p -> ui(() -> {
            if (p.linked) {
                CactusConfig.link(p.nick, p.token);
                this.pairCode = null;
                this.pairUrl = null;
                CactusSkins.resetSession();
                this.setStatus("Готово — привет, " + p.nick + "!", C_OK);
                this.init();
            } else if (!p.pending && p.error != null) {
                this.pairCode = null;
                this.pairUrl = null;
                this.setStatus(p.error, C_ERR);
                this.init();
            }
        }));
    }

    // ──────────────────────────────────────────────────────────── библиотека

    private void loadLibrary() {
        if (!CactusConfig.isLinked()) return;
        this.busy = true;
        this.setStatus("Загружаю библиотеку...", C_BUSY);

        SkinService.library().thenAccept(lib -> ui(() -> {
            this.busy = false;
            if (lib == null) {
                this.setStatus("Backend не отвечает", C_ERR);
                return;
            }
            this.skins.clear();
            this.skins.addAll(lib.skins);
            this.activeId = lib.activeId;
            this.setStatus(this.skins.isEmpty()
                    ? "Пусто — добавь первый скин"
                    : "Скинов: " + this.skins.size(), this.skins.isEmpty() ? C_HINT : C_OK);
        }));
    }

    private void activate(SkinEntry e) {
        this.busy = true;
        this.setStatus("Надеваю «" + e.name + "»...", C_BUSY);
        SkinService.activate(e.id).thenAccept(r -> ui(() -> {
            this.busy = false;
            if (r.ok()) {
                this.activeId = e.id;
                CactusSkins.resetSession();
                this.setStatus("Надет: " + e.name, C_OK);
            } else {
                this.setStatus("Ошибка: " + r.error(), C_ERR);
            }
        }));
    }

    private void deleteSkin(SkinEntry e) {
        this.busy = true;
        this.setStatus("Удаляю «" + e.name + "»...", C_BUSY);
        SkinService.delete(e.id).thenAccept(r -> ui(() -> {
            this.busy = false;
            if (r.ok()) {
                CactusSkins.resetSession();
                this.loadLibrary();
            } else {
                this.setStatus("Ошибка: " + r.error(), C_ERR);
            }
        }));
    }

    // ─────────────────────────────────────────────────────────── файлы

    private void pickAndUpload() {
        new Thread(() -> {
            String picked = openPngDialog();
            if (this.client == null) return;
            ui(() -> {
                if (picked == null) return;
                this.upload(new File(picked));
            });
        }, "cactusskins-picker").start();
    }

    private static String openPngDialog() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            return TinyFileDialogs.tinyfd_openFileDialog(
                    "Выбери скин (PNG 64x64)",
                    System.getProperty("user.home", "") + File.separator,
                    filters, "PNG (*.png)", false);
        } catch (Throwable t) {
            return null;
        }
    }

    private void upload(File f) {
        int[] wh = pngSize(f);
        if (wh == null) {
            this.setStatus("Это не PNG", C_ERR);
            return;
        }
        if (wh[0] != 64 || (wh[1] != 64 && wh[1] != 32)) {
            this.setStatus("Нужен скин 64x64, а тут " + wh[0] + "x" + wh[1], C_ERR);
            return;
        }

        String name = f.getName().replaceAll("(?i)\\.png$", "");
        if (name.length() > 24) name = name.substring(0, 24);
        boolean slim = wh[1] == 64 && name.toLowerCase().contains("slim");

        this.busy = true;
        this.setStatus("Загружаю «" + name + "»...", C_BUSY);
        SkinService.addSkin(name, slim, f, null).thenAccept(r -> ui(() -> {
            this.busy = false;
            if (r.ok()) {
                CactusSkins.resetSession();
                this.loadLibrary();
            } else {
                this.setStatus("Ошибка: " + r.error(), C_ERR);
            }
        }));
    }

    private static int[] pngSize(File f) {
        if (f == null || !f.isFile() || !f.getName().toLowerCase().endsWith(".png")) return null;
        try {
            BufferedImage img = ImageIO.read(f);
            return img == null ? null : new int[] { img.getWidth(), img.getHeight() };
        } catch (Exception e) {
            return null;
        }
    }

    private void handleDropped(List<Path> paths) {
        if (!CactusConfig.isLinked()) {
            this.setStatus("Сначала подключи аккаунт", C_ERR);
            return;
        }
        if (paths == null || paths.isEmpty()) return;
        for (Path p : paths) {
            File f = p.toFile();
            if (pngSize(f) != null) {
                this.upload(f);
                return;
            }
        }
        this.setStatus("Не нашёл подходящий PNG", C_ERR);
    }

    //? if >=1.21.10 {
    @Override
    public void onFilesDropped(List<Path> paths) {
        this.handleDropped(paths);
    }
    //?} else {
    /*@Override
    public void filesDragged(List<Path> paths) {
        this.handleDropped(paths);
    }
    *///?}

    // ───────────────────────────────────────────────────────────── клики

    /** Общая логика клика по плитке. true = клик поглощён. */
    private boolean handleTileClick(double mx, double my, int btn) {
        if (!CactusConfig.isLinked() || this.busy) return false;
        SkinEntry hit = this.tileAt(mx, my);
        if (hit == null) return false;

        if (btn == 0 && !hit.id.equals(this.activeId)) {
            this.activate(hit);
        } else if (btn == 1) {
            this.deleteSkin(hit);
        }
        return true;
    }

    //? if >=1.21.10 {
    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        if (this.handleTileClick(click.x(), click.y(), click.button())) return true;
        return super.mouseClicked(click, doubled);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (this.handleTileClick(mx, my, btn)) return true;
        return super.mouseClicked(mx, my, btn);
    }
    *///?}

    private SkinEntry tileAt(double mx, double my) {
        int n = this.skins.size();
        if (n == 0) return null;
        int cols = Math.min(GRID_COLS, n);
        int gridW = cols * TILE + (cols - 1) * TILE_GAP;
        int x0 = (this.width - gridW) / 2;

        for (int i = 0; i < n; i++) {
            int x = x0 + (i % cols) * (TILE + TILE_GAP);
            int y = GRID_TOP + (i / cols) * (TILE + TILE_GAP + 10);
            if (mx >= x && mx < x + TILE && my >= y && my < y + TILE) return this.skins.get(i);
        }
        return null;
    }

    // ───────────────────────────────────────────────────────────── render

    private void renderBody(GuiCompat.Canvas c, int mouseX, int mouseY) {
        int cx = this.width / 2;
        GuiCompat.drawCentered(c, this.textRenderer, "CactusSkins", cx, 16, C_TITLE);

        if (!CactusConfig.isLinked()) {
            this.renderOnboarding(c, cx);
        } else {
            this.renderLibrary(c, cx, mouseX, mouseY);
        }

        if (this.devOpen) {
            GuiCompat.drawText(c, this.textRenderer, "Backend / dev", cx - 152, this.height - 88, C_HINT);
            GuiCompat.drawText(c, this.textRenderer,
                    "токен: " + CactusConfig.maskedToken(), cx + 20, this.height - 88, C_HINT);
        }

        if (!this.status.isEmpty()) {
            GuiCompat.drawCentered(c, this.textRenderer, this.status, cx,
                    this.height - (this.devOpen ? 100 : 68), this.statusColor);
        }
    }

    private void renderOnboarding(GuiCompat.Canvas c, int cx) {
        GuiCompat.drawCentered(c, this.textRenderer,
                "Свой скин, который видят все игроки с модом", cx, 36, C_DIM);

        if (this.pairCode == null) {
            GuiCompat.drawCentered(c, this.textRenderer,
                    "Нажми кнопку — откроется браузер, укажешь ник.", cx, 72, C_HINT);
            GuiCompat.drawCentered(c, this.textRenderer,
                    "Пароли и токены вводить не нужно.", cx, 86, C_HINT);
        } else {
            GuiCompat.drawCentered(c, this.textRenderer, "Код подтверждения", cx, 74, C_DIM);

            String code = this.pairCode;
            int w = this.textRenderer.getWidth(code) * 2 + 24;
            int x = cx - w / 2;
            GuiCompat.fill(c, x, 88, x + w, 122, 0xC0111417);
            GuiCompat.border(c, x, 88, w, 34, 0xFF2C333C);
            GuiCompat.drawCentered(c, this.textRenderer, code, cx, 101, C_OK);

            GuiCompat.drawCentered(c, this.textRenderer,
                    "Подтверди на сайте — игра поймёт сама", cx, 130, C_HINT);
        }
    }

    private void renderLibrary(GuiCompat.Canvas c, int cx, int mouseX, int mouseY) {
        GuiCompat.drawCentered(c, this.textRenderer,
                "Аккаунт: " + CactusConfig.get().nick, cx, 34, C_DIM);
        GuiCompat.drawCentered(c, this.textRenderer,
                "ЛКМ — надеть, ПКМ — удалить, или перетащи PNG в окно", cx, 50, C_HINT);

        int n = this.skins.size();
        if (n == 0) {
            GuiCompat.drawCentered(c, this.textRenderer,
                    this.busy ? "Загружаю..." : "Библиотека пуста", cx, GRID_TOP + 20, C_HINT);
            return;
        }

        int cols = Math.min(GRID_COLS, n);
        int gridW = cols * TILE + (cols - 1) * TILE_GAP;
        int x0 = (this.width - gridW) / 2;

        for (int i = 0; i < n; i++) {
            SkinEntry e = this.skins.get(i);
            int x = x0 + (i % cols) * (TILE + TILE_GAP);
            int y = GRID_TOP + (i / cols) * (TILE + TILE_GAP + 10);

            boolean active = e.id.equals(this.activeId);
            boolean hover = mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE;

            GuiCompat.fill(c, x, y, x + TILE, y + TILE, 0xFF22262E);
            GuiCompat.border(c, x, y, TILE, TILE,
                    active ? 0xFF3F9E53 : (hover ? 0xFF4A525E : 0xFF2B3138));

            this.ensurePreview(e);
            if (e.preview != null) {
                GuiCompat.drawFace(c, e.preview, x + 12, y + 8, 32);
            } else {
                GuiCompat.drawCentered(c, this.textRenderer,
                        e.previewFailed ? "?" : "...", x + TILE / 2, y + TILE / 2 - 4, C_HINT);
            }

            String label = e.name.length() > 9 ? e.name.substring(0, 8) + "." : e.name;
            GuiCompat.drawCentered(c, this.textRenderer, label,
                    x + TILE / 2, y + TILE - 12, active ? C_OK : C_DIM);
            if (e.slim) {
                GuiCompat.drawText(c, this.textRenderer, "S", x + TILE - 9, y + 3, C_HINT);
            }
            if (e.hasCape) {
                GuiCompat.drawText(c, this.textRenderer, "\u25AC", x + 3, y + 3, C_HINT);
            }
        }
    }

    private void ensurePreview(SkinEntry e) {
        if (e.preview != null || e.previewRequested) return;
        e.previewRequested = true;
        SkinService.preview(CactusConfig.get().nick, e.id).thenAccept(tex -> ui(() -> {
            if (tex == null) e.previewFailed = true;
            else e.preview = tex;
        }));
    }

    //? if >=1.20.2 {
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.renderBody(new GuiCompat.Canvas(ctx), mouseX, mouseY);
    }
    //?} else {
    /*@Override
    public void render(MatrixStack m, int mouseX, int mouseY, float delta) {
        this.renderBackground(m);
        super.render(m, mouseX, mouseY, delta);
        this.renderBody(new GuiCompat.Canvas(m), mouseX, mouseY);
    }
    *///?}

    // ───────────────────────────────────────────────────────────── прочее

    private void add(ClickableWidget w) {
        //? if <1.17.1 {
        /*this.addButton(w);
        *///?} else {
        this.addDrawableChild(w);
        //?}
    }

    private void ui(Runnable r) {
        if (this.client != null) this.client.execute(r);
    }

    private void setStatus(String s, int color) {
        this.status = s;
        this.statusColor = color;
    }

    private void openUrl(String url) {
        if (url == null) return;
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception e) {
            this.setStatus("Открой вручную: " + url, C_HINT);
        }
    }

    private void goBack() {
        if (this.client != null) GuiCompat.openScreen(this.client, this.parent);
    }

    //? if >=1.18.2 {
    @Override
    public void close() {
        this.goBack();
    }
    //?} else {
    /*@Override
    public void onClose() {
        this.goBack();
    }
    *///?}
}
