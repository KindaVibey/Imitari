package com.vibey.imitari.block;

import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Interface that provides all CopyBlock logic.
 *
 * EASY MODE: Extend CopyBlockBase and just set mass multiplier
 * CUSTOM MODE: Implement this interface and delegate methods
 *
 * Each implementing class's getMassMultiplier() is automatically used
 * in all calculations via polymorphism.
 */
public interface ICopyBlock {

    /**
     * The ONLY method you MUST implement.
     * Return the mass multiplier for this block variant.
     * - 1.0f = full block
     * - 0.5f = slab
     * - 0.75f = stairs
     * - etc.
     */
    float getMassMultiplier();

    /**
     * Whether this block should use the CopyBlock dynamic model system.
     * Override to return false if you want custom rendering.
     */
    default boolean useDynamicModel() {
        return true;
    }

    // ==================== PHYSICS METHODS ====================

    /**
     * Explosion resistance based on copied block * mass multiplier.
     * Delegate to this from your Block's getExplosionResistance() override.
     */
    default float copyblock$getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseResistance = copiedState.getBlock().getExplosionResistance();
                return baseResistance * getMassMultiplier();
            }
        }
        return 3.0f;
    }

    /**
     * Mining speed based on copied block / mass multiplier.
     * Delegate to this from your Block's getDestroyProgress() override.
     */
    default float copyblock$getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseProgress = copiedState.getDestroyProgress(player, level, pos);
                return baseProgress / getMassMultiplier();
            }
        }
        return 1.0f;
    }

    /**
     * Sound type from copied block.
     * Delegate to this from your Block's getSoundType() override.
     */
    default SoundType copyblock$getSoundType(BlockState state, LevelReader level, BlockPos pos, Entity entity) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                return copiedState.getSoundType(level, pos, entity);
            }
        }
        return SoundType.WOOD;
    }

    // ==================== INTERACTION ====================

    /**
     * Handles right-click interaction for placing/removing copied blocks.
     * Delegate to this from your Block's use() override.
     */
    default InteractionResult copyblock$use(BlockState state, Level level, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopyBlockEntity copyBlockEntity)) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        BlockState currentCopied = copyBlockEntity.getCopiedBlock();

        // Shift + empty hand (creative only) = remove copied block
        if (player.isShiftKeyDown() && heldItem.isEmpty() && !currentCopied.isAir() && player.isCreative()) {
            copyBlockEntity.setCopiedBlock(Blocks.AIR.defaultBlockState());
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
            return InteractionResult.SUCCESS;
        }

        // Place block in CopyBlock
        if (heldItem.getItem() instanceof BlockItem blockItem) {
            Block targetBlock = blockItem.getBlock();
            if (targetBlock instanceof ICopyBlock) return InteractionResult.FAIL;

            BlockState targetState = targetBlock.defaultBlockState();
            if (!targetState.isCollisionShapeFullBlock(level, pos)) return InteractionResult.FAIL;

            if (!currentCopied.isAir()) {
                if (currentCopied.getBlock() == targetBlock) {
                    // Already has this block, just update rotation (no sound)
                    copyBlockEntity.setCopiedBlock(targetState);
                    return InteractionResult.SUCCESS;
                } else return InteractionResult.FAIL;
            } else {
                // First time placing - play sound and consume item
                if (!player.isCreative()) heldItem.shrink(1);
                copyBlockEntity.setCopiedBlock(targetState);
                copyblock$playBlockSound(level, pos, targetState);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    // ==================== CREATIVE PICK BLOCK ====================

    /**
     * Middle-click with shift gives the copied block.
     * Delegate to this from your Block's getCloneItemStack() override.
     */
    default ItemStack copyblock$getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player != null && mc.player.isShiftKeyDown()) {
                        return new ItemStack(copiedState.getBlock());
                    }
                } catch (Exception e) {
                    // Server side or error
                }
            }
        }
        return ItemStack.EMPTY;
    }

    // ==================== DROPS ====================

    /**
     * Drops the copied block when broken (not in creative).
     * Delegate to this from your Block's onRemove() override.
     */
    default void copyblock$onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CopyBlockEntity be) {
                BlockState copiedState = be.getCopiedBlock();
                if (!copiedState.isAir() && !be.wasRemovedByCreative()) {
                    ItemStack droppedItem = new ItemStack(copiedState.getBlock());
                    droppedItem.setTag(null);
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5,
                            pos.getY() + 0.5, pos.getZ() + 0.5, droppedItem));
                }
            }
        }
    }

    /**
     * Marks block as removed by creative to prevent drops.
     * Delegate to this from your Block's playerWillDestroy() override.
     */
    default void copyblock$playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopyBlockEntity copyBlockEntity) {
            if (player.isCreative()) copyBlockEntity.setRemovedByCreative(true);
        }
    }

    // ==================== PLACEMENT ====================

    /**
     * Ensures block starts empty when placed.
     * Delegate to this from your Block's setPlacedBy() override.
     */
    default void copyblock$setPlacedBy(Level level, BlockPos pos, BlockState state,
                                       @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                copyBE.setCopiedBlock(Blocks.AIR.defaultBlockState());
            }
        }
    }

    // ==================== HELPERS ====================

    /**
     * Plays the copied block's placement sound.
     * Called automatically by copyblock$use().
     */
    default void copyblock$playBlockSound(Level level, BlockPos pos, BlockState copiedState) {
        if (!level.isClientSide && !copiedState.isAir()) {
            SoundType soundType = copiedState.getSoundType(level, pos, null);
            level.playSound(null, pos, soundType.getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F);
        }
    }
}