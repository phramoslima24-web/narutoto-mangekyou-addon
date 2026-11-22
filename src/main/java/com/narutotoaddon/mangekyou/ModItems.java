package com.narutotoaddon.mangekyou;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MangekyouAddon.MODID);

    public static final RegistryObject<Item> MANGEKYOU_ITACHI =
            ITEMS.register("mangekyou_itachi", () -> new ImplantItem(Implant.ITACHI));
    public static final RegistryObject<Item> MANGEKYOU_SHISUI =
            ITEMS.register("mangekyou_shisui", () -> new ImplantItem(Implant.SHISUI));
    public static final RegistryObject<Item> MANGEKYOU_MADARA =
            ITEMS.register("mangekyou_madara", () -> new ImplantItem(Implant.MADARA));
    public static final RegistryObject<Item> MANGEKYOU_SASUKE =
            ITEMS.register("mangekyou_sasuke", () -> new ImplantItem(Implant.SASUKE));
    public static final RegistryObject<Item> RINNE_SHARINGAN =
            ITEMS.register("rinne_sharingan", () -> new ImplantItem(Implant.RINNE));

    /** Item (implante) correspondente a cada olho. */
    public static Item itemFor(Implant i) {
        return switch (i) {
            case ITACHI -> MANGEKYOU_ITACHI.get();
            case SHISUI -> MANGEKYOU_SHISUI.get();
            case MADARA -> MANGEKYOU_MADARA.get();
            case SASUKE -> MANGEKYOU_SASUKE.get();
            case RINNE -> RINNE_SHARINGAN.get();
        };
    }

    private ModItems() {}
}
