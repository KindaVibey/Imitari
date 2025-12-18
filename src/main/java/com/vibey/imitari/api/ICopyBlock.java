package com.vibey.imitari.api;

import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
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
 * Core interface for blocks that can copy and mimic other blocks.
 *
 * <p><b>Quick Start for Addon Developers:</b></p>
 * <pre>{@code
 * public class MyCustomCopyBlock extends Block implements ICopyBlock {
 *     public MyCustomCopyBlock(Properties props) {
 *         super(props);
 *     }
 *
 *     @Override
 *     public float getMassMultiplier() {
 *         return 0.5f; // Half mass (like a slab)
 *     }
 *
 *     // That's it! Everything else is handled automatically.
 * }
 * }</pre>
 *
 * <p><b>Features Provided:</b></p>
 * <ul>
 *   <li>Automatic texture/model copying from source blocks</li>
 *   <li>Dynamic physics (hardness, explosion resistance) based on copied block</li>
 *   <li>Dynamic tags - inherits all tags from copied block</li>
 *   <li>Dynamic sounds matching the copied block</li>
 *   <li>Valkyrien Skies 2 integration (if VS2 is loaded)</li>
 *   <li>Right-click to place blocks into the CopyBlock</li>
 *   <li>Creative middle-click to get copied block</li>
 *   <li>Automatic drop handling</li>
 * </ul>
 *
 * <p><b>Mass Multipliers:</b></p>
 * Common values:
 * <ul>
 *   <li>1.0f = Full block</li>
 *   <li>0.75f = Stairs</li>
 *   <li>0.5f = Slab</li>
 *   <li>0.125f = 1/8 layer</li>
 * </ul>
 *
 * @see ICopyBlockEntity
 * @see CopyBlockAPI
 */
public interface ICopyBlock {

    // ==================== REQUIRED METHOD ====================

    /**
     * The ONLY method you MUST implement.
     *
     * @return The mass multiplier for this block variant (0.0 to 1.0+)
     */
    float getMassMultiplier();

    // ==================== OPTIONAL OVERRIDES ====================

    /**
     * Whether this block should use Imitari's dynamic model system.
     * Override to return false if you want custom rendering.
     *
     * @return true to use dynamic models (default), false for custom rendering
     */
    default boolean useDynamicModel() {
        return true;
    }

    /**
     * Whether this block should inherit tags from copied blocks.
     * Override to return false if you don't want dynamic tag behavior.
     *
     * @return true to inherit tags (default), false to use only this block's tags
     */
    default boolean useDynamicTags() {
        return true;
    }

    /**
     * Whether this block should inherit physics from copied blocks.
     * Override to return false for custom physics behavior.
     *
     * @return true to inherit physics (default), false for custom physics
     */
    default boolean useDynamicPhysics() {
        return true;
    }

    /**
     * Whether placing blocks into this CopyBlock consumes items.
     * Override to return false for creative-like behavior in survival.
     *
     * @return true to consume items (default), false to allow free copying
     */
    default boolean consumesItemsOnPlace() {
        return true;
    }

    /**
     * Whether shift+empty hand in creative mode removes the copied block.
     *
     * @return true to allow removal (default), false to disable
     */
    default boolean allowCreativeRemoval() {
        return true;
    }

    // ==================== DELEGATION METHODS ====================
    // Call these from your Block class overrides for automatic behavior

