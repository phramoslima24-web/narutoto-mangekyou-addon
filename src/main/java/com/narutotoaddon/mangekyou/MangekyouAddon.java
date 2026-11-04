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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

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
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addToCreativeTab);

        MinecraftForge.EVENT_BUS.register(AbilityScheduler.class);
        MinecraftForge.EVENT_BUS.register(TimeStopHandler.class);
        MinecraftForge.EVENT_BUS.register(RewindHandler.class);
        MinecraftForge.EVENT_BUS.register(Implants.class);
        MinecraftForge.EVENT_BUS.register(RinneAbilities.class);
        MinecraftForge.EVENT_BUS.register(IzanagiHandler.class);
        MinecraftForge.EVENT_BUS.register(IzanamiHandler.class);
        MinecraftForge.EVENT_BUS.register(EyeDrops.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEvents::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }

    /** Itens ORIGINAIS do Narutoto escondidos: o olho do Itachi original nao tem Izanagi/Izanami (os do addon tem). */
    private static final java.util.Set<String> HIDE_ORIGINAL = java.util.Set.of("itachi_eyeball");

    private static boolean hideOriginal(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().equals("narutoto")) return false;
        String path = id.getPath().toLowerCase();
        if (path.contains("rinne")) return false;
        return HIDE_ORIGINAL.contains(path);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        for (java.util.Map.Entry<ItemStack, CreativeModeTab.TabVisibility> en : event.getEntries()) {
            if (hideOriginal(en.getKey())) toRemove.add(en.getKey());
        }
        for (ItemStack s : toRemove) event.getEntries().remove(s);

        if (event.getTabKey().equals(NARUTOTO_TAB)) {
            event.accept(ModItems.MANGEKYOU_ITACHI);
            event.accept(ModItems.MANGEKYOU_SHISUI);
            event.accept(ModItems.MANGEKYOU_MADARA);
            event.accept(ModItems.RINNE_SHARINGAN);
        }
    }
}
