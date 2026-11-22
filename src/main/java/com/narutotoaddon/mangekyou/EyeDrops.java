package com.narutotoaddon.mangekyou;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Drops de Mangekyou dos NPCs do Narutoto (os drops originais deles nao sao tocados):
 * Itachi (narutoto:itachi_npc) sempre larga o olho dele; Sasuke (narutoto:sasuke_npc) pode largar o dele.
 * Os dois itens sao implantes do proprio addon (ver {@link Implants}), tambem usados para recuperar a visao.
 */
public final class EyeDrops {
    private EyeDrops() {}

    /** Chance base do Mangekyou do Sasuke ao derrota-lo (0.25 = 25%). Cada nivel de Pilhagem soma {@link #SASUKE_LOOTING_BONUS}. */
    public static final float SASUKE_DROP_CHANCE = 0.25F;
    public static final float SASUKE_LOOTING_BONUS = 0.05F;

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getSource().getEntity() instanceof Player)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType());
        if (id == null || !id.getNamespace().equals("narutoto")) return;
        var en = e.getEntity();
        switch (id.getPath()) {
            case "itachi_npc" -> e.getDrops().add(new ItemEntity(en.level(), en.getX(), en.getY() + 0.5, en.getZ(),
                    new ItemStack(ModItems.MANGEKYOU_ITACHI.get())));
            case "sasuke_npc" -> {
                float chance = SASUKE_DROP_CHANCE + SASUKE_LOOTING_BONUS * Math.max(0, e.getLootingLevel());
                if (en.getRandom().nextFloat() < chance) {
                    e.getDrops().add(new ItemEntity(en.level(), en.getX(), en.getY() + 0.5, en.getZ(),
                            new ItemStack(ModItems.MANGEKYOU_SASUKE.get())));
                }
            }
            default -> { }
        }
    }
}
