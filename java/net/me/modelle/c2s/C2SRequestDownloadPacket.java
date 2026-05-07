package net.me.modelle.c2s;

import net.me.modelle.s2c.S2CModelChunkPacket;
import net.me.modelle.util.ModelManager;
import net.me.modelle.util.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.function.Supplier;

public class C2SRequestDownloadPacket {
    private final String hash;

    public C2SRequestDownloadPacket(String hash) { this.hash = hash; }

    public static void encode(C2SRequestDownloadPacket msg, FriendlyByteBuf buf) { buf.writeUtf(msg.hash); }
    public static C2SRequestDownloadPacket decode(FriendlyByteBuf buf) { return new C2SRequestDownloadPacket(buf.readUtf()); }

    public static void handle(C2SRequestDownloadPacket msg, Supplier<NetworkEvent.Context> ct) {
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Используем НОВОЕ название метода: getModelStorageDir
            File storage = ModelManager.getModelStorageDir(player.level());
            File file = new File(storage, msg.hash + ".mbm");

            if (file.exists()) {
                long size = file.length();
                if (size == 0 || size > ModelManager.MAX_MODEL_SIZE) {
                    System.err.println("[Modelle Server] Файл " + msg.hash + " имеет недопустимый размер: " + size);
                    return;
                }
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    int chunkSize = 20000;
                    for (int i = 0; i < bytes.length; i += chunkSize) {
                        int end = Math.min(bytes.length, i + chunkSize);
                        byte[] chunk = java.util.Arrays.copyOfRange(bytes, i, end);
                        ModMessages.SIMPLE.sendTo(
                                new S2CModelChunkPacket(msg.hash, i, bytes.length, chunk),
                                player.connection.connection,
                                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}