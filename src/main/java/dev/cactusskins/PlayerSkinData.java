package dev.cactusskins;

import net.minecraft.util.Identifier;

/** Скачанный скин игрока: текстура, опциональный плащ и модель рук. */
public final class PlayerSkinData {
    public final Identifier texture;
    public final Identifier cape;
    public final boolean slim;

    public PlayerSkinData(Identifier texture, Identifier cape, boolean slim) {
        this.texture = texture;
        this.cape = cape;
        this.slim = slim;
    }
}
