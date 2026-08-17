package dev.cactusskins.mixin;

//? if <1.19.4 {
/*import dev.cactusskins.CactusSkins;
import dev.cactusskins.PlayerSkinData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void cactusskins$model(CallbackInfoReturnable<String> cir) {
        PlayerSkinData data = CactusSkins.OVERRIDES.get(
                ((AbstractClientPlayerEntity) (Object) this).getUuid());
        if (data != null) cir.setReturnValue(data.slim ? "slim" : "classic");
    }
}
*///?}
