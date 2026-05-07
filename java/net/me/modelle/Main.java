package net.me.modelle;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Main.MODID)
public class Main {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "modelle";
    public Main(FMLJavaModLoadingContext ctx) {
        IEventBus bus = ctx.getModEventBus();
        ModelBlock.registerBlocks(bus);
    }


}
