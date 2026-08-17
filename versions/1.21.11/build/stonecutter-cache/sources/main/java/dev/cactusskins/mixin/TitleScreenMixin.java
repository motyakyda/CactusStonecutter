package dev.cactusskins.mixin;

import dev.cactusskins.gui.SkinsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.19.0 {
/*import net.minecraft.text.LiteralText;
*///?}

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void cactusskins$addSkinsButton(CallbackInfo ci) {
        //? if >=1.19.4 {
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("Skins"),
                        b -> {
                            if (this.client != null) {
                                this.client.setScreen(new SkinsScreen((TitleScreen) (Object) this));
                            }
                        })
                .dimensions(this.width / 2 - 50, this.height - 24, 100, 20)
                .build());
        //?} else if >=1.19.0 {
        /*this.addDrawableChild(new ButtonWidget(this.width / 2 - 50, this.height - 24, 100, 20,
                Text.literal("Skins"), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new SkinsScreen((TitleScreen) (Object) this));
                    }
                }));
        *///?} else if >=1.17.1 {
        /*this.addDrawableChild(new ButtonWidget(this.width / 2 - 50, this.height - 24, 100, 20,
                new LiteralText("Skins"), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new SkinsScreen((TitleScreen) (Object) this));
                    }
                }));
        *///?} else {
        /*this.addButton(new ButtonWidget(this.width / 2 - 50, this.height - 24, 100, 20,
                new LiteralText("Skins"), b -> {
                    if (this.client != null) {
                        this.client.openScreen(new SkinsScreen((TitleScreen) (Object) this));
                    }
                }));
        *///?}
    }
}