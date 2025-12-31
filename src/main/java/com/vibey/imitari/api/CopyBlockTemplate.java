package com.vibey.imitari.api;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * ============================================================================
 * ICOPYBLOCK TEMPLATE - READ THIS FIRST
 * ============================================================================
 *
 * This is a TEMPLATE showing the minimal implementation needed for ICopyBlock.
 * This does NOT extend Block - it's just an interface implementation guide.
 *
 * WHAT YOU NEED TO DO:
 * 1. Make your block extend Block (or any Block subclass you want)
 * 2. Implement EntityBlock
 * 3. Implement ICopyBlock
 * 4. Copy the methods below that you need
 *
 * EXAMPLE:
 * public class MyCustomBlock extends YourBaseBlock implements EntityBlock, ICopyBlock {
 *     // Copy methods from this template
 * }
 *
 * ============================================================================
 * REQUIRED: YOU MUST IMPLEMENT
 * ============================================================================
 */

/**
 * Example implementation - YOUR block should extend Block and implement these interfaces
 *
 * <p><b>NOTE:</b> This class is for documentation purposes only and is not used by Imitari.
 * It exists solely as a reference for addon developers. Do not instantiate or register this class.</p>
 */
@ApiStatus.NonExtendable
@SuppressWarnings({"unused", "NullableProblems"})
public class CopyBlockTemplate implements EntityBlock, ICopyBlock {

    // ========================================================================
    // STEP 1: SET YOUR MASS MULTIPLIER
    // ========================================================================
    // This is the ONLY required method for ICopyBlock
    // Common values:
    // - 1.0f = Full block
    // - 0.75f = Stairs
    // - 0.5f = Slab
    // - 0.25f = Quarter block
    // - 0.125f = 1/8 layer

    @Override
    public float getMassMultiplier() {
        return 1.0f; // Change this to your block's mass multiplier
    }

