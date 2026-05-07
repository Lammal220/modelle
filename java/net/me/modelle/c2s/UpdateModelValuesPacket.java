package net.me.modelle.c2s;

import net.me.modelle.ModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateModelValuesPacket {
    private BlockPos pos;
    private float raw;
    private float pitch;
    private float roll;
    private float scale;

    public UpdateModelValuesPacket(BlockPos pos, float raw, float pitch, float roll, float scale){
        this.pitch = pitch;
        this.raw = raw;
        this.pos = pos;
        this.roll = roll;
        this.scale = scale;
    }

    public void encode(FriendlyByteBuf buf){
        buf.writeBlockPos(this.pos);
        buf.writeFloat(this.raw);
        buf.writeFloat(this.pitch);
        buf.writeFloat(this.roll);
        buf.writeFloat(this.scale);
    }

    public static UpdateModelValuesPacket decode(FriendlyByteBuf buf){
        return new UpdateModelValuesPacket(
                buf.readBlockPos(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void handle(UpdateModelValuesPacket msg, Supplier<NetworkEvent.Context> ct){
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(()->{
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Level level = player.level();
            if(level.hasChunkAt(msg.pos) && level.getBlockState(msg.pos).is(ModelBlock.MODEL_BLOCK.get())){
                ModelBlock.ModelBlockEntity entity = (ModelBlock.ModelBlockEntity) level.getBlockEntity(msg.pos);
                if (entity == null) return;

                if (!entity.isOwner(player) && !entity.allowEdit) return;

                entity.raw = msg.raw;
                entity.pitch = msg.pitch;
                entity.roll = msg.roll;
                entity.scale = msg.scale;
                entity.setChanged();
                BlockState state = level.getBlockState(msg.pos);
                level.sendBlockUpdated(msg.pos, state, state, 3);
            }
        });
        ctx.setPacketHandled(true);
    }
}