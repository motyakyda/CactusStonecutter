package dev.cactusskins.mixin;

import dev.cactusskins.gui.SkinsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if <1.19.0 {
/*import net.minecraft.text.LiteralText;*/
//?}

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

    protected OptionsScreenMixin(Screen parent, GameOptions options) {
        //? if >=1.19.0 {
        super(Text.empty());
        //?} else {
        /*super(new LiteralText(""));*/
        //?}
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void cactusskins$addSettingsButton(CallbackInfo ci) {
        //? if >=1.19.4 {
        /*this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("CactusSkins"),
                        b -> {
                            if (this.client != null) {
                                this.client.setScreen(new SkinsScreen((OptionsScreen) (Object) this));
                            }
                        })
                .dimensions(4, this.height - 24, 100, 20)
                .build());
        *///?} else if >=1.19.0 {
        this.addDrawableChild(new ButtonWidget(4, this.height - 24, 100, 20,
                Text.literal("CactusSkins"), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new SkinsScreen((OptionsScreen) (Object) this));
                    }
                }));
        //?} else if >=1.17.1 {
        /*this.addDrawableChild(new ButtonWidget(4, this.height - 24, 100, 20,
                new LiteralText("CactusSkins"), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new SkinsScreen((OptionsScreen) (Object) this));
                    }
                }));*/
        //?} else {
        /*this.addButton(new ButtonWidget(4, this.height - 24, 100, 20,
                new LiteralText("CactusSkins"), b -> {
                    if (this.client != null) {
                        this.client.setScreen(new SkinsScreen((OptionsScreen) (Object) this));
                    }
                }));*/
        //?}
    }
}