package com.skinpainter.client.painting;

import com.skinpainter.SkinPainterMod;
import com.skinpainter.client.render.SkinTextureManager;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;
import java.util.UUID;

public class SkinPainter {

    private final SkinTextureManager mgr;
    private       UndoRedoManager    undo;

    private PaintMode mode        = PaintMode.PAINT;
    private float     hue         = 0f;
    private float     saturation  = 1f;
    private float     brightness  = 1f;
    private int       brushRadius = 1;
    private int       currentArgb = 0xFF_FF5500; // orange

    public SkinPainter(SkinTextureManager mgr) { this.mgr = mgr; }

    public void initForPlayer(UUID uuid) {
        undo = new UndoRedoManager(mgr, uuid);
    }

    // ── Actions ──────────────────────────────────────────────────

    public void onStrokeBegin(double mx, double my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mode == PaintMode.PICKER) { doPickColor(mc); return; }

        if (undo != null) undo.beginStroke();
        doStroke(mx, my, mc.player);
    }

    public void onStrokeDrag(double mx, double my) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mode == PaintMode.PICKER) return;
        doStroke(mx, my, mc.player);
    }

    public void onStrokeEnd() {
        if (undo != null) undo.commitStroke();
    }

    private void doStroke(double mx, double my, PlayerEntity player) {
        ModelRayCaster.HitResult hit = ModelRayCaster.cast(mx, my, player);
        if (hit == null) return;

        SkinPainterMod.LOGGER.debug("[SkinPainter] {} → {} UV({},{})",
                mode, hit.part.name, hit.u, hit.v);

        if (mode == PaintMode.ERASER) {
            mgr.erasePixel(player.getUuid(), hit.u, hit.v, brushRadius);
        } else {
            mgr.paintBrush(player.getUuid(), hit.u, hit.v, brushRadius, currentArgb);
        }
    }

    private void doPickColor(MinecraftClient mc) {
        if (mc.crosshairTarget == null || mc.world == null) return;
        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult bh  = (BlockHitResult) mc.crosshairTarget;
        BlockPos       pos = bh.getBlockPos();
        // FIXED: use MapColor.color (int rgb) directly
        MapColor mapColor = mc.world.getBlockState(pos).getMapColor(mc.world, pos);
        int rgb = mapColor.color;

        setCurrentColorRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        mode = PaintMode.PAINT;
        SkinPainterMod.LOGGER.info("[SkinPainter] Picked color #{}", String.format("%06X", rgb & 0xFFFFFF));
    }

    // ── Undo/Redo ────────────────────────────────────────────────

    public boolean undo()      { return undo != null && undo.undo(); }
    public boolean redo()      { return undo != null && undo.redo(); }
    public boolean canUndo()   { return undo != null && undo.canUndo(); }
    public boolean canRedo()   { return undo != null && undo.canRedo(); }
    public int     undoCount() { return undo != null ? undo.undoCount() : 0; }
    public int     redoCount() { return undo != null ? undo.redoCount() : 0; }
    public void    clearHistory() { if (undo != null) undo.clear(); }

    // ── Color ────────────────────────────────────────────────────

    public void setHSB(float h, float s, float b) {
        hue = h; saturation = s; brightness = b;
        Color c = Color.getHSBColor(h, s, b);
        currentArgb = 0xFF_000000 | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    public void setCurrentColorRGB(int r, int g, int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        setHSB(hsb[0], hsb[1], hsb[2]);
    }

    // ── Getters/setters ──────────────────────────────────────────

    public PaintMode getMode()          { return mode; }
    public void      setMode(PaintMode m) { this.mode = m; }

    public int   getCurrentArgb()       { return currentArgb; }
    public int   getCurrentRGB()        { return currentArgb & 0x00_FFFFFF; }
    public float getHue()               { return hue; }
    public float getSaturation()        { return saturation; }
    public float getBrightness()        { return brightness; }

    public int  getBrushRadius()        { return brushRadius; }
    public void setBrushRadius(int r)   { brushRadius = Math.max(1, Math.min(8, r)); }
}
