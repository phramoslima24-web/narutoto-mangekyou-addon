package com.narutotoaddon.mangekyou;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fogo de verdade (queima e machuca como o fogo azul), so que preto. Nao se espalha, nao some sozinho
 * e nem a chuva apaga. Para apagar: Shift + usar o Novo Amaterasu.
 */
public class BlackFireBlock extends BaseFireBlock {
    public BlackFireBlock(Properties properties) {
        super(properties, 2.0F);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return this.canSurvive(state, level, pos) ? this.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return false;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel sl && level.getBlockState(pos).is(this)) {
            BlackFireData.get(sl).add(pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (level instanceof ServerLevel sl && !newState.is(this)) {
            BlackFireData.get(sl).remove(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
