package com.narutotoaddon.mangekyou;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Recuperar a visao: matar o Itachi (narutoto:itachi_npc) e reimplantar o olho dele. */
public final class EyeDrops {
    private EyeDrops() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getSource().getEntity() instanceof Player)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType());
        if (id == null || !id.getNamespace().equals("narutoto") || !id.getPath().equals("itachi_npc")) return;
        var en = e.getEntity();
        e.getDrops().add(new ItemEntity(en.level(), en.getX(), en.getY() + 0.5, en.getZ(),
                new ItemStack(ModItems.MANGEKYOU_ITACHI.get())));
    }
}
