package com.vibey.imitari.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import com.vibey.imitari.blockentity.CopyBlockEntity;

/**
 * Interface for all blocks that can copy other blocks' appearance and properties.
 * Implement this interface in your custom blocks to make them compatible with
 * the Imitari rendering system.
 */
public interface ICopyBlock {

    /**
     * Get the mass multiplier for this block variant.
     * Used for VS2 physics calculations and hardness scaling.
     *
     * @return The mass multiplier (1.0 = full block, 0.5 = slab, 0.75 = stairs, etc.)
     */
    float getMassMultiplier();

    /**
     * Get the explosion resistance based on the copied block.
     * Override this if you need custom explosion resistance behavior.
     */
    default float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseResistance = copiedState.getBlock().getExplosionResistance();
                return baseResistance * getMassMultiplier();
            }
        }
        // Return a default value if no copied block
        return 3.0f;
    }

    /**
     * Get the destroy progress based on the copied block.
     * Override this if you need custom mining speed behavior.
     */
    default float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseProgress = copiedState.getDestroyProgress(player, level, pos);
                return baseProgress / getMassMultiplier();
            }
        }
        // Return a default value if no copied block
        return 1.0f;
    }

    default float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseProgress = copiedState.getDestroySpeed(level, pos);
                return baseProgress / getMassMultiplier();
            }
        }
        // Return a default value if no copied block
        return 1.0f;
    }

    /**
     * Check if this block should be registered for dynamic model rendering.
     * Override to return false if you want to handle rendering yourself.
     *
     * @return true if this block should use the CopyBlock model system
     */
    default boolean useDynamicModel() {
        return true;
    }
}