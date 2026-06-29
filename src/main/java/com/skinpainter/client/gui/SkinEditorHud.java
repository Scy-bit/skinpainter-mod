package com.skinpainter.client.gui;

import com.skinpainter.client.painting.PaintMode;
import com.skinpainter.client.painting.SkinPainter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class SkinEditorHud {

    private final SkinPainter      painter;
    private final ColorWheelWidget wheel;

    // Panel state (updated each frame)
    private int panelX, panelY, panelW, panelH;

    // Button hit areas
    private int modeBtnY;
    private int btnPaintX, btnPickX, btnEraseX;
    private int brushBtnY, brushMinusX, brushPlusX;
    private int undoRedoBtnY, btnUndoX, btnRedoX;

    private static final int R       = 60;   // wheel radius
    private static final int PAD     = 10;
    private static final int INNER   = 7;
    private static final int BTN_H   = 18;
    private static final int BTN_WM  = 68;  // mode button width
    private static final int BTN_WUR = 80;  // undo/redo button width

    public SkinEditorHud(SkinPainter painter) {
        this.painter = painter;
        this.wheel   = new ColorWheelWidget(0, 0, R, painter);
    }

    // ── Render ───────────────────────────────────────────────────

    public void render(DrawContext ctx, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int sh = mc.getWindow().getScaledHeight();

        int wheelH = wheel.getTotalHeight();
        panelW = R * 2 + INNER * 2 + 12;
        panelH = INNER + 14 + INNER   // title
               + wheelH  + INNER      // wheel
               + BTN_H   + INNER      // mode buttons
               + BTN_H   + INNER      // brush row
               + BTN_H   + INNER      // undo/redo
               + 10      + INNER;     // hint

        panelX = PAD;
        panelY = sh - panelH - PAD;

        // Background + border
        ctx.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xCC080810);
        ctx.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY - 1,          0xFF3366AA);
        ctx.fill(panelX - 2, panelY + panelH + 1, panelX + panelW + 2, panelY + panelH + 2, 0xFF3366AA);
        ctx.fill(panelX - 2, panelY - 2, panelX - 1, panelY + panelH + 2, 0xFF3366AA);
        ctx.fill(panelX + panelW + 1, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFF3366AA);

        int cur = panelY + INNER;

        // Title
        ctx.drawText(mc.textRenderer, Text.literal("§b✦ §eSkin Painter §b✦"),
                panelX + INNER, cur + 2, 0xFFFFFF, true);
        cur += 14 + INNER;

        // Color wheel
        wheel.setPosition(panelX + INNER, cur);
        wheel.render(ctx, delta);
        cur += wheelH + INNER;

        // ── Mode buttons ─────────────────────────────────────────
        modeBtnY   = cur;
        btnPaintX  = panelX + INNER;
        btnPickX   = btnPaintX + BTN_WM + 3;
        btnEraseX  = btnPickX  + BTN_WM + 3;

        drawModeBtn(ctx, mc, btnPaintX, cur, BTN_WM, BTN_H, "§f✏ Paint",
                painter.getMode() == PaintMode.PAINT,  0xFF143C14);
        drawModeBtn(ctx, mc, btnPickX,  cur, BTN_WM, BTN_H, "§f🎯 Pick",
                painter.getMode() == PaintMode.PICKER, 0xFF141448);
        drawModeBtn(ctx, mc, btnEraseX, cur, BTN_WM, BTN_H, "§f✦ Erase",
                painter.getMode() == PaintMode.ERASER, 0xFF3C1414);
        cur += BTN_H + INNER;

        // ── Brush row ─────────────────────────────────────────────
        brushBtnY  = cur;
        brushMinusX = panelX + INNER;
        brushPlusX  = brushMinusX + 20;

        drawSmallBtn(ctx, mc, brushMinusX, cur, 18, BTN_H, "§f-");
        drawSmallBtn(ctx, mc, brushPlusX,  cur, 18, BTN_H, "§f+");

        // Dot preview
        int dotX = brushPlusX + 22;
        int dots = painter.getBrushRadius();
        for (int i = 0; i < dots; i++)
            ctx.fill(dotX + i * 9, cur + 7, dotX + i * 9 + 7, cur + 14, 0xFF88CCFF);
        ctx.drawText(mc.textRenderer, Text.literal("§7r=" + dots + "px"),
                dotX + dots * 9 + 3, cur + 5, 0xAAAAAA, false);
        cur += BTN_H + INNER;

        // ── Undo/Redo row ─────────────────────────────────────────
        undoRedoBtnY = cur;
        btnUndoX     = panelX + INNER;
        btnRedoX     = btnUndoX + BTN_WUR + 4;

        drawActionBtn(ctx, mc, btnUndoX, cur, BTN_WUR, BTN_H,
                "§f↩ Undo §7(" + painter.undoCount() + ")",
                painter.canUndo(), 0xFF201414);
        drawActionBtn(ctx, mc, btnRedoX, cur, BTN_WUR, BTN_H,
                "§f↪ Redo §7(" + painter.redoCount() + ")",
                painter.canRedo(), 0xFF142014);
        cur += BTN_H + INNER;

        // Hint
        ctx.drawText(mc.textRenderer,
                Text.literal("§8[ESC]§7 Keluar  §8[RMB]§7 Putar kamera"),
                panelX + INNER, cur, 0x777777, false);
    }

    // ── Input ────────────────────────────────────────────────────

    public boolean onMouseClick(double mx, double my, int button) {
        if (!isOver(mx, my)) return false;

        if (wheel.onMouseClick(mx, my, button)) return true;
        if (button != 0) return true;

        if (hit(mx, my, btnPaintX,   modeBtnY,     BTN_WM,  BTN_H)) { painter.setMode(PaintMode.PAINT);  return true; }
        if (hit(mx, my, btnPickX,    modeBtnY,     BTN_WM,  BTN_H)) { painter.setMode(PaintMode.PICKER); return true; }
        if (hit(mx, my, btnEraseX,   modeBtnY,     BTN_WM,  BTN_H)) { painter.setMode(PaintMode.ERASER); return true; }
        if (hit(mx, my, brushMinusX, brushBtnY,    18,      BTN_H)) { painter.setBrushRadius(painter.getBrushRadius() - 1); return true; }
        if (hit(mx, my, brushPlusX,  brushBtnY,    18,      BTN_H)) { painter.setBrushRadius(painter.getBrushRadius() + 1); return true; }
        if (hit(mx, my, btnUndoX,    undoRedoBtnY, BTN_WUR, BTN_H)) { painter.undo(); return true; }
        if (hit(mx, my, btnRedoX,    undoRedoBtnY, BTN_WUR, BTN_H)) { painter.redo(); return true; }

        return true;
    }

    public void onMouseRelease(double mx, double my, int button) { wheel.onMouseRelease(button); }
    public void onMouseMove(double mx, double my)                 { wheel.onMouseMove(mx, my); }

    public boolean isOver(double mx, double my) {
        return mx >= panelX - 2 && mx <= panelX + panelW + 2 &&
               my >= panelY - 2 && my <= panelY + panelH + 2;
    }

    // ── Helpers ──────────────────────────────────────────────────

    private boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void drawModeBtn(DrawContext ctx, MinecraftClient mc,
                              int x, int y, int w, int h, String label,
                              boolean active, int activeBg) {
        ctx.fill(x, y, x+w, y+h, active ? 0xFF5599DD : 0xFF444444);
        ctx.fill(x+1, y+1, x+w-1, y+h-1, active ? activeBg : 0xFF1C1C1C);
        ctx.drawText(mc.textRenderer, Text.literal(label), x+4, y+5, 0xFFFFFF, false);
    }

    private void drawSmallBtn(DrawContext ctx, MinecraftClient mc,
                               int x, int y, int w, int h, String label) {
        ctx.fill(x, y, x+w, y+h, 0xFF555555);
        ctx.fill(x+1, y+1, x+w-1, y+h-1, 0xFF252525);
        ctx.drawText(mc.textRenderer, Text.literal(label), x+5, y+5, 0xFFFFFF, false);
    }

    private void drawActionBtn(DrawContext ctx, MinecraftClient mc,
                                int x, int y, int w, int h, String label,
                                boolean enabled, int enabledBg) {
        ctx.fill(x, y, x+w, y+h, enabled ? 0xFF666666 : 0xFF333333);
        ctx.fill(x+1, y+1, x+w-1, y+h-1, enabled ? enabledBg : 0xFF111111);
        ctx.drawText(mc.textRenderer, Text.literal(label), x+5, y+5,
                enabled ? 0xFFFFFF : 0xFF555555, false);
    }
}
