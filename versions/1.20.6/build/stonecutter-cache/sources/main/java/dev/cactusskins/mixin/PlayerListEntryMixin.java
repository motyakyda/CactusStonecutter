package dev.cactusskins.mixin;

import dev.cactusskins.CactusSkins;
import dev.cactusskins.PlayerSkinData;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.9 {
/*import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
*///?} else if >=1.20.2 {
import net.minecraft.client.util.SkinTextures;
//?}

/** Переопределение скина/плаща игрока через PlayerListEntry. */
@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    //? if >=1.21.9 {
    /*@Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void cactusskins$override(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerSkinData data = CactusSkins.OVERRIDES.get(
                CactusSkins.profileId(((PlayerListEntry) (Object) this).getProfile()));
        if (data == null) return;

        SkinTextures base = cir.getReturnValue();
        cir.setReturnValue(new SkinTextures(
                new CactusAsset(data.texture),
                data.cape != null ? new CactusAsset(data.cape) : base.cape(),
                base.elytra(),
                data.slim ? PlayerSkinType.SLIM : PlayerSkinType.WIDE,
                base.secure()));
    }

    /^* TextureAsset поверх текстуры, зарегистрированной в TextureManager. ^/
    record CactusAsset(Identifier id) implements AssetInfo.TextureAsset {
        @Override
        public Identifier texturePath() {
            return this.id;
        }
    }
    *///?} else if >=1.20.2 {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void cactusskins$override20(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerSkinData data = CactusSkins.OVERRIDES.get(
                CactusSkins.profileId(((PlayerListEntry) (Object) this).getProfile()));
        if (data == null) return;

        SkinTextures base = cir.getReturnValue();
        cir.setReturnValue(new SkinTextures(
                data.texture,
                null,
                data.cape != null ? data.cape : base.capeTexture(),
                base.elytraTexture(),
                data.slim ? SkinTextures.Model.SLIM : SkinTextures.Model.WIDE,
                base.secure()));
    }
    //?} else {
    /*@Inject(method = "getSkinTexture", at = @At("RETURN"), cancellable = true)
    private void cactusskins$skin(CallbackInfoReturnable<Identifier> cir) {
        PlayerSkinData data = CactusSkins.OVERRIDES.get(
                CactusSkins.profileId(((PlayerListEntry) (Object) this).getProfile()));
        if (data != null) cir.setReturnValue(data.texture);
    }

    @Inject(method = "getCapeTexture", at = @At("RETURN"), cancellable = true)
    private void cactusskins$cape(CallbackInfoReturnable<Identifier> cir) {
        PlayerSkinData data = CactusSkins.OVERRIDES.get(
                CactusSkins.profileId(((PlayerListEntry) (Object) this).getProfile()));
        if (data != null && data.cape != null) cir.setReturnValue(data.cape);
    }
    *///?}
}
