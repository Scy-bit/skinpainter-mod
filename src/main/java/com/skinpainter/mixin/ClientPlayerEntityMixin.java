package com.skinpainter.mixin;

import com.skinpainter.client.SkinPainterClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Locks player movement while in paint mode.
 * FIXED: Use setVelocity with current Y (gravity) and zero X/Z.
 *        Access input fields via the public Input class.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void skinPainter_lockMovement(CallbackInfo ci) {
        if (!SkinPainterClient.isInPaintMode) return;

        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;

        // Stop horizontal movement but preserve vertical (gravity)
        Vec3d vel = self.getVelocity();
        self.setVelocity(0.0, vel.y, 0.0);

        // Zero out input - these are public fields on net.minecraft.client.input.Input
        if (self.input != null) {
            self.input.movementForward  = 0f;
            self.input.movementSideways = 0f;
            self.input.jumping          = false;
            self.input.sneaking         = false;
        }
    }
}
