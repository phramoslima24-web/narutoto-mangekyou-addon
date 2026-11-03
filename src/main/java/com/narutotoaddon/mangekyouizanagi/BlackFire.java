package com.narutotoaddon.mangekyouizanagi;

import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Coloca e apaga o fogo preto. */
public final class BlackFire {
    private BlackFire() {}

    public static boolean place(ServerLevel lv, BlockPos pos) {
        BlockState cur = lv.getBlockState(pos);
        if (!(cur.isAir() || cur.canBeReplaced()) || !cur.getFluidState().isEmpty()) return false;
        BlockState fire = ModBlocks.BLACK_FIRE.get().defaultBlockState();
        if (!fire.canSurvive(lv, pos)) return false;
        return lv.setBlockAndUpdate(pos, fire);
    }

    /** Planta fogo preto numa area quadrada ao redor de um ponto (acompanha o relevo). */
    public static int placeAround(ServerLevel lv, BlockPos center, int radius) {
        int n = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 1; dy >= -2; dy--) {
                    if (place(lv, center.offset(dx, dy, dz))) {
                        n++;
                        break;
                    }
                }
            }
        }
        return n;
    }

    /** Shift + Novo Amaterasu: apaga TODOS os fogos pretos (em todas as dimensoes carregadas). */
    public static void extinguishAll(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        int count = 0;
        for (ServerLevel lv : server.getAllLevels()) {
            BlackFireData data = BlackFireData.get(lv);
            for (BlockPos pos : data.snapshot()) {
                if (!lv.isLoaded(pos)) continue;
                if (lv.getBlockState(pos).is(ModBlocks.BLACK_FIRE.get())) {
                    lv.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    count++;
                } else {
                    data.remove(pos);
                }
            }
        }
        for (Map.Entry<UUID, AbilityScheduler.Timed> en : AbilityScheduler.BURNING.entrySet()) {
            ServerLevel lv = server.getLevel(en.getValue().dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            if (ent != null) ent.clearFire();
        }
        AbilityScheduler.BURNING.clear();
        player.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".extinguished", count), true);
    }
}
