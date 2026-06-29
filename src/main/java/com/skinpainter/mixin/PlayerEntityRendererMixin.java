package com.skinpainter.mixin;

import com.skinpainter.client.SkinPainterClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FIXED: The correct Yarn 1.20.1 method is getTexture(), not getSkin().
 * getTexture() is the method from EntityRenderer that PlayerEntityRenderer overrides.
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {

    @Inject(method = "getTexture(Lnet/minecraft/client/network/AbstractClientPlayerEntity;)Lnet/minecraft/util/Identifier;",
            at = @At("RETURN"),
            cancellable = true)
    private void skinPainter_getTexture(AbstractClientPlayerEntity player,
                                         CallbackInfoReturnable<Identifier> cir) {
        if (SkinPainterClient.textureManager == null) return;

        Identifier custom = SkinPainterClient.textureManager.getTexture(player.getUuid());
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
