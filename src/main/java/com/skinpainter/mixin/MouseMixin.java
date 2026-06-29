package com.skinpainter.mixin;

import com.skinpainter.client.SkinPainterClient;
import com.skinpainter.client.network.ClientNetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow @Final private MinecraftClient client;

    // ── Camera rotation ──────────────────────────────────────────

    /**
     * FIXED: Just cancel updateMouse() entirely when RMB not held.
     * No need to shadow cursorDeltaX/Y — cancelling the method is enough.
     */
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void skinPainter_updateMouse(CallbackInfo ci) {
        if (!SkinPainterClient.isInPaintMode) return;
        // Only allow camera rotation when right-mouse is held
        if (!SkinPainterClient.rightMouseHeld) {
            ci.cancel();
        }
    }

    // ── Mouse buttons ────────────────────────────────────────────

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void skinPainter_onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!SkinPainterClient.isInPaintMode) return;

        double sf = client.getWindow().getScaleFactor();
        double mx = client.mouse.getX() / sf;
        double my = client.mouse.getY() / sf;

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            SkinPainterClient.rightMouseHeld = (action == GLFW.GLFW_PRESS);
            ci.cancel();
            return;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW.GLFW_PRESS) {
                boolean consumed = SkinPainterClient.editorHud.onMouseClick(mx, my, button);
                if (!consumed) {
                    SkinPainterClient.isPainting = true;
                    SkinPainterClient.skinPainter.onStrokeBegin(mx, my);
                }
            } else if (action == GLFW.GLFW_RELEASE) {
                if (SkinPainterClient.isPainting) {
                    SkinPainterClient.isPainting = false;
                    SkinPainterClient.skinPainter.onStrokeEnd();
                    if (client.player != null) {
                        ClientNetworkHandler.sendSkinUpdate(client.player.getUuid());
                    }
                }
                SkinPainterClient.editorHud.onMouseRelease(mx, my, button);
            }
            ci.cancel();
            return;
        }

        ci.cancel(); // block all other buttons in paint mode
    }

    // ── Cursor tracking ──────────────────────────────────────────

    @Inject(method = "onCursorPos", at = @At("TAIL"))
    private void skinPainter_onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (!SkinPainterClient.isInPaintMode) return;

        double sf = client.getWindow().getScaleFactor();
        double mx = x / sf, my = y / sf;

        SkinPainterClient.editorHud.onMouseMove(mx, my);

        if (SkinPainterClient.isPainting) {
            SkinPainterClient.skinPainter.onStrokeDrag(mx, my);
        }
    }

    // ── Scroll → brush size ──────────────────────────────────────

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void skinPainter_onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!SkinPainterClient.isInPaintMode) return;
        int delta = vertical > 0 ? 1 : -1;
        SkinPainterClient.skinPainter.setBrushRadius(
                SkinPainterClient.skinPainter.getBrushRadius() + delta);
        ci.cancel();
    }
}
