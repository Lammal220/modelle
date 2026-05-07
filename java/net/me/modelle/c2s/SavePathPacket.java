package net.me.modelle.c2s;

import net.me.modelle.ModelBlock;
import net.me.modelle.s2c.S2CRequestModelPacket;
import net.me.modelle.util.ModelManager;
import net.me.modelle.util.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.util.function.Supplier;

public class SavePathPacket {
    private final BlockPos pos;
    private final String path;
    private final String hash;

    public SavePathPacket(BlockPos pos, String path, String hash) {
        this.pos = pos;
        this.path = path;
        this.hash = hash;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeUtf(this.path);
        buf.writeUtf(this.hash);
    }

    public static SavePathPacket decode(FriendlyByteBuf buf) {
        return new SavePathPacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readUtf()
        );
    }

    public static void handle(SavePathPacket msg, Supplier<NetworkEvent.Context> ct) {
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Level level = player.level();
            if (level.hasChunkAt(msg.pos) && level.getBlockState(msg.pos).is(ModelBlock.MODEL_BLOCK.get())) {
                ModelBlock.ModelBlockEntity entity = (ModelBlock.ModelBlockEntity) level.getBlockEntity(msg.pos);
                if (entity != null) {
                    if (!entity.isOwner(player) && !entity.allowEdit) return;

                    entity.path = msg.path;
                    entity.modelHash = msg.hash;
                    entity.setChanged();

                    if (msg.hash != null && !msg.hash.isEmpty()) {
                        File serverFile = new File(ModelManager.getModelStorageDir(level), msg.hash + ".mbm");

                        if (!serverFile.exists()) {
                            ModMessages.SIMPLE.sendTo(
                                    new S2CRequestModelPacket(msg.hash),
                                    player.connection.connection,
                                    NetworkDirection.PLAY_TO_CLIENT
                            );
                            System.out.println("[Modelle Server] Файл " + msg.hash + " не найден. Запрошена загрузка у игрока.");
                        }
                    }

                    BlockState state = level.getBlockState(msg.pos);
                    level.sendBlockUpdated(msg.pos, state, state, 3);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}