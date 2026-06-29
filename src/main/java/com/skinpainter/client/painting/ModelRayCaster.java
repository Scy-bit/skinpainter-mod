package com.skinpainter.client.painting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Ray-casts from camera through mouse cursor → player model body parts.
 * Returns UV coordinates in the 64×64 skin texture.
 *
 * FIXED: Camera direction math, AABB intersection, face detection.
 */
public class ModelRayCaster {

    public static class BodyPart {
        public final String name;
        public final float minX, minY, minZ, maxX, maxY, maxZ;
        /** [front,back,right,left,top,bottom] each = {u0,v0,u1,v1} in skin texture */
        public final int[][] faceUV;

        BodyPart(String name,
                 float minX, float minY, float minZ,
                 float maxX, float maxY, float maxZ,
                 int[][] faceUV) {
            this.name = name;
            // 1 pixel = 1/16 block in model space
            this.minX = minX / 16f; this.maxX = maxX / 16f;
            this.minY = minY / 16f; this.maxY = maxY / 16f;
            this.minZ = minZ / 16f; this.maxZ = maxZ / 16f;
            this.faceUV = faceUV;
        }
    }

    // Skin UV layout for 64×64 Steve/Alex skin
    public static final BodyPart[] PARTS = {
        new BodyPart("Head",     -4,24,-4,  4,32, 4,
            new int[][]{{ 8, 8,16,16},{24, 8,32,16},{ 0, 8, 8,16},{16, 8,24,16},{ 8, 0,16, 8},{16, 0,24, 8}}),
        new BodyPart("Body",     -4,12,-2,  4,24, 2,
            new int[][]{{20,20,28,32},{32,20,40,32},{16,20,20,32},{28,20,32,32},{20,16,28,20},{28,16,36,20}}),
        new BodyPart("RightArm", -8,12,-2, -4,24, 2,
            new int[][]{{44,20,48,32},{52,20,56,32},{40,20,44,32},{48,20,52,32},{44,16,48,20},{48,16,52,20}}),
        new BodyPart("LeftArm",   4,12,-2,  8,24, 2,
            new int[][]{{36,52,40,64},{44,52,48,64},{32,52,36,64},{40,52,44,64},{36,48,40,52},{40,48,44,52}}),
        new BodyPart("RightLeg", -4, 0,-2,  0,12, 2,
            new int[][]{{ 4,20, 8,32},{12,20,16,32},{ 0,20, 4,32},{ 8,20,12,32},{ 4,16, 8,20},{ 8,16,12,20}}),
        new BodyPart("LeftLeg",   0, 0,-2,  4,12, 2,
            new int[][]{{20,52,24,64},{28,52,32,64},{16,52,20,64},{24,52,28,64},{20,48,24,52},{24,48,28,52}}),
    };

    public static class HitResult {
        public final BodyPart part;
        public final int u, v;
        public final double t;
        HitResult(BodyPart p, int u, int v, double t) {
            this.part = p; this.u = u; this.v = v; this.t = t;
        }
    }

    /**
     * Cast ray from camera through screen point (mouseX, mouseY).
     * Returns skin UV hit, or null if nothing was hit.
     */
    public static HitResult cast(double mouseX, double mouseY, PlayerEntity player) {
        MinecraftClient mc     = MinecraftClient.getInstance();
        Camera          camera = mc.gameRenderer.getCamera();

        int    sw   = mc.getWindow().getScaledWidth();
        int    sh   = mc.getWindow().getScaledHeight();
        double fov  = mc.options.getFov().getValue();    // degrees
        double tanY = Math.tan(Math.toRadians(fov * 0.5));
        double asp  = (double) sw / sh;

        // Normalized device coords (-1..1)
        double ndcX = (2.0 * mouseX / sw) - 1.0;
        double ndcY = 1.0 - (2.0 * mouseY / sh);

        // Ray direction in camera space (camera looks along +Z in camera space here)
        double rcX = ndcX * tanY * asp;
        double rcY = ndcY * tanY;
        double rcZ = 1.0;

        // Rotate to world space using camera yaw/pitch
        // Minecraft: yaw=0 → looking south (+Z), yaw increases counter-clockwise
        double pitch = Math.toRadians(camera.getPitch());
        double yaw   = Math.toRadians(camera.getYaw());

        double sinP = Math.sin(pitch), cosP = Math.cos(pitch);
        double sinY2 = Math.sin(yaw),  cosY = Math.cos(yaw);

        // Camera axes in world space:
        //   forward = (-sinY*cosP,  -sinP, cosY*cosP)
        //   right   = ( cosY,        0,    sinY)
        //   up      = (-sinY*sinP,   cosP, cosY*sinP)
        double fwdX = -sinY2 * cosP,   fwdY = -sinP,  fwdZ = cosY * cosP;
        double rgtX =  cosY,           rgtY =  0,     rgtZ = sinY2;
        double upX  = -sinY2 * sinP,   upY  =  cosP,  upZ  = cosY * sinP;

        double wdx = rcX * rgtX + rcY * upX + rcZ * fwdX;
        double wdy = rcX * rgtY + rcY * upY + rcZ * fwdY;
        double wdz = rcX * rgtZ + rcY * upZ + rcZ * fwdZ;
        double len = Math.sqrt(wdx*wdx + wdy*wdy + wdz*wdz);
        wdx /= len; wdy /= len; wdz /= len;

        // Ray origin = camera position
        Vec3d ro = camera.getPos();

        // Transform ray into player-local space
        Vec3d pp  = player.getPos();
        double ox = ro.x - pp.x;
        double oy = ro.y - pp.y;
        double oz = ro.z - pp.z;

        // Player body yaw (180° offset: body yaw 0 = model faces south = +Z)
        double pyaw    = Math.toRadians(player.getBodyYaw() + 180.0);
        double cpyaw   = Math.cos(pyaw);
        double spyaw   = Math.sin(pyaw);

        // Rotate origin and direction into model space
        double lox =  ox * cpyaw + oz * spyaw;
        double loy =  oy;
        double loz = -ox * spyaw + oz * cpyaw;

        double ldx =  wdx * cpyaw + wdz * spyaw;
        double ldy =  wdy;
        double ldz = -wdx * spyaw + wdz * cpyaw;

        // Test all body parts
        HitResult best = null;
        for (BodyPart p : PARTS) {
            double t = rayAABB(lox, loy, loz, ldx, ldy, ldz,
                               p.minX, p.minY, p.minZ, p.maxX, p.maxY, p.maxZ);
            if (t < 0.001) continue;
            if (best != null && t >= best.t) continue;

            double hx = lox + ldx * t;
            double hy = loy + ldy * t;
            double hz = loz + ldz * t;

            int[] uv = faceUV(hx, hy, hz, ldx, ldy, ldz, p);
            if (uv != null) best = new HitResult(p, uv[0], uv[1], t);
        }
        return best;
    }