    /**
     * Get explosion resistance based on copied block * mass multiplier.
     * Call this from your Block's {@code getExplosionResistance()} override.
     *
     * @return Explosion resistance value
     */
    default float copyblock$getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        if (!useDynamicPhysics()) {
            return 3.0f;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ICopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseResistance = copiedState.getBlock().getExplosionResistance();
                return baseResistance * getMassMultiplier();
            }
        }
        return 3.0f;
    }

    /**
     * Get mining speed based on copied block / mass multiplier.
     * Call this from your Block's {@code getDestroyProgress()} override.
     *
     * @return Destroy progress value (0.0 to 1.0+)
     */
    default float copyblock$getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!useDynamicPhysics()) {
            return 1.0f;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ICopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseProgress = copiedState.getDestroyProgress(player, level, pos);
                return baseProgress / getMassMultiplier();
            }
        }
        return 1.0f;
    }

    /**
     * Get sound type from copied block.
     * Call this from your Block's {@code getSoundType()} override.
     *
     * @return Sound type from copied block, or default wood sound
     */
    default SoundType copyblock$getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ICopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                return copiedState.getSoundType(level, pos, entity);
            }
        }
        return SoundType.WOOD;
    }

    /**
     * Handle right-click interaction for placing/removing copied blocks.
     * Call this from your Block's {@code use()} override.
     *
     * @return Interaction result
     */
    default InteractionResult copyblock$use(BlockState state, Level level, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ICopyBlockEntity copyBlockEntity)) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        BlockState currentCopied = copyBlockEntity.getCopiedBlock();

        // Shift + empty hand (creative only) = remove copied block
        if (allowCreativeRemoval() && player.isShiftKeyDown() && heldItem.isEmpty() &&
                !currentCopied.isAir() && player.isCreative()) {
            copyBlockEntity.setCopiedBlock(Blocks.AIR.defaultBlockState());
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());
            return InteractionResult.SUCCESS;
        }

        // Place block in CopyBlock
        if (heldItem.getItem() instanceof BlockItem blockItem) {
            Block targetBlock = blockItem.getBlock();

            // Don't allow copying another CopyBlock
            if (targetBlock instanceof ICopyBlock) return InteractionResult.FAIL;

            BlockState targetState = targetBlock.defaultBlockState();

            // Only allow full blocks (unless overridden)
            if (!allowPartialBlocks() && !targetState.isCollisionShapeFullBlock(level, pos)) {
                return InteractionResult.FAIL;
            }

            if (!currentCopied.isAir()) {
                // Already has a block - allow rotation if same type
                if (currentCopied.getBlock() == targetBlock) {
                    copyBlockEntity.setCopiedBlock(targetState);
                    return InteractionResult.SUCCESS;
                } else {
                    return InteractionResult.FAIL;
                }
            } else {
                // First time placing - consume item and play sound
                if (consumesItemsOnPlace() && !player.isCreative()) {
                    heldItem.shrink(1);
                }
                copyBlockEntity.setCopiedBlock(targetState);
                copyblock$playBlockSound(level, pos, targetState);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Whether to allow copying partial blocks (stairs, slabs, etc).
     * Override to return true if you want to allow any block type.
     *
     * @return false to only allow full blocks (default), true to allow any
     */
    default boolean allowPartialBlocks() {
        return false;
    }

    /**
     * Middle-click with shift gives the copied block.
     * Call this from your Block's {@code getCloneItemStack()} override.
     *
     * @return ItemStack of copied block, or empty if not applicable
     */
    default ItemStack copyblock$getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ICopyBlockEntity copyBE) {
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

    /**
     * Drop the copied block when broken (not in creative).
     * Call this from your Block's {@code onRemove()} override.
     *
     * IMPORTANT: This also notifies VS2 to subtract the correct mass!
     */
    default void copyblock$onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            // Block is being removed/replaced

            // CRITICAL: Notify VS2 BEFORE the block entity is removed!
            com.vibey.imitari.vs2.VS2CopyBlockIntegration.onBlockRemoved(level, pos, state, newState);

            // Then handle drops
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof ICopyBlockEntity be) {
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
    }

    /**
     * Mark block as removed by creative to prevent drops.
     * Call this from your Block's {@code playerWillDestroy()} override.
     */
    default void copyblock$playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ICopyBlockEntity copyBlockEntity) {
            if (player.isCreative()) {
                copyBlockEntity.setRemovedByCreative(true);
            }
        }
    }

    /**
     * Ensure block starts empty when placed.
     * Call this from your Block's {@code setPlacedBy()} override.
     */
    default void copyblock$setPlacedBy(Level level, BlockPos pos, BlockState state,
                                       @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ICopyBlockEntity copyBE) {
                copyBE.setCopiedBlock(Blocks.AIR.defaultBlockState());
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Play the copied block's placement sound.
     * Called automatically by {@code copyblock$use()}.
     */
    default void copyblock$playBlockSound(Level level, BlockPos pos, BlockState copiedState) {
        if (!level.isClientSide && !copiedState.isAir()) {
            SoundType soundType = copiedState.getSoundType(level, pos, null);
            level.playSound(null, pos, soundType.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F);
        }
    }
}