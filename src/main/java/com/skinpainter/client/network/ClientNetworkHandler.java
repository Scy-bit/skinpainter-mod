package com.skinpainter.client.network;

import com.skinpainter.SkinPainterMod;
import com.skinpainter.client.render.SkinTextureManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;

public class ClientNetworkHandler {

    private static SkinTextureManager mgr;

    public static void register(SkinTextureManager m) {
        mgr = m;
        ClientPlayNetworking.registerGlobalReceiver(SkinPainterMod.SKIN_UPDATE_S2C,
            (client, handler, buf, responseSender) -> {
                UUID   uuid = buf.readUuid();
                byte[] data = buf.readByteArray();
                client.execute(() -> mgr.loadBytes(uuid, data));
            });
    }

    public static void sendSkinUpdate(UUID uuid) {
        if (mgr == null) return;
        byte[] data = mgr.exportBytes(uuid);
        if (data == null) return;
        if (data.length > 1_500_000) {
            SkinPainterMod.LOGGER.warn("[SkinPainter] Skin data terlalu besar: {} bytes", data.length);
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByteArray(data);
        ClientPlayNetworking.send(SkinPainterMod.SKIN_UPDATE_C2S, buf);
        SkinPainterMod.LOGGER.debug("[SkinPainter] Sent skin ({} bytes)", data.length);
    }
}
