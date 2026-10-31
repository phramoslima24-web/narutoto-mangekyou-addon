package com.narutotoaddon.mangekyou;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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

public class MangekyouItem extends Item {

    public enum Kind {
        ITACHI(Ability.SUSANOO, Ability.AMATERASU, Ability.TSUKUYOMI),
        SHISUI(Ability.SUSANOO, Ability.AMATERASU, Ability.KOTOAMATSUKAMI),
        MADARA(Ability.REWIND, Ability.TIME_STOP);

        public final Ability[] abilities;

        Kind(Ability... abilities) {
            this.abilities = abilities;
        }
    }

    private static final String TAG_SELECTED = "SelectedAbility";

    public final Kind kind;

    public MangekyouItem(Kind kind) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.kind = kind;
    }

    private int selected(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int sel = tag == null ? 0 : tag.getInt(TAG_SELECTED);
        return Math.floorMod(sel, kind.abilities.length);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            int sel = selected(stack);
            if (player.isShiftKeyDown()) {
                sel = (sel + 1) % kind.abilities.length;
                stack.getOrCreateTag().putInt(TAG_SELECTED, sel);
                sp.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".selected",
                        Component.translatable(kind.abilities[sel].langKey())), true);
            } else {
                AbilityHandler.use(sp, kind.abilities[sel]);
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
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".abilities").withStyle(ChatFormatting.GRAY));
        for (Ability a : kind.abilities) {
            tooltip.add(Component.literal(" - ").append(Component.translatable(a.langKey())).withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".selected",
                Component.translatable(kind.abilities[selected(stack)].langKey())).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip." + MangekyouAddon.MODID + ".help").withStyle(ChatFormatting.DARK_GRAY));
    }
}
