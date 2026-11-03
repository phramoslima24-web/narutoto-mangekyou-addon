package com.narutotoaddon.mangekyouizanagi;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Clicar com o botao direito faz o implante (e consome o item fora do criativo). */
public class ImplantItem extends Item {
    public final Implant implant;

    public ImplantItem(Implant implant) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.implant = implant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (Implants.apply(sp, implant) && !sp.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".implant").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".abilities").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" - ").append(Component.translatable("jutsu.mkiz.susanoo_mod"))
                .withStyle(ChatFormatting.DARK_RED));
        for (Ability a : implant.abilities) {
            tooltip.add(Component.literal(" - ").append(Component.translatable(a.jutsuKey()))
                    .withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".help").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".blind_cost").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".blind_cure").withStyle(ChatFormatting.GRAY));
    }
}
