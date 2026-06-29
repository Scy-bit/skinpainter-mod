package com.skinpainter.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skinpainter.client.painting.SkinPainter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;

import java.awt.Color;

/**
 * HSB color wheel + brightness slider widget.
 * FIXED: Correct RenderSystem/Tessellator calls for Fabric 1.20.1
 */
public class ColorWheelWidget {

    private int    x, y;
    private final int    radius;
    private final SkinPainter painter;

    private boolean draggingWheel  = false;
    private boolean draggingSlider = false;

    private float hue = 0f, sat = 1f, bri = 1f;

    private static final int SLIDER_H = 12;
    private static final int SLIDER_GAP = 8;

    public ColorWheelWidget(int x, int y, int radius, SkinPainter painter) {
        this.x = x; this.y = y; this.radius = radius; this.painter = painter;
    }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public int getTotalHeight() {
        return radius * 2 + SLIDER_GAP + SLIDER_H + 24;
    }

    // ── Render ───────────────────────────────────────────────────

    public void render(DrawContext ctx, float delta) {
        hue = painter.getHue(); sat = painter.getSaturation(); bri = painter.getBrightness();

        int cx = x + radius, cy = y + radius, diam = radius * 2;

        // Color wheel
        drawWheel(cx, cy);

        // Selection dot
        double ang = hue * Math.PI * 2;
        int dotX = cx + (int)(Math.cos(ang) * sat * radius);
        int dotY = cy + (int)(Math.sin(ang) * sat * radius);
        ctx.fill(dotX - 3, dotY - 3, dotX + 3, dotY + 3, 0xFFFFFFFF);
        ctx.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0xFF000000);

        // Brightness slider
        int slY = y + diam + SLIDER_GAP;
        drawSlider(slY, diam);

        // Slider handle
        int hndX = x + (int)(bri * (diam - 4)) + 2;
        ctx.fill(hndX - 3, slY - 2, hndX + 3, slY + SLIDER_H + 2, 0xFFFFFFFF);
        ctx.fill(hndX - 2, slY - 1, hndX + 2, slY + SLIDER_H + 1, 0xFF555555);

        // Color swatch
        int swY  = slY + SLIDER_H + 6;
        int swatch = 0xFF000000 | painter.getCurrentRGB();
        ctx.fill(x, swY, x + diam, swY + 14, 0xFF000000);
        ctx.fill(x + 1, swY + 1, x + diam - 1, swY + 13, swatch);

        // Hex label
        ctx.drawText(MinecraftClient.getInstance().textRenderer,
                Text.literal(String.format("§7#%06X", painter.getCurrentRGB())),
                x + diam + 5, swY + 3, 0xFFFFFF, false);
    }

    private void drawWheel(int cx, int cy) {
        Tessellator   tess = Tessellator.getInstance();
        BufferBuilder buf  = tess.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        buf.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        // Center = white
        buf.vertex(cx, cy, 0).color(255, 255, 255, 255).next();
        int segs = 64;
        for (int i = 0; i <= segs; i++) {
            float a = (float)(i * Math.PI * 2.0 / segs);
            float h = a / (float)(Math.PI * 2.0);
            Color c = Color.getHSBColor(h, 1f, bri);
            float vx = cx + (float)Math.cos(a) * radius;
            float vy = cy + (float)Math.sin(a) * radius;
            buf.vertex(vx, vy, 0).color(c.getRed(), c.getGreen(), c.getBlue(), 255).next();
        }
        tess.draw();
        RenderSystem.disableBlend();
    }

    private void drawSlider(int slY, int w) {
        Tessellator   tess = Tessellator.getInstance();
        BufferBuilder buf  = tess.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Color fc = Color.getHSBColor(hue, sat, 1f);
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buf.vertex(x,     slY + SLIDER_H, 0).color(0, 0, 0, 255).next();
        buf.vertex(x,     slY,            0).color(0, 0, 0, 255).next();
        buf.vertex(x + w, slY,            0).color(fc.getRed(), fc.getGreen(), fc.getBlue(), 255).next();
        buf.vertex(x + w, slY + SLIDER_H, 0).color(fc.getRed(), fc.getGreen(), fc.getBlue(), 255).next();
        tess.draw();
        RenderSystem.disableBlend();
    }

    // ── Input ────────────────────────────────────────────────────

    public boolean onMouseClick(double mx, double my, int button) {
        if (button != 0) return false;
        int diam = radius * 2;
        int slY  = y + diam + SLIDER_GAP;

        // Brightness slider
        if (mx >= x && mx <= x + diam && my >= slY - 2 && my <= slY + SLIDER_H + 2) {
            draggingSlider = true;
            applyBrightness(mx);
            return true;
        }
        // Wheel
        double ddx = mx - (x + radius), ddy = my - (y + radius);
        if (Math.sqrt(ddx * ddx + ddy * ddy) <= radius) {
            draggingWheel = true;
            applyWheel(mx, my);
            return true;
        }
        return false;
    }

    public void onMouseRelease(int button) {
        if (button == 0) { draggingWheel = false; draggingSlider = false; }
    }

    public void onMouseMove(double mx, double my) {
        if (draggingWheel)  applyWheel(mx, my);
        if (draggingSlider) applyBrightness(mx);
    }

    private void applyWheel(double mx, double my) {
        int cx = x + radius, cy = y + radius;
        double ddx = mx - cx, ddy = my - cy;
        hue = (float)(Math.atan2(ddy, ddx) / (Math.PI * 2.0));
        if (hue < 0) hue += 1f;
        sat = (float)Math.min(1.0, Math.sqrt(ddx*ddx + ddy*ddy) / radius);
        painter.setHSB(hue, sat, bri);
    }

    private void applyBrightness(double mx) {
        bri = (float)Math.max(0.0, Math.min(1.0, (mx - x) / (radius * 2)));
        painter.setHSB(hue, sat, bri);
    }
}
