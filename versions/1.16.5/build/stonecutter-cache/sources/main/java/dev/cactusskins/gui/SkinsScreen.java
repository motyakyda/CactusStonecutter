package dev.cactusskins.gui;

import dev.cactusskins.CactusConfig;
import dev.cactusskins.CactusSkins;
import dev.cactusskins.SkinService;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
//? if >=1.20.2 {
/*import net.minecraft.client.gui.DrawContext;
*///?} else {
import net.minecraft.client.util.math.MatrixStack;
//?}
//? if <1.19.0 {
import net.minecraft.text.LiteralText;
//?}

import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.net.URI;

public class SkinsScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget apiUrlField;
    private TextFieldWidget nickField;
    private TextFieldWidget tokenField;
    private boolean slim = false;
    private String status = "Первый раз? Выбери файл скина и нажми «Загрузить»";
    private File skinFile;
    private File capeFile;

    public SkinsScreen(Screen parent) {
        //? if >=1.19.0 {
        /*super(Text.literal("Skins"));
        *///?} else {
        super(new LiteralText("Skins"));
        //?}
        this.parent = parent;
    }

    @Override
    protected void init() {
        String self = this.client != null ? this.client.getSession().getUsername() : "";

        this.apiUrlField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 65, 300, 18, label(""));
        this.apiUrlField.setMaxLength(256);
        this.apiUrlField.setText(CactusConfig.get().apiUrl);
        this.add(this.apiUrlField);

        this.nickField = new TextFieldWidget(this.textRenderer, this.width / 2 - 150, 100, 145, 18, label(""));
        this.nickField.setMaxLength(16);
        this.nickField.setText(self);
        this.add(this.nickField);

        this.tokenField = new TextFieldWidget(this.textRenderer, this.width / 2 + 5, 100, 145, 18, label(""));
        this.tokenField.setMaxLength(64);
        this.tokenField.setText(CactusConfig.get().authToken);
        this.add(this.tokenField);

        this.add(button(this.width / 2 - 150, 127, 145, 20, this.modelLabel(), b -> {
            this.slim = !this.slim;
            b.setMessage(this.modelLabel());
        }));

        this.add(button(this.width / 2 + 5, 127, 145, 20,
                label("Скин: не выбран"), b -> {
            File f = pickPng("Выбери файл скина (PNG 64x64)");
            if (f != null) {
                this.skinFile = f;
                b.setMessage(label("Скин: " + f.getName()));
            }
        }));

        this.add(button(this.width / 2 - 150, 152, 145, 20,
                label("Плащ: не выбран"), b -> {
            File f = pickPng("Выбери файл плаща (PNG 2:1)");
            if (f != null) {
                this.capeFile = f;
                b.setMessage(label("Плащ: " + f.getName()));
            }
        }));

        this.add(button(this.width / 2 + 5, 152, 145, 20,
                label("Загрузить"), b -> this.startUpload()));

        this.add(button(this.width / 2 - 150, 180, 145, 20,
                label("Сбросить кэш"), b -> {
            SkinService.clearDiskCache();
            CactusSkins.resetSession();
            this.status = "Кэш сброшен";
        }));

        this.add(button(this.width / 2 + 5, 180, 145, 20,
                label("Открыть сайт"), b -> openSite()));

        this.add(button(this.width / 2 - 75, this.height - 28, 150, 20,
                label("Готово"), b -> {
                    //? if >=1.18.2 {
                    /*this.close();
                    *///?} else {
                    this.onClose();
                    //?}
                }));
    }

    private static Text label(String s) {
        //? if >=1.19.0 {
        /*return Text.literal(s);
        *///?} else {
        return new LiteralText(s);
        //?}
    }

    private static ButtonWidget button(int x, int y, int w, int h, Text msg, ButtonWidget.PressAction onPress) {
        //? if >=1.19.4 {
        /*return ButtonWidget.builder(msg, onPress).dimensions(x, y, w, h).build();
        *///?} else {
        return new ButtonWidget(x, y, w, h, msg, onPress);
        //?}
    }

    private void add(ClickableWidget w) {
        //? if <1.17.1 {
        this.addButton(w);
        //?} else {
        /*this.addDrawableChild(w);
        *///?}
    }

    private int centeredX(Text text) {
        return (this.width - this.textRenderer.getWidth(text)) / 2;
    }

    private Text modelLabel() {
        return label("Руки: " + (this.slim ? "Slim (3px)" : "Classic (4px)"));
    }

    private void openSite() {
        if (!CactusConfig.isConfigured()) {
            this.status = "Сначала укажи адрес Backend";
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(CactusConfig.get().apiUrl));
        } catch (Exception ignored) {
        }
    }

    private void startUpload() {
        String nick = this.nickField.getText().trim();
        if (!CactusConfig.isConfigured()) {
            this.status = "Сначала укажи адрес Backend (API)";
            return;
        }
        if (nick.isEmpty()) {
            this.status = "Укажи ник";
            return;
        }
        if (this.skinFile == null && this.capeFile == null) {
            this.status = "Выбери файл скина или плаща";
            return;
        }

        this.saveApiUrl();
        final String fNick = nick;
        final String fToken = this.tokenField.getText().trim();
        this.status = "Загружаю...";

        SkinService.upload(fNick, fToken, this.slim, this.skinFile, this.capeFile)
                .thenAccept(res -> {
                    if (this.client != null) {
                        this.client.execute(() -> {
                            if (res.ok()) {
                                CactusConfig.get().authToken = res.token();
                                CactusConfig.save();
                                this.tokenField.setText(res.token());
                                this.status = res.claimed()
                                        ? "Готово! Ник закреплён, токен сохранён автоматически"
                                        : "Готово! Скин обновлён";
                                CactusSkins.resetSession();
                            } else {
                                this.status = "Ошибка: " + res.error();
                            }
                        });
                    }
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

    private static File pickPng(String title) {
        try {
            Frame frame = new Frame();
            FileDialog dialog = new FileDialog(frame, title, FileDialog.LOAD);
            dialog.setVisible(true);
            String file = dialog.getFile();
            String dir = dialog.getDirectory();
            frame.dispose();
            if (file == null) return null;
            File f = new File(dir, file);
            return f.getName().toLowerCase().endsWith(".png") ? f : null;
        } catch (Exception e) {
            return null;
        }
    }

    //? if >=1.18.2 {
    /*@Override
    public void close() {
        this.saveApiUrl();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
    *///?} else {
    @Override
    public void onClose() {
        this.saveApiUrl();
        if (this.client != null) {
            this.client.openScreen(this.parent);
        }
    }
    //?}

    //? if >=1.20.2 {
    /*@Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(this.textRenderer, this.title, this.centeredX(this.title), 20, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, label("Адрес Backend (API)"), this.width / 2 - 150, 54, 0xA9B0BA);
        context.drawTextWithShadow(this.textRenderer, label(this.status), this.centeredX(label(this.status)), 210, 0x7FE0A0);
    }
    *///?} else {
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawTextWithShadow(matrices, this.textRenderer, this.title, this.centeredX(this.title), 20, 0xFFFFFF);
        drawTextWithShadow(matrices, this.textRenderer, label("Адрес Backend (API)"), this.width / 2 - 150, 54, 0xA9B0BA);
        drawTextWithShadow(matrices, this.textRenderer, label(this.status), this.centeredX(label(this.status)), 210, 0x7FE0A0);
    }
    //?}
}