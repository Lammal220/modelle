package net.me.modelle.c2s;

import net.me.modelle.ModelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateModelPosPacket {
    private BlockPos pos;
    private float posX;
    private float posY;
    private float posZ;

    public UpdateModelPosPacket(BlockPos pos, float posX, float posY, float posZ){
        this.pos = pos;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public void encode(FriendlyByteBuf buf){
        buf.writeBlockPos(this.pos);
        buf.writeFloat(this.posX);
        buf.writeFloat(this.posY);
        buf.writeFloat(this.posZ);
    }

    public static UpdateModelPosPacket decode(FriendlyByteBuf buf){
        return new UpdateModelPosPacket(
                buf.readBlockPos(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void handle(UpdateModelPosPacket msg, Supplier<NetworkEvent.Context> ct){
        NetworkEvent.Context ctx = ct.get();
        ctx.enqueueWork(()->{
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Level level = player.level();
            if(level.hasChunkAt(msg.pos) && level.getBlockState(msg.pos).is(ModelBlock.MODEL_BLOCK.get())){
                ModelBlock.ModelBlockEntity entity = (ModelBlock.ModelBlockEntity) level.getBlockEntity(msg.pos);
                if (entity == null) return;

                if (!entity.isOwner(player) && !entity.allowEdit) return;

                entity.posX = msg.posX;
                entity.posY = msg.posY;
                entity.posZ = msg.posZ;
                entity.setChanged();
                BlockState state = level.getBlockState(msg.pos);
                level.sendBlockUpdated(msg.pos, state, state, 3);
            }
        });
        ctx.setPacketHandled(true);
    }
}