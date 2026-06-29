package com.skinpainter.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.skinpainter.SkinPainterMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages a custom 64×64 painted skin per player UUID.
 * FIXED: Correct NativeImage/DynamicTexture API for Fabric 1.20.1
 */
public class SkinTextureManager {

    private final Map<UUID, NativeImage>    images   = new HashMap<>();
    private final Map<UUID, DynamicTexture> textures = new HashMap<>();
    private final Map<UUID, Identifier>     ids      = new HashMap<>();

    // ── Paint ────────────────────────────────────────────────────

    /** Paint a circular brush at skin UV coordinate (u,v). */
    public void paintBrush(UUID uuid, int u, int v, int radius, int argb) {
        NativeImage img = getOrCreate(uuid);
        int r2 = radius * radius;
        for (int du = -radius; du <= radius; du++) {
            for (int dv = -radius; dv <= radius; dv++) {
                if (du * du + dv * dv <= r2) {
                    int pu = u + du, pv = v + dv;
                    if (pu >= 0 && pu < 64 && pv >= 0 && pv < 64) {
                        // NativeImage.setColor expects ABGR in 1.20.1
                        img.setColor(pu, pv, toAbgr(argb));
                    }
                }
            }
        }
        upload(uuid);
    }

    // ── Texture access ───────────────────────────────────────────

    public boolean     hasTexture(UUID uuid)  { return ids.containsKey(uuid); }
    public Identifier  getTexture(UUID uuid)  { return ids.get(uuid); }

    // ── Network I/O ──────────────────────────────────────────────

    /** Load skin from raw PNG bytes received over the network. */
    public void loadBytes(UUID uuid, byte[] png) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(png)) {
            NativeImage img = NativeImage.read(bais);
            if (img.getWidth() != 64 || img.getHeight() != 64) {
                SkinPainterMod.LOGGER.warn("[SkinPainter] Bad skin size {}x{}", img.getWidth(), img.getHeight());
                img.close();
                return;
            }
            NativeImage old = images.put(uuid, img);
            if (old != null) old.close();
            upload(uuid);
        } catch (IOException e) {
            SkinPainterMod.LOGGER.error("[SkinPainter] loadBytes failed", e);
        }
    }

    /** Export current skin image to PNG bytes for network transmission. */
    public byte[] exportBytes(UUID uuid) {
        NativeImage img = images.get(uuid);
        if (img == null) return null;
        try {
            return img.getBytes();
        } catch (IOException e) {
            SkinPainterMod.LOGGER.error("[SkinPainter] exportBytes failed", e);
            return null;
        }
    }

    // ── Internal ─────────────────────────────────────────────────

    public NativeImage getOrCreate(UUID uuid) {
        return images.computeIfAbsent(uuid, k -> new NativeImage(64, 64, true));
    }

    private void upload(UUID uuid) {
        NativeImage img = images.get(uuid);
        if (img == null) return;

        DynamicTexture tex = textures.get(uuid);
        if (tex == null) {
            // First time — create and register
            tex = new DynamicTexture(img);
            Identifier id = new Identifier(SkinPainterMod.MOD_ID,
                    "skin_" + uuid.toString().replace("-", ""));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            textures.put(uuid, tex);
            ids.put(uuid, id);
        } else {
            tex.upload();
        }
    }

    /** Convert ARGB (0xAARRGGBB) → ABGR (NativeImage format in 1.20.1). */
    private static int toAbgr(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        if (a == 0) a = 255; // fully transparent eraser: set to 0 explicitly below
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /** Paint transparent (erase). */
    public void erasePixel(UUID uuid, int u, int v, int radius) {
        NativeImage img = getOrCreate(uuid);
        int r2 = radius * radius;
        for (int du = -radius; du <= radius; du++) {
            for (int dv = -radius; dv <= radius; dv++) {
                if (du * du + dv * dv <= r2) {
                    int pu = u + du, pv = v + dv;
                    if (pu >= 0 && pu < 64 && pv >= 0 && pv < 64) {
                        img.setColor(pu, pv, 0x00000000); // fully transparent
                    }
                }
            }
        }
        upload(uuid);
    }

    public void removePlayer(UUID uuid) {
        NativeImage img = images.remove(uuid);
        if (img != null) img.close();
        DynamicTexture tex = textures.remove(uuid);
        if (tex != null) tex.close();
        ids.remove(uuid);
    }
}