    /** Slab method ray-AABB intersection. Returns t or -1. */
    private static double rayAABB(double ox, double oy, double oz,
                                   double dx, double dy, double dz,
                                   float x0, float y0, float z0,
                                   float x1, float y1, float z1) {
        double tNear = Double.NEGATIVE_INFINITY;
        double tFar  = Double.POSITIVE_INFINITY;

        double invX = 1.0 / dx;
        if (Math.abs(dx) > 1e-10) {
            double ta = (x0 - ox) * invX, tb = (x1 - ox) * invX;
            tNear = Math.max(tNear, Math.min(ta, tb));
            tFar  = Math.min(tFar,  Math.max(ta, tb));
        } else if (ox < x0 || ox > x1) return -1;

        double invY = 1.0 / dy;
        if (Math.abs(dy) > 1e-10) {
            double ta = (y0 - oy) * invY, tb = (y1 - oy) * invY;
            tNear = Math.max(tNear, Math.min(ta, tb));
            tFar  = Math.min(tFar,  Math.max(ta, tb));
        } else if (oy < y0 || oy > y1) return -1;

        double invZ = 1.0 / dz;
        if (Math.abs(dz) > 1e-10) {
            double ta = (z0 - oz) * invZ, tb = (z1 - oz) * invZ;
            tNear = Math.max(tNear, Math.min(ta, tb));
            tFar  = Math.min(tFar,  Math.max(ta, tb));
        } else if (oz < z0 || oz > z1) return -1;

        if (tFar < tNear || tFar < 0) return -1;
        return tNear < 0 ? tFar : tNear;
    }

    /** Determine which face was hit and return [u, v] in skin texture space. */
    private static int[] faceUV(double hx, double hy, double hz,
                                  double dx, double dy, double dz,
                                  BodyPart p) {
        double eps = 1e-3;
        int    fi;
        double lu, lv;

        if      (Math.abs(hz - p.maxZ) < eps && dz < 0) { fi=0; lu=(hx-p.minX)/(p.maxX-p.minX);     lv=1-(hy-p.minY)/(p.maxY-p.minY); }
        else if (Math.abs(hz - p.minZ) < eps && dz > 0) { fi=1; lu=1-(hx-p.minX)/(p.maxX-p.minX);   lv=1-(hy-p.minY)/(p.maxY-p.minY); }
        else if (Math.abs(hx - p.maxX) < eps && dx < 0) { fi=2; lu=1-(hz-p.minZ)/(p.maxZ-p.minZ);   lv=1-(hy-p.minY)/(p.maxY-p.minY); }
        else if (Math.abs(hx - p.minX) < eps && dx > 0) { fi=3; lu=(hz-p.minZ)/(p.maxZ-p.minZ);     lv=1-(hy-p.minY)/(p.maxY-p.minY); }
        else if (Math.abs(hy - p.maxY) < eps && dy < 0) { fi=4; lu=(hx-p.minX)/(p.maxX-p.minX);     lv=(hz-p.minZ)/(p.maxZ-p.minZ);   }
        else if (Math.abs(hy - p.minY) < eps && dy > 0) { fi=5; lu=(hx-p.minX)/(p.maxX-p.minX);     lv=1-(hz-p.minZ)/(p.maxZ-p.minZ); }
        else return null;

        int[] r = p.faceUV[fi];
        int w = r[2] - r[0], h = r[3] - r[1];
        int u = r[0] + (int)(lu * w);
        int v = r[1] + (int)(lv * h);
        u = Math.max(r[0], Math.min(r[2] - 1, u));
        v = Math.max(r[1], Math.min(r[3] - 1, v));
        return new int[]{u, v};
    }
}
