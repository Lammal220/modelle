package net.me.modelle.util;

import net.me.modelle.Main;
import net.me.modelle.c2s.*;
import net.me.modelle.s2c.S2CModelChunkPacket;
import net.me.modelle.s2c.S2CRequestModelPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    public static final SimpleChannel SIMPLE = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Main.MODID,"channel"),
            ()->"1",
            "1"::equals,
            "1"::equals
    );

    private static int id = 0;

    public static void register(){
        SIMPLE.messageBuilder(
                UpdateModelValuesPacket.class,
                id++,
                NetworkDirection.PLAY_TO_SERVER
        ).encoder(UpdateModelValuesPacket::encode)
                .decoder(UpdateModelValuesPacket::decode)
                .consumerMainThread(UpdateModelValuesPacket::handle)
                .add();
        SIMPLE.messageBuilder(
                SavePathPacket.class,
                id++,
                NetworkDirection.PLAY_TO_SERVER
        ).encoder(SavePathPacket::encode)
                .decoder(SavePathPacket::decode)
                .consumerMainThread(SavePathPacket::handle)
                .add();
        SIMPLE.messageBuilder(
                UpdateModelPosPacket.class,
                id++,
                NetworkDirection.PLAY_TO_SERVER
                ).encoder(UpdateModelPosPacket::encode)
                .decoder(UpdateModelPosPacket::decode)
                .consumerMainThread(UpdateModelPosPacket::handle)
                .add();
        SIMPLE.messageBuilder(S2CRequestModelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CRequestModelPacket::encode)
                .decoder(S2CRequestModelPacket::decode)
                .consumerMainThread(S2CRequestModelPacket::handle)
                .add();

        SIMPLE.messageBuilder(C2SModelChunkPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SModelChunkPacket::encode)
                .decoder(C2SModelChunkPacket::decode)
                .consumerMainThread(C2SModelChunkPacket::handle)
                .add();
        SIMPLE.messageBuilder(C2SRequestDownloadPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestDownloadPacket::encode).decoder(C2SRequestDownloadPacket::decode)
                .consumerMainThread(C2SRequestDownloadPacket::handle).add();

        SIMPLE.messageBuilder(S2CModelChunkPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CModelChunkPacket::encode).decoder(S2CModelChunkPacket::decode)
                .consumerMainThread(S2CModelChunkPacket::handle).add();
        SIMPLE.messageBuilder(UpdateModelSettingsPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateModelSettingsPacket::encode).decoder(UpdateModelSettingsPacket::decode)
                .consumerMainThread(UpdateModelSettingsPacket::handle).add();
    }
}
