package com.skinpainter.client;

import com.skinpainter.SkinPainterMod;
import com.skinpainter.client.gui.SkinEditorHud;
import com.skinpainter.client.network.ClientNetworkHandler;
import com.skinpainter.client.painting.SkinPainter;
import com.skinpainter.client.render.SkinTextureManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class SkinPainterClient implements ClientModInitializer {

    // Static state — accessed by Mixins
    public static KeyBinding        KEY_TOGGLE;
    public static SkinTextureManager textureManager;
    public static SkinPainter        skinPainter;
    public static SkinEditorHud      editorHud;

    public static boolean isInPaintMode  = false;
    public static boolean rightMouseHeld = false;
    public static boolean isPainting     = false;

    private static Perspective savedPerspective = Perspective.FIRST_PERSON;

    @Override
    public void onInitializeClient() {
        KEY_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skinpainter.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "category.skinpainter"
        ));

        textureManager = new SkinTextureManager();
        skinPainter    = new SkinPainter(textureManager);
        editorHud      = new SkinEditorHud(skinPainter);

        ClientNetworkHandler.register(textureManager);

        HudRenderCallback.EVENT.register((ctx, delta) -> {
            if (isInPaintMode) editorHud.render(ctx, delta);
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;
            while (KEY_TOGGLE.wasPressed()) {
                if (!isInPaintMode) enterPaintMode(mc);
            }
        });

        SkinPainterMod.LOGGER.info("[SkinPainter] Client ready. Tekan X untuk mulai melukis!");
    }

    // ── Mode transitions ─────────────────────────────────────────

    public static void enterPaintMode(MinecraftClient mc) {
        if (mc.player == null || isInPaintMode) return;

        isInPaintMode  = true;
        rightMouseHeld = false;
        isPainting     = false;

        savedPerspective = mc.options.getPerspective();
        mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        mc.mouse.unlockCursor();

        skinPainter.initForPlayer(mc.player.getUuid());

        SkinPainterMod.LOGGER.info("[SkinPainter] Mode edit aktif. Ctrl+Z=Undo, ESC=Keluar");
    }

    public static void exitPaintMode(MinecraftClient mc) {
        if (!isInPaintMode) return;

        isInPaintMode  = false;
        isPainting     = false;
        rightMouseHeld = false;

        mc.options.setPerspective(savedPerspective);
        if (mc.isWindowFocused()) mc.mouse.lockCursor();

        if (mc.player != null && textureManager.hasTexture(mc.player.getUuid())) {
            ClientNetworkHandler.sendSkinUpdate(mc.player.getUuid());
        }

        skinPainter.clearHistory();
        SkinPainterMod.LOGGER.info("[SkinPainter] Mode edit selesai.");
    }
}
