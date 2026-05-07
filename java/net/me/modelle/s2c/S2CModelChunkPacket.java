package net.me.modelle.s2c;

import net.me.modelle.ModelBlock;
import net.me.modelle.util.ModelManager;
import net.me.modelle.util.ModelTransferManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Supplier;

public class S2CModelChunkPacket {
    private final String hash;
    private final int offset;
    private final int totalSize;
    private final byte[] data;

    public S2CModelChunkPacket(String hash, int offset, int totalSize, byte[] data) {
        this.hash = hash; this.offset = offset; this.totalSize = totalSize; this.data = data;
    }

    public static void encode(S2CModelChunkPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.hash); buf.writeInt(msg.offset); buf.writeInt(msg.totalSize); buf.writeByteArray(msg.data);
    }
    public static S2CModelChunkPacket decode(FriendlyByteBuf buf) {
        return new S2CModelChunkPacket(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readByteArray());
    }

    public static void handle(S2CModelChunkPacket msg, Supplier<NetworkEvent.Context> ct) {
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(() -> {
            // КЛИЕНТ принимает кусок от сервера
            if (msg.offset == 0) ModelTransferManager.startReception(msg.hash, msg.totalSize);

            if (ModelTransferManager.receiveChunk(msg.hash, msg.offset, msg.data)) {
                byte[] fullFile = ModelTransferManager.getCompleteData(msg.hash);
                try {
                    // Сохраняем во временный КЭШ
                    File target = new File(ModelManager.CACHE_DIR, msg.hash + ".mbm");
                    Files.write(target.toPath(), fullFile);
                    ModelBlock.ModelBlockEntity.REQUESTED_HASHES.remove(msg.hash);
                    System.out.println("[Modelle] Модель скачана с сервера: " + msg.hash);

                    // Модель теперь на диске, рендер подхватит её в следующем кадре!
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
        ctx.setPacketHandled(true);
    }
}
