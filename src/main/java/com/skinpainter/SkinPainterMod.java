package com.skinpainter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SkinPainterMod implements ModInitializer {

    public static final String MOD_ID = "skinpainter";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    // Packet identifiers
    public static final Identifier SKIN_UPDATE_C2S = new Identifier(MOD_ID, "skin_update_c2s");
    public static final Identifier SKIN_UPDATE_S2C = new Identifier(MOD_ID, "skin_update_s2c");

    @Override
    public void onInitialize() {
        LOGGER.info("[SkinPainter] Server initialized.");

        // Receive painted skin from one client → relay to ALL players
        ServerPlayNetworking.registerGlobalReceiver(SKIN_UPDATE_C2S,
            (server, player, handler, buf, responseSender) -> {
                byte[] skinBytes  = buf.readByteArray();
                UUID   senderUuid = player.getUuid();

                server.execute(() -> {
                    for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                        PacketByteBuf out = PacketByteBufs.create();
                        out.writeUuid(senderUuid);
                        out.writeByteArray(skinBytes);
                        ServerPlayNetworking.send(other, SKIN_UPDATE_S2C, out);
                    }
                });
            });
    }
}
