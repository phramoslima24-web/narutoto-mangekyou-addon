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
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addToCreativeTab);

        MinecraftForge.EVENT_BUS.register(AbilityScheduler.class);
        MinecraftForge.EVENT_BUS.register(TimeStopHandler.class);
        MinecraftForge.EVENT_BUS.register(RewindHandler.class);
        MinecraftForge.EVENT_BUS.register(Implants.class);
        MinecraftForge.EVENT_BUS.register(IzanagiHandler.class);
        MinecraftForge.EVENT_BUS.register(IzanamiHandler.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEvents::register);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::register);
    }

    /** Palavras (no nome de registro) dos olhos ORIGINAIS do Narutoto que ficam escondidos: eles nao tem Izanagi/Izanami. */
    private static final String[] HIDE_WORDS = {"mangekyou", "mangekyo", "itachi", "shisui", "madara"};
    /** Nunca esconder o que tiver isso no nome (Rinnesharingan fica). */
    private static final String[] KEEP_WORDS = {"rinne", "rinnesharingan", "rinesharingan"};

    private static boolean hideOriginal(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getNamespace().equals("narutoto")) return false;
        String path = id.getPath().toLowerCase();
        for (String k : KEEP_WORDS) if (path.contains(k)) return false;
        for (String h : HIDE_WORDS) if (path.contains(h)) return true;
        return false;
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // tira os olhos originais repetidos (sem Izanagi/Izanami) de qualquer aba, inclusive da busca
        List<ItemStack> toRemove = new ArrayList<>();
        for (java.util.Map.Entry<ItemStack, CreativeModeTab.TabVisibility> en : event.getEntries()) {
            if (hideOriginal(en.getKey())) toRemove.add(en.getKey());
        }
        for (ItemStack s : toRemove) event.getEntries().remove(s);

        if (event.getTabKey().equals(NARUTOTO_TAB)) {
            event.accept(ModItems.MANGEKYOU_ITACHI);
            event.accept(ModItems.MANGEKYOU_SHISUI);
            event.accept(ModItems.MANGEKYOU_MADARA);
        }
    }
}
