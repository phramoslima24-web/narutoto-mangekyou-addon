package com.narutotoaddon.mangekyou;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Drops de Mangekyou dos NPCs do Narutoto (os drops originais deles nao sao tocados).
 * Entidades reais conferidas no jar: narutoto:sasuke_npc e narutoto:itachi_npc. Madara e Shisui NAO existem como
 * entidade no NarutoTo 0.4.1; seus drops so existem se voce os mapear em {@link MangekyouConfig#EXTRA_DROPS}.
 * Os itens sao implantes do proprio addon (ver {@link Implants}), tambem usados para recuperar a visao.
 */
public final class EyeDrops {
    private EyeDrops() {}

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getSource().getEntity() instanceof Player)) return;
        LivingEntity en = e.getEntity();
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(en.getType());
        if (id == null) return;
        int looting = Math.max(0, e.getLootingLevel());
        if (id.getNamespace().equals("narutoto")) {
            switch (id.getPath()) {
                case "itachi_npc" -> roll(e, en, Implant.ITACHI,
                        (float) (MangekyouConfig.ITACHI_CHANCE.get() + MangekyouConfig.ITACHI_LOOTING.get() * looting));
                case "sasuke_npc" -> roll(e, en, Implant.SASUKE,
                        (float) (MangekyouConfig.SASUKE_CHANCE.get() + MangekyouConfig.SASUKE_LOOTING.get() * looting));
                default -> { }
            }
        }
        for (MangekyouConfig.Extra x : MangekyouConfig.extras()) {
            if (x.entityId().equals(id.toString())) roll(e, en, x.eye(), x.chance() + x.looting() * looting);
        }
    }

    private static void roll(LivingDropsEvent e, LivingEntity en, Implant eye, float chance) {
        if (en.getRandom().nextFloat() >= chance) return;
        Item item = ModItems.itemFor(eye);
        if (item == null) return;
        e.getDrops().add(new ItemEntity(en.level(), en.getX(), en.getY() + 0.5, en.getZ(), new ItemStack(item)));
    }
}
