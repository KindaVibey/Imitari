package com.vibey.copycraft.block;

import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;

/**
 * Base class for all CopyBlock variants (full block, stairs, slabs, etc.)
 * Each variant has a mass multiplier that scales properties
 */
public abstract class CopyBlockVariant extends CopyBlock {
    private final float massMultiplier;

    public CopyBlockVariant(Properties properties, float massMultiplier) {
        super(properties);
        this.massMultiplier = massMultiplier;
    }

    public float getMassMultiplier() {
        return massMultiplier;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                // Return scaled explosion resistance
                return copiedState.getBlock().getExplosionResistance() * massMultiplier;
            }
        }
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                // Get base destroy progress and adjust for mass multiplier
                // Lower multiplier = easier to break (higher progress per tick)
                float baseProgress = copiedState.getDestroyProgress(player, level, pos);
                return baseProgress / massMultiplier;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}