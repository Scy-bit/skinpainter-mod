package com.skinpainter.mixin;

import com.skinpainter.client.SkinPainterClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void skinPainter_onKey(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        if (!SkinPainterClient.isInPaintMode) return;

        boolean ctrl  = (mods & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean press = (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT);

        // ESC → exit paint mode
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            SkinPainterClient.exitPaintMode(client);
            ci.cancel();
            return;
        }

        // Ctrl+Z → Undo
        if (ctrl && key == GLFW.GLFW_KEY_Z && press) {
            SkinPainterClient.skinPainter.undo();
            ci.cancel();
            return;
        }

        // Ctrl+Y → Redo
        if (ctrl && key == GLFW.GLFW_KEY_Y && press) {
            SkinPainterClient.skinPainter.redo();
            ci.cancel();
            return;
        }

        // Block WASD + Space + Shift movement keys (FIXED: use if-chain, not switch expression)
        if (press && !ctrl) {
            if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_A ||
                key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_D ||
                key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_LEFT_SHIFT) {
                ci.cancel();
            }
        }
    }
}
