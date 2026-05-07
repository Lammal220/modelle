package net.me.modelle.s2c;

import net.me.modelle.c2s.C2SModelChunkPacket;
import net.me.modelle.util.ModMessages;
import net.me.modelle.util.ModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Supplier;

public class S2CRequestModelPacket {
    private final String hash;

    public S2CRequestModelPacket(String hash) {
        this.hash = hash;
    }

    public static void encode(S2CRequestModelPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hash);
    }

    public static S2CRequestModelPacket decode(FriendlyByteBuf buf) {
        return new S2CRequestModelPacket(buf.readUtf());
    }

    public static void handle(S2CRequestModelPacket msg, Supplier<NetworkEvent.Context> ct) {
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(() -> {
            // 1. На клиенте получаем доступ к текущему уровню
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return;

            // 2. Ищем файл в папке хранения этого мира
            // (В одиночной игре это папка мира, в мультиплеере для создателя это будет CACHE_DIR)
            File storage = ModelManager.getModelStorageDir(level);
            File file = new File(storage, msg.hash + ".mbm");

            // 3. Если по хэшу файл не найден в хранилище мира, попробуем поискать в кэше на всякий случай
            if (!file.exists()) {
                file = new File(ModelManager.CACHE_DIR, msg.hash + ".mbm");
            }

            if (file.exists()) {
                long size = file.length();
                if (size == 0 || size > ModelManager.MAX_MODEL_SIZE) {
                    System.err.println("[Modelle] Локальный файл " + msg.hash + " слишком большой или пустой");
                    return;
                }
                try {
                    byte[] allBytes = java.nio.file.Files.readAllBytes(file.toPath());
                    int chunkSize = 20000;
                    int totalSize = allBytes.length;
                    for (int offset = 0; offset < totalSize; offset += chunkSize) {
                        int end = Math.min(totalSize, offset + chunkSize);
                        byte[] chunk = java.util.Arrays.copyOfRange(allBytes, offset, end);
                        ModMessages.SIMPLE.sendToServer(new C2SModelChunkPacket(msg.hash, offset, totalSize, chunk));
                    }
                    System.out.println("[Modelle] Файл отправлен на сервер: " + msg.hash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
