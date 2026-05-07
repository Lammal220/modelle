package net.me.modelle;

import net.me.modelle.util.ModelManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class OnClientStart {


    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(ModelBlock.MODEL_BLOCK_MENU.get(), ModelBlock.ModelBlockScreen::new);
        BlockEntityRenderers.register(
                ModelBlock.MODEL_BLOCK_ENTITY.get(),
                context -> new ModelBlock.ModelBlockRenderer()
        );

        Thread loader = new Thread(ModelManager::init);
        loader.setName("Modelle Model Loader");
        loader.start();
    }
}