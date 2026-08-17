package dev.cactusskins.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
//? if >=1.20.2 {
import net.minecraft.client.gui.DrawContext;
//?} else {
/*import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
*///?}
//? if >=1.19.4 {
import net.minecraft.client.gui.tooltip.Tooltip;
//?}
//? if <1.19.0 {
/*import net.minecraft.text.LiteralText;
*///?}

/** Кросс-версионные обёртки над GUI API. Canvas — псевдоним холста версии. */
public final class GuiCompat {

    //? if >=1.20.2 {
    public static final class Canvas {
        public final DrawContext ctx;
        public Canvas(DrawContext ctx) { this.ctx = ctx; }
    }
    //?} else {
    /*public static final class Canvas {
        public final MatrixStack matrices;
        public Canvas(MatrixStack matrices) { this.matrices = matrices; }
    }
    *///?}

    private GuiCompat() {}

    public static Text text(String s) {
        //? if >=1.19.0 {
        return Text.literal(s);
        //?} else {
        /*return new LiteralText(s);
        *///?}
    }

    public static void drawText(Canvas c, TextRenderer tr, Text t, int x, int y, int color) {
        //? if >=1.20.2 {
        c.ctx.drawTextWithShadow(tr, t, x, y, color);
        //?} else {
        /*DrawableHelper.drawTextWithShadow(c.matrices, tr, t, x, y, color);
        *///?}
    }

    public static void drawText(Canvas c, TextRenderer tr, String s, int x, int y, int color) {
        drawText(c, tr, text(s), x, y, color);
    }

    public static void drawCentered(Canvas c, TextRenderer tr, String s, int cx, int y, int color) {
        Text t = text(s);
        drawText(c, tr, t, cx - tr.getWidth(t) / 2, y, color);
    }

    public static void fill(Canvas c, int x1, int y1, int x2, int y2, int argb) {
        //? if >=1.20.2 {
        c.ctx.fill(x1, y1, x2, y2, argb);
        //?} else {
        /*DrawableHelper.fill(c.matrices, x1, y1, x2, y2, argb);
        *///?}
    }

    /** Рамка толщиной 1px. */
    public static void border(Canvas c, int x, int y, int w, int h, int argb) {
        fill(c, x, y, x + w, y + 1, argb);
        fill(c, x, y + h - 1, x + w, y + h, argb);
        fill(c, x, y + 1, x + 1, y + h - 1, argb);
        fill(c, x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    /** Кусок текстуры 64x64: u,v — позиция в текстуре, rw,rh — размер куска. */
    private static void blit(Canvas c, Identifier tex, int x, int y, int w, int h,
                            float u, float v, int rw, int rh) {
        //? if >=1.21.5 {
        c.ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED,
                tex, x, y, u, v, w, h, rw, rh, 64, 64);
        //?} else if >=1.20.2 {
        /*c.ctx.drawTexture(tex, x, y, w, h, u, v, rw, rh, 64, 64);
        *///?} else {
        /*// bindTexture на 1.17+ сам делает RenderSystem.setShaderTexture, на 1.16 — прямой GL bind
        net.minecraft.client.MinecraftClient.getInstance().getTextureManager().bindTexture(tex);
        DrawableHelper.drawTexture(c.matrices, x, y, w, h, u, v, rw, rh, 64, 64);
        *///?}
    }

    /** Лицо скина (8x8 из 8,8) + слой шляпы (из 40,8), масштаб до size. */
    public static void drawFace(Canvas c, Identifier tex, int x, int y, int size) {
        blit(c, tex, x, y, size, size, 8F, 8F, 8, 8);
        blit(c, tex, x, y, size, size, 40F, 8F, 8, 8);
    }

    public static ButtonWidget button(int x, int y, int w, int h, Text msg,
                                      ButtonWidget.PressAction onPress) {
        //? if >=1.19.4 {
        return ButtonWidget.builder(msg, onPress).dimensions(x, y, w, h).build();
        //?} else {
        /*return new ButtonWidget(x, y, w, h, msg, onPress);
        *///?}
    }

    public static void tooltip(ClickableWidget w, String s) {
        //? if >=1.19.4 {
        w.setTooltip(Tooltip.of(text(s)));
        //?}
    }

    public static void placeholder(TextFieldWidget f, String s) {
        //? if >=1.19.4 {
        f.setPlaceholder(text(s));
        //?} else {
        /*f.setSuggestion(s);
        *///?}
    }

    public static void openScreen(net.minecraft.client.MinecraftClient mc, Screen s) {
        //? if >=1.17.1 {
        mc.setScreen(s);
        //?} else {
        /*mc.openScreen(s);
        *///?}
    }
}
