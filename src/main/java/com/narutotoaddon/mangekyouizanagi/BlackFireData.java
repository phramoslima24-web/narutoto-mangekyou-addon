package com.narutotoaddon.mangekyouizanagi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Lembra onde existe fogo preto em cada dimensao (para apagar todos de uma vez). */
public class BlackFireData extends SavedData {
    private static final String NAME = "mkiz_black_fire";
    private final Set<Long> positions = new HashSet<>();

    public static BlackFireData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(BlackFireData::load, BlackFireData::new, NAME);
    }

    public static BlackFireData load(CompoundTag tag) {
        BlackFireData d = new BlackFireData();
        for (long l : tag.getLongArray("pos")) d.positions.add(l);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("pos", positions.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    public void add(BlockPos pos) {
        if (positions.add(pos.asLong())) setDirty();
    }

    public void remove(BlockPos pos) {
        if (positions.remove(pos.asLong())) setDirty();
    }

    public List<BlockPos> snapshot() {
        List<BlockPos> out = new ArrayList<>(positions.size());
        for (long l : positions) out.add(BlockPos.of(l));
        return out;
    }
}
