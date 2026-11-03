package com.narutotoaddon.mangekyouizanagi;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MangekyouAddon.MODID);

    /** Fogo preto do Novo Amaterasu (so aparece pela habilidade, nao tem item). */
    public static final RegistryObject<Block> BLACK_FIRE = BLOCKS.register("black_fire",
            () -> new BlackFireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .noCollission()
                    .instabreak()
                    .noLootTable()
                    .lightLevel(state -> 10)
                    .sound(SoundType.WOOL)));

    private ModBlocks() {}
}
