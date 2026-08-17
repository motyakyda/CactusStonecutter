package dev.cactusskins.gui;

import dev.cactusskins.CactusConfig;
import dev.cactusskins.CactusSkins;
import dev.cactusskins.SkinService;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
//? if >=1.20.2 {
/*import net.minecraft.client.gui.DrawContext;
*///?} else {
import net.minecraft.client.util.math.MatrixStack;
//?}
//? if >=1.19.4 {
import net.minecraft.client.gui.tooltip.Tooltip;
//?}
//? if <1.19.0 {
/*import net.minecraft.text.LiteralText;
*///?}

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public class SkinsScreen extends Screen {

    private static final int COL_W = 155;
    private static final int GAP = 10;

    private static final int C_TITLE = 0xFFFFFF;
    private static final int C_LABEL = 0x9AA3AE;
    private static final int C_HINT = 0x6E7681;
    private static final int C_OK = 0x6FDF97;
    private static final int C_ERR = 0xF07A7A;
    private static final int C_BUSY = 0xE8C86A;

    private final Screen parent;

    private TextFieldWidget apiUrlField;
    private TextFieldWidget nickField;
    private TextFieldWidget tokenField;
    private ButtonWidget modelButton;
    private ButtonWidget skinButton;
    private ButtonWidget capeButton;
    private ButtonWidget uploadButton;

    private boolean slim = false;
    private boolean busy = false;
    private File skinFile;
    private File capeFile;

    private String status = "Перетащи PNG в окно или нажми «Выбрать скин»";
    private int statusColor = C_HINT;

    public SkinsScreen(Screen parent) {
        //? if >=1.19.0 {
        super(Text.literal("CactusSkins"));
        //?} else {
        /*super(new LiteralText("CactusSkins"));
        *///?}
        this.parent = parent;
    }

    // ------------------------------------------------------------------ layout

    private int leftX() {
        return this.width / 2 - COL_W - GAP / 2;
    }

    private int rightX() {
        return this.width / 2 + GAP / 2;
    }

    private int fullX() {
        return this.leftX();
    }

    private int fullW() {
        return COL_W * 2 + GAP;
    }

    @Override
    protected void init() {
        String self = this.client != null ? this.client.getSession().getUsername() : "";
        int left = this.leftX();
        int right = this.rightX();

        // --- Backend URL ---
        this.apiUrlField = new TextFieldWidget(
                this.textRenderer, this.fullX(), 48, this.fullW(), 18, label("Backend"));
        this.apiUrlField.setMaxLength(256);
        this.apiUrlField.setText(CactusConfig.get().apiUrl);
        placeholder(this.apiUrlField, "https://skins.example.com");
        this.add(this.apiUrlField);

        // --- Nick ---
        this.nickField = new TextFieldWidget(this.textRenderer, left, 84, COL_W, 18, label("Ник"));
        this.nickField.setMaxLength(16);
        this.nickField.setText(self);
        placeholder(this.nickField, self.isEmpty() ? "Твой ник" : self);
        this.add(this.nickField);

        // --- Token ---
        this.tokenField = new TextFieldWidget(this.textRenderer, right, 84, COL_W, 18, label("Токен"));
        this.tokenField.setMaxLength(64);
        this.tokenField.setText(CactusConfig.get().authToken);
        placeholder(this.tokenField, "заполнится сам");
        this.add(this.tokenField);

        // --- Model toggle ---
        this.modelButton = button(left, 112, COL_W, 20, this.modelLabel(), b -> {
            this.slim = !this.slim;
            b.setMessage(this.modelLabel());
        });
        tip(this.modelButton, "Classic — руки 4px (Steve)\nSlim — руки 3px (Alex)");
        this.add(this.modelButton);

        // --- Pick skin ---
        this.skinButton = button(right, 112, COL_W, 20, this.skinLabel(),
                b -> this.pickInto(true));
        tip(this.skinButton, "PNG 64x64 или 64x32.\nМожно просто перетащить файл в окно.");
        this.add(this.skinButton);

        // --- Pick cape ---
        this.capeButton = button(left, 136, COL_W, 20, this.capeLabel(),
                b -> this.pickInto(false));
        tip(this.capeButton, "PNG с соотношением 2:1 (например 64x32).\nМожно перетащить файл в окно.");
        this.add(this.capeButton);

        // --- Upload ---
        this.uploadButton = button(right, 136, COL_W, 20, label("Загрузить"),
                b -> this.startUpload());
        tip(this.uploadButton, "Отправить выбранные файлы на Backend");
        this.add(this.uploadButton);

        // --- Reset cache ---
        ButtonWidget reset = button(left, 160, COL_W, 20, label("Сбросить кэш"), b -> {
            SkinService.clearDiskCache();
            CactusSkins.resetSession();
            this.setStatus("Кэш скинов очищен — перезагрузятся сами", C_OK);
        });
        tip(reset, "Удаляет локально скачанные скины.\nПомогает, если скин «залип».");
        this.add(reset);

        // --- Open site ---
        ButtonWidget site = button(right, 160, COL_W, 20, label("Открыть сайт"), b -> this.openSite());
        tip(site, "Открыть Backend в браузере");
        this.add(site);

        // --- Clear selection ---
        ButtonWidget clear = button(left, 184, COL_W, 20, label("Очистить выбор"), b -> {
            this.skinFile = null;
            this.capeFile = null;
            this.refreshFileLabels();
            this.setStatus("Выбор сброшен", C_HINT);
        });
        this.add(clear);

        // --- Done ---
        ButtonWidget done = button(right, 184, COL_W, 20, label("Готово"), b -> {
            //? if >=1.18.2 {
            this.close();
            //?} else {
            /*this.onClose();
            *///?}
        });
        this.add(done);

        this.refreshFileLabels();
        this.updateUploadState();
    }

    // ------------------------------------------------------------ file picking

    private void pickInto(boolean isSkin) {
        String title = isSkin ? "Выбери скин (PNG)" : "Выбери плащ (PNG)";
        // Диалог блокирующий — уводим в отдельный поток, результат применяем на клиенте.
        new Thread(() -> {
            String picked = openPngDialog(title);
            if (this.client == null) return;
            this.client.execute(() -> {
                if (picked == null) {
                    this.setStatus("Файл не выбран", C_HINT);
                    return;
                }
                this.assign(new File(picked), isSkin);
            });
        }, "cactusskins-file-dialog").start();
    }

    /** Кроссплатформенный пикер: LWJGL TinyFileDialogs (Win/macOS/Linux), без AWT. */
    private static String openPngDialog(String title) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            return TinyFileDialogs.tinyfd_openFileDialog(
                    title, defaultDir(), filters, "PNG изображение (*.png)", false);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String defaultDir() {
        String home = System.getProperty("user.home", "");
        if (home.isEmpty()) return "";
        return home + File.separator;
    }

    /** Присваивает файл слоту, проверяя PNG и размеры. */
    private void assign(File f, boolean isSkin) {
        if (f == null || !f.isFile()) {
            this.setStatus("Файл не найден", C_ERR);
            return;
        }
        if (!f.getName().toLowerCase().endsWith(".png")) {
            this.setStatus("Нужен PNG-файл", C_ERR);
            return;
        }

        int[] wh = readSize(f);
        if (wh == null) {
            this.setStatus("Не удалось прочитать PNG", C_ERR);
            return;
        }
        int w = wh[0];
        int h = wh[1];

        if (isSkin) {
            if (!(w == 64 && (h == 64 || h == 32))) {
                this.setStatus("Скин должен быть 64x64 или 64x32, а тут " + w + "x" + h, C_ERR);
                return;
            }
            this.skinFile = f;
            this.setStatus("Скин готов: " + f.getName() + " (" + w + "x" + h + ")", C_OK);
        } else {
            if (w != h * 2) {
                this.setStatus("Плащ должен быть 2:1 (например 64x32), а тут " + w + "x" + h, C_ERR);
                return;
            }
            this.capeFile = f;
            this.setStatus("Плащ готов: " + f.getName() + " (" + w + "x" + h + ")", C_OK);
        }
        this.refreshFileLabels();
        this.updateUploadState();
    }

    private static int[] readSize(File f) {
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) return null;
            return new int[] { img.getWidth(), img.getHeight() };
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------- drag & drop

    private void handleDropped(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return;

        int taken = 0;
        for (Path p : paths) {
            File f = p.toFile();
            if (!f.isFile() || !f.getName().toLowerCase().endsWith(".png")) continue;

            int[] wh = readSize(f);
            if (wh == null) continue;
            int w = wh[0];
            int h = wh[1];

            // Авто-определение: 64x64 / 64x32 -> скин; иначе 2:1 -> плащ.
            if (w == 64 && (h == 64 || h == 32)) {
                this.skinFile = f;
                taken++;
            } else if (w == h * 2) {
                this.capeFile = f;
                taken++;
            }
        }

        this.refreshFileLabels();
        this.updateUploadState();

        if (taken == 0) {
            this.setStatus("Не подошло: нужен скин 64x64/64x32 или плащ 2:1", C_ERR);
        } else if (taken == 1) {
            this.setStatus("Принял файл — жми «Загрузить»", C_OK);
        } else {
            this.setStatus("Принял " + taken + " файла — жми «Загрузить»", C_OK);
        }
    }

    //? if >=1.21.10 {
    /*@Override
    public void onFilesDropped(List<Path> paths) {
        this.handleDropped(paths);
    }
    *///?} else {
    @Override
    public void filesDragged(List<Path> paths) {
        this.handleDropped(paths);
    }
    //?}

    // ------------------------------------------------------------------ actions

    private void openSite() {
        if (!CactusConfig.isConfigured()) {
            this.setStatus("Сначала укажи адрес Backend", C_ERR);
            return;
        }
        this.saveApiUrl();
        try {
            Util.getOperatingSystem().open(URI.create(CactusConfig.get().apiUrl));
        } catch (Exception e) {
            this.setStatus("Не смог открыть браузер", C_ERR);
        }
    }

    private void startUpload() {
        if (this.busy) return;

        String nick = this.nickField.getText().trim();
        if (!CactusConfig.isConfigured() && this.apiUrlField.getText().trim().isEmpty()) {
            this.setStatus("Укажи адрес Backend в верхнем поле", C_ERR);
            return;
        }
        this.saveApiUrl();
        if (!CactusConfig.isConfigured()) {
            this.setStatus("Адрес Backend выглядит неправильно", C_ERR);
            return;
        }
        if (nick.isEmpty()) {
            this.setStatus("Укажи ник", C_ERR);
            return;
        }
        if (this.skinFile == null && this.capeFile == null) {
            this.setStatus("Сначала выбери или перетащи файл", C_ERR);
            return;
        }

        final String fToken = this.tokenField.getText().trim();
        this.busy = true;
        this.updateUploadState();
        this.setStatus("Загружаю на сервер...", C_BUSY);

        SkinService.upload(nick, fToken, this.slim, this.skinFile, this.capeFile)
                .thenAccept(res -> {
                    if (this.client == null) return;
                    this.client.execute(() -> {
                        this.busy = false;
                        if (res.ok()) {
                            CactusConfig.get().authToken = res.token();
                            CactusConfig.save();
                            this.tokenField.setText(res.token());
                            this.setStatus(res.claimed()
                                    ? "Готово! Ник закреплён за тобой, токен сохранён"
                                    : "Готово! Скин обновлён", C_OK);
                            CactusSkins.resetSession();
                        } else {
                            this.setStatus("Ошибка: " + res.error(), C_ERR);
                        }
                        this.updateUploadState();
                    });
                });
    }

    private void saveApiUrl() {
        String url = this.apiUrlField.getText().trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.isEmpty() && !url.equals(CactusConfig.get().apiUrl)) {
            CactusConfig.get().apiUrl = url;
            CactusConfig.save();
            CactusSkins.resetSession();
        }
    }

    // ------------------------------------------------------------------- labels

    private void setStatus(String text, int color) {
        this.status = text;
        this.statusColor = color;
    }

    private void refreshFileLabels() {
        if (this.skinButton != null) this.skinButton.setMessage(this.skinLabel());
        if (this.capeButton != null) this.capeButton.setMessage(this.capeLabel());
    }

    private void updateUploadState() {
        if (this.uploadButton == null) return;
        boolean can = !this.busy && (this.skinFile != null || this.capeFile != null);
        this.uploadButton.active = can;
        this.uploadButton.setMessage(label(this.busy ? "Загружаю..." : "Загрузить"));
    }

    private Text modelLabel() {
        return label(this.slim ? "Руки: Slim (3px)" : "Руки: Classic (4px)");
    }

    private Text skinLabel() {
        return label(this.skinFile == null
                ? "Выбрать скин"
                : "Скин: " + shorten(this.skinFile.getName()));
    }

    private Text capeLabel() {
        return label(this.capeFile == null
                ? "Выбрать плащ"
                : "Плащ: " + shorten(this.capeFile.getName()));
    }

    private static String shorten(String name) {
        if (name.length() <= 14) return name;
        return name.substring(0, 12) + "..";
    }

    // -------------------------------------------------------------- MC helpers

    private static Text label(String s) {
        //? if >=1.19.0 {
        return Text.literal(s);
        //?} else {
        /*return new LiteralText(s);
        *///?}
    }

    private static ButtonWidget button(int x, int y, int w, int h, Text msg, ButtonWidget.PressAction onPress) {
        //? if >=1.19.4 {
        return ButtonWidget.builder(msg, onPress).dimensions(x, y, w, h).build();
        //?} else {
        /*return new ButtonWidget(x, y, w, h, msg, onPress);
        *///?}
    }

    private static void tip(ClickableWidget w, String text) {
        //? if >=1.19.4 {
        w.setTooltip(Tooltip.of(label(text)));
        //?}
    }

    private static void placeholder(TextFieldWidget f, String text) {
        //? if >=1.19.4 {
        f.setPlaceholder(label(text));
        //?} else {
        /*f.setSuggestion(text);
        *///?}
    }

    private void add(ClickableWidget w) {
        //? if <1.17.1 {
        /*this.addButton(w);
        *///?} else {
        this.addDrawableChild(w);
        //?}
    }

    private int centeredX(Text text) {
        return (this.width - this.textRenderer.getWidth(text)) / 2;
    }

    //? if >=1.18.2 {
    @Override
    public void close() {
        this.saveApiUrl();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
    //?} else {
    /*@Override
    public void onClose() {
        this.saveApiUrl();
        if (this.client != null) {
            this.client.openScreen(this.parent);
        }
    }
    *///?}

    // -------------------------------------------------------------------- render

    //? if >=1.20.2 {
    /*@Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        Text t = this.title;
        ctx.drawTextWithShadow(this.textRenderer, t, this.centeredX(t), 18, C_TITLE);
        ctx.drawTextWithShadow(this.textRenderer, label("Адрес Backend (API)"), this.fullX(), 36, C_LABEL);
        ctx.drawTextWithShadow(this.textRenderer, label("Ник"), this.leftX(), 72, C_LABEL);
        ctx.drawTextWithShadow(this.textRenderer, label("Токен (сохраняется сам)"), this.rightX(), 72, C_LABEL);
        Text hint = label("Можно перетащить PNG прямо в это окно");
        ctx.drawTextWithShadow(this.textRenderer, hint, this.centeredX(hint), 212, C_HINT);
        Text st = label(this.status);
        ctx.drawTextWithShadow(this.textRenderer, st, this.centeredX(st), 228, this.statusColor);
    }
    *///?} else {
    @Override
    public void render(MatrixStack m, int mouseX, int mouseY, float delta) {
        super.render(m, mouseX, mouseY, delta);
        Text t = this.title;
        drawTextWithShadow(m, this.textRenderer, t, this.centeredX(t), 18, C_TITLE);
        drawTextWithShadow(m, this.textRenderer, label("Адрес Backend (API)"), this.fullX(), 36, C_LABEL);
        drawTextWithShadow(m, this.textRenderer, label("Ник"), this.leftX(), 72, C_LABEL);
        drawTextWithShadow(m, this.textRenderer, label("Токен (сохраняется сам)"), this.rightX(), 72, C_LABEL);
        Text hint = label("Можно перетащить PNG прямо в это окно");
        drawTextWithShadow(m, this.textRenderer, hint, this.centeredX(hint), 212, C_HINT);
        Text st = label(this.status);
        drawTextWithShadow(m, this.textRenderer, st, this.centeredX(st), 228, this.statusColor);
    }
    //?}
}