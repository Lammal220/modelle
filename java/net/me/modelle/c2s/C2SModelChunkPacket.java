package net.me.modelle.c2s;

import net.me.modelle.util.ModelManager;
import net.me.modelle.util.ModelTransferManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.io.File;
import java.nio.file.Files;
import java.util.function.Supplier;

public class C2SModelChunkPacket {
    private final String hash;
    private final int offset;
    private final int totalSize;
    private final byte[] data;

    public C2SModelChunkPacket(String hash, int offset, int totalSize, byte[] data) {
        this.hash = hash;
        this.offset = offset;
        this.totalSize = totalSize;
        this.data = data;
    }

    public static void encode(C2SModelChunkPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hash);
        buf.writeInt(msg.offset);
        buf.writeInt(msg.totalSize);
        buf.writeByteArray(msg.data);
    }

    public static C2SModelChunkPacket decode(FriendlyByteBuf buf) {
        return new C2SModelChunkPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readByteArray());
    }

    public static void handle(C2SModelChunkPacket msg, Supplier<NetworkEvent.Context> ct) {
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() == null) return;

            // ЛОГИКА НА СЕРВЕРЕ: Принимаем кусок
            if (msg.totalSize <= 0 || msg.totalSize > ModelManager.MAX_MODEL_SIZE) {
                System.err.println("[Modelle Server] Отклонён chunk: недопустимый totalSize");
                return;
            }

            if (msg.offset == 0) {
                ModelTransferManager.startReception(msg.hash, msg.totalSize);
            }

            boolean isComplete = ModelTransferManager.receiveChunk(msg.hash, msg.offset, msg.data);

            if (isComplete) {
                byte[] fullData = ModelTransferManager.getCompleteData(msg.hash);
                try {
                    // Используем НОВОЕ название метода: getModelStorageDir
                    File storage = ModelManager.getModelStorageDir(ctx.getSender().level());
                    File target = new File(storage, msg.hash + ".mbm");
                    java.nio.file.Files.write(target.toPath(), fullData);
                    System.out.println("[Modelle Server] Модель успешно загружена и сохранена: " + msg.hash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}