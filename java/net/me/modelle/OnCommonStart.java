package net.me.modelle;

import net.me.modelle.util.ModMessages;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class OnCommonStart {
    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event){
        ModMessages.register();
    }
}
