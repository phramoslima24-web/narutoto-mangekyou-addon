package com.narutotoaddon.mangekyou;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MangekyouAddon.MODID)
public class MangekyouAddon {
    public static final String MODID = "narutoto_mangekyou";

    /** Aba criativa do mod Narutoto (a mesma onde ficam Sharingan/Itachi/Byakugan Eyeball). */
    private static final ResourceKey<CreativeModeTab> NARUTOTO_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("narutoto", "narutoto_tab"));

    public MangekyouAddon() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addToCreativeTab);

        MinecraftForge.EVENT_BUS.register(AbilityScheduler.class);
        MinecraftForge.EVENT_BUS.register(TimeStopHandler.class);
        MinecraftForge.EVENT_BUS.register(RewindHandler.class);
        MinecraftForge.EVENT_BUS.register(Implants.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEvents::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(NARUTOTO_TAB)) {
            event.accept(ModItems.MANGEKYOU_ITACHI);
            event.accept(ModItems.MANGEKYOU_SHISUI);
            event.accept(ModItems.MANGEKYOU_MADARA);
        }
    }
}
