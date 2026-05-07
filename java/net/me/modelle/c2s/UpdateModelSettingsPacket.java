package net.me.modelle.c2s;

import net.me.modelle.ModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class UpdateModelSettingsPacket {
    private final BlockPos pos;
    private final boolean allowCopy, allowEdit, forceRender;

    public UpdateModelSettingsPacket(BlockPos pos, boolean allowCopy, boolean allowEdit, boolean forceRender) {
        this.pos = pos;
        this.allowCopy = allowCopy;
        this.allowEdit = allowEdit;
        this.forceRender = forceRender;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(allowCopy);
        buf.writeBoolean(allowEdit);
        buf.writeBoolean(forceRender);
    }

    public static UpdateModelSettingsPacket decode(FriendlyByteBuf buf) {
        return new UpdateModelSettingsPacket(buf.readBlockPos(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(UpdateModelSettingsPacket msg, Supplier<NetworkEvent.Context> ct) {
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return; // 🔒 Было пропущено!

            Level level = player.level();
            if (level.getBlockEntity(msg.pos) instanceof ModelBlock.ModelBlockEntity entity) {
                if (entity.isOwner(player)) {
                    entity.allowCopy = msg.allowCopy;
                    entity.allowEdit = msg.allowEdit;
                    entity.forceRender = msg.forceRender;
                    entity.setChanged();
                    level.sendBlockUpdated(msg.pos, level.getBlockState(msg.pos), level.getBlockState(msg.pos), 3);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}