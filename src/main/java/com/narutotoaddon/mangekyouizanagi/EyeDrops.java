package com.narutotoaddon.mangekyouizanagi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Como recuperar a visao: matar o dono original do olho de novo. O Itachi NPC do Narutoto
 * (narutoto:itachi_npc) larga o Mangekyou do Itachi quando morre pelas maos de um jogador.
 * (Shisui/Madara nao tem NPC no Narutoto 0.4.1; os ids shisui_npc / madara_npc ja ficam prontos
 * caso o mod passe a ter, e os itens tambem estao na aba criativa.)
 */
public final class EyeDrops {
    private EyeDrops() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getSource().getEntity() instanceof Player)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType());
        if (id == null || !id.getNamespace().equals("narutoto")) return;
        ItemStack stack = switch (id.getPath()) {
            case "itachi_npc" -> new ItemStack(ModItems.MANGEKYOU_ITACHI.get());
            case "shisui_npc" -> new ItemStack(ModItems.MANGEKYOU_SHISUI.get());
            case "madara_npc" -> new ItemStack(ModItems.MANGEKYOU_MADARA.get());
            default -> ItemStack.EMPTY;
        };
        if (stack.isEmpty()) return;
        var en = e.getEntity();
        e.getDrops().add(new ItemEntity(en.level(), en.getX(), en.getY() + 0.5, en.getZ(), stack));
    }
}