    // ========================================================================
    // STEP 2: ENTITYBLOCK IMPLEMENTATION (REQUIRED)
    // ========================================================================
    // Copy this exactly - it provides the BlockEntity that stores copied data

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopyBlockEntity(pos, state);
    }

    // ========================================================================
    // STEP 3: RENDERING (REQUIRED)
    // ========================================================================
    // Copy this for dynamic texture rendering

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ========================================================================
    // STEP 4: PHYSICS DELEGATION (RECOMMENDED)
    // ========================================================================
    // These make your block inherit hardness/resistance from copied blocks
    // Copy these into your Block's override methods

    public float getExplosionResistance(BlockState state, BlockGetter level,
                                        BlockPos pos, Explosion explosion) {
        return copyblock$getExplosionResistance(state, level, pos, explosion);
    }

    public float getDestroyProgress(BlockState state, Player player,
                                    BlockGetter level, BlockPos pos) {
        return copyblock$getDestroyProgress(state, player, level, pos);
    }

    public SoundType getSoundType(BlockState state, LevelReader level,
                                  BlockPos pos, @Nullable Entity entity) {
        return copyblock$getSoundType(state, level, pos, entity);
    }

    // ========================================================================
    // STEP 5: INTERACTION (REQUIRED FOR PLACING/REMOVING BLOCKS)
    // ========================================================================
    // This handles right-click to place blocks and shift+click to remove

    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        return copyblock$use(state, level, pos, player, hand, hit);
    }

    // ========================================================================
    // STEP 6: CREATIVE MIDDLE-CLICK (OPTIONAL BUT NICE)
    // ========================================================================
    // Allows players to middle-click to get the copied block in creative

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos,
                                       BlockState state) {
        ItemStack result = copyblock$getCloneItemStack(level, pos, state);
        // If no copied block, fall back to your block's item
        // NOTE: In your real implementation, replace with:
        // return result.isEmpty() ? new ItemStack(this) : result;
        return result.isEmpty() ? ItemStack.EMPTY : result;
    }

    // ========================================================================
    // STEP 7: LIFECYCLE METHODS (REQUIRED)
    // ========================================================================
    // These handle block removal and placement

    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        // IMPORTANT: Call this FIRST before super.onRemove()
        copyblock$onRemove(state, level, pos, newState, isMoving);

        // Then call your parent class's onRemove if it has one
        // super.onRemove(state, level, pos, newState, isMoving);
    }

    public void playerWillDestroy(Level level, BlockPos pos, BlockState state,
                                  Player player) {
        // IMPORTANT: Call this FIRST before super.playerWillDestroy()
        copyblock$playerWillDestroy(level, pos, state, player);

        // Then call your parent class's playerWillDestroy if it has one
        // super.playerWillDestroy(level, pos, state, player);
    }

    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            ItemStack stack) {
        // Call your parent class's setPlacedBy first if it has one
        // super.setPlacedBy(level, pos, state, placer, stack);

        // Then initialize the CopyBlock
        copyblock$setPlacedBy(level, pos, state, placer, stack);
    }

    // ========================================================================
    // OPTIONAL: ADVANCED CUSTOMIZATION
    // ========================================================================
    // Only override these if you need custom behavior

    @Override
    public boolean useDynamicModel() {
        return true; // false = custom rendering (advanced)
    }

    @Override
    public boolean useDynamicTags() {
        return true; // false = don't inherit tags from copied blocks
    }

    @Override
    public boolean useDynamicPhysics() {
        return true; // false = use your own hardness/resistance
    }

    @Override
    public boolean consumesItemsOnPlace() {
        return true; // false = free copying in survival (creative mode)
    }

    @Override
    public boolean allowCreativeRemoval() {
        return true; // false = can't remove with shift+click in creative
    }

    @Override
    public boolean allowSurvivalRemoval() {
        return false; // true = can remove with shift+click in survival
    }

    @Override
    public boolean allowPartialBlocks() {
        return false; // true = can copy stairs, slabs, etc (usually false)
    }

    // ========================================================================
    // SPECIAL CASES: BLOCKS WITH DYNAMIC SHAPES
    // ========================================================================
    // If your block has shapes that change (like layers or slabs), you need
    // to calculate the effective mass multiplier based on block state

    /**
     * EXAMPLE: For a layer block with 1-8 layers
     *
     * public float getEffectiveMassMultiplier(BlockState state) {
     *     int layers = state.getValue(LAYERS); // 1-8
     *     return getMassMultiplier() * layers; // 0.125 * layers
     * }
     */

    /**
     * EXAMPLE: For a slab with bottom/top/double variants
     *
     * public float getEffectiveMassMultiplier(BlockState state) {
     *     SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
     *     if (type == SlabType.DOUBLE) {
     *         return getMassMultiplier() * 2.0f; // Double slab = 2x mass
     *     }
     *     return getMassMultiplier(); // Single slab = 1x mass
     * }
     */

    // ========================================================================
    // REGISTRATION: DON'T FORGET THIS
    // ========================================================================
    /**
     * In your mod's FMLCommonSetupEvent:
     *
     * event.enqueueWork(() -> {
     *     CopyBlockRegistration.registerForMod("yourmodid");
     * });
     *
     * This enables:
     * - Dynamic textures
     * - Dynamic tags
     * - VS2 integration (if installed)
     */

    // ========================================================================
    // COMPLETE EXAMPLE
    // ========================================================================
    /**
     * Here's what a real implementation looks like:
     *
     * public class MyQuarterBlock extends Block implements EntityBlock, ICopyBlock {
     *
     *     public MyQuarterBlock(Properties props) {
     *         super(props);
     *     }
     *
     *     @Override
     *     public float getMassMultiplier() {
     *         return 0.25f; // Quarter block
     *     }
     *
     *     // Copy all the methods from STEP 2-7 above
     *
     *     @Nullable
     *     @Override
     *     public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
     *         return new CopyBlockEntity(pos, state);
     *     }
     *
     *     @Override
     *     public RenderShape getRenderShape(BlockState state) {
     *         return RenderShape.MODEL;
     *     }
     *
     *     // ... etc (copy other methods)
     * }
     */
}