package com.vibey.imitari.vs2;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import com.vibey.imitari.block.CopyBlockLayer;
import kotlin.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

/**
 * Simplified VS2 integration that provides accurate mass without complex tracking.
 *
 * Key principle: Just provide the correct mass when VS2 queries it.
 * No complex old/new mass tracking needed.
 */
public class VS2CopyBlockIntegrationImpl implements BlockStateInfoProvider {
    public static final VS2CopyBlockIntegrationImpl INSTANCE = new VS2CopyBlockIntegrationImpl();

    private static final double EMPTY_COPY_BLOCK_MASS = 10.0;

    // Simple thread-local context for BlockEntity access during getBlockStateMass
    private static final ThreadLocal<Context> CURRENT_CONTEXT = new ThreadLocal<>();

    private record Context(Level level, BlockPos pos) {}

    @Override
    public int getPriority() {
        return 200;
    }

    /**
     * VS2 calls this to get the mass of a block.
     * We calculate: (copied block mass) * (effective multiplier from block state)
     */
    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        if (!(blockState.getBlock() instanceof ICopyBlock copyBlock)) {
            return null;
        }

        // Need context to access BlockEntity
        Context ctx = CURRENT_CONTEXT.get();
        if (ctx == null) {
            return EMPTY_COPY_BLOCK_MASS; // No context = assume empty
        }

        BlockEntity be = ctx.level.getBlockEntity(ctx.pos);
        if (!(be instanceof ICopyBlockEntity copyBE)) {
            return EMPTY_COPY_BLOCK_MASS;
        }

        BlockState copiedBlock = copyBE.getCopiedBlock();

        // Empty CopyBlock = always 10kg
        if (copiedBlock == null || copiedBlock.isAir()) {
            return EMPTY_COPY_BLOCK_MASS;
        }

        // Has copied block = (copied mass) * (effective multiplier)
        Pair<Double, VsiBlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
        double copiedMass = (copiedInfo != null && copiedInfo.getFirst() != null)
                ? copiedInfo.getFirst()
                : 50.0; // Default fallback

        float effectiveMultiplier = getEffectiveMassMultiplier(blockState, copyBlock);

        return copiedMass * effectiveMultiplier;
    }

    /**
     * Calculate effective mass multiplier based on block state.
     * Handles layers (1-8), double slabs (2x), etc.
     */
    private float getEffectiveMassMultiplier(BlockState state, ICopyBlock copyBlock) {
        // CopyBlockLayer: base multiplier * layer count
        if (copyBlock instanceof CopyBlockLayer layerBlock) {
            return layerBlock.getEffectiveMassMultiplier(state);
        }

        // Double slab: 2x base multiplier
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)) {
            var slabType = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE);
            if (slabType == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE) {
                return copyBlock.getMassMultiplier() * 2.0f;
            }
        }

        // Default: just the base multiplier
        return copyBlock.getMassMultiplier();
    }

    @Nullable
    @Override
    public VsiBlockType getBlockStateType(BlockState blockState) {
        return null; // Let other providers handle this
    }

    // ==================== HELPER METHODS ====================

    /**
     * Query the current mass of a block.
     * Sets context, calls getBlockStateMass, then clears context.
     */
    private static double getCurrentMass(Level level, BlockPos pos, BlockState state) {
        CURRENT_CONTEXT.set(new Context(level, pos));
        try {
            Double mass = INSTANCE.getBlockStateMass(state);
            return mass != null ? mass : EMPTY_COPY_BLOCK_MASS;
        } finally {
            CURRENT_CONTEXT.remove();
        }
    }

    /**
     * Calculate what the mass WOULD BE with a different copied block.
     * Used for old mass calculations.
     */
    private static double calculateMassWithCopiedBlock(BlockState copyBlockState,
                                                       BlockState copiedBlock,
                                                       ICopyBlock copyBlock) {
        if (copiedBlock == null || copiedBlock.isAir()) {
            return EMPTY_COPY_BLOCK_MASS;
        }

        Pair<Double, VsiBlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
        double copiedMass = (copiedInfo != null && copiedInfo.getFirst() != null)
                ? copiedInfo.getFirst()
                : 50.0;

        float effectiveMultiplier = INSTANCE.getEffectiveMassMultiplier(copyBlockState, copyBlock);

        return copiedMass * effectiveMultiplier;
    }

    // ==================== PUBLIC NOTIFICATION METHODS ====================

    /**
     * Notify VS2 when the copied block changes.
     * Called from CopyBlockEntity.setCopiedBlock().
     */
    public static void updateCopyBlockMass(Level level, BlockPos pos,
                                           BlockState copyBlockState,
                                           BlockState oldCopiedBlock) {
        if (level.isClientSide) return;
        if (!(copyBlockState.getBlock() instanceof ICopyBlock copyBlock)) return;

        var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipWorld == null) return;

        // Calculate old mass (with old copied block)
        double oldMass = calculateMassWithCopiedBlock(copyBlockState, oldCopiedBlock, copyBlock);

        // Calculate new mass (with current copied block from BlockEntity)
        double newMass = getCurrentMass(level, pos, copyBlockState);

        // Only notify if mass actually changed
        if (Math.abs(oldMass - newMass) < 0.001) return;

        Pair<Double, VsiBlockType> blockInfo = BlockStateInfo.INSTANCE.get(copyBlockState);
        if (blockInfo == null) return;

        shipWorld.onSetBlock(
                pos.getX(), pos.getY(), pos.getZ(),
                VSGameUtilsKt.getDimensionId(level),
                blockInfo.getSecond(),
                blockInfo.getSecond(),
                oldMass,
                newMass
        );
    }

    /**
     * Notify VS2 when BlockEntity loads NBT data.
     * At load time, VS2 queried mass before NBT was loaded (got 10kg).
     * Now we need to tell it the correct mass.
     */
    public static void onBlockEntityDataLoaded(Level level, BlockPos pos,
                                               BlockState state,
                                               BlockState copiedBlock) {
        if (level == null || level.isClientSide) return;
        if (!(state.getBlock() instanceof ICopyBlock)) return;

        // Only matters if we actually have copied content
        if (copiedBlock == null || copiedBlock.isAir()) return;

        var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipWorld == null) return;

        // Old mass = what VS2 saw before NBT load (always 10kg for empty)
        double oldMass = EMPTY_COPY_BLOCK_MASS;

        // New mass = correct mass now that NBT is loaded
        double newMass = getCurrentMass(level, pos, state);

        Pair<Double, VsiBlockType> blockInfo = BlockStateInfo.INSTANCE.get(state);
        if (blockInfo == null) return;

        shipWorld.onSetBlock(
                pos.getX(), pos.getY(), pos.getZ(),
                VSGameUtilsKt.getDimensionId(level),
                blockInfo.getSecond(),
                blockInfo.getSecond(),
                oldMass,
                newMass
        );
    }

    /**
     * Notify VS2 when block state changes (layer count, slab type, etc).
     * Only matters if there's copied content.
     */
    public static void onBlockStateChanged(Level level, BlockPos pos,
                                           BlockState oldState,
                                           BlockState newState) {
        if (level.isClientSide) return;
        if (!(newState.getBlock() instanceof ICopyBlock copyBlock)) return;

        // Verify BlockEntity exists
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ICopyBlockEntity copyBE)) return;

        BlockState copiedBlock = copyBE.getCopiedBlock();

        // No copied block = mass is always 10kg regardless of layers/slab type
        if (copiedBlock == null || copiedBlock.isAir()) return;

        // Calculate mass with both states
        float oldMultiplier = INSTANCE.getEffectiveMassMultiplier(oldState, copyBlock);
        float newMultiplier = INSTANCE.getEffectiveMassMultiplier(newState, copyBlock);

        // Only notify if multiplier actually changed
        if (Math.abs(oldMultiplier - newMultiplier) < 0.001f) return;

        Pair<Double, VsiBlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
        double baseMass = (copiedInfo != null && copiedInfo.getFirst() != null)
                ? copiedInfo.getFirst()
                : 50.0;

        double oldMass = baseMass * oldMultiplier;
        double newMass = baseMass * newMultiplier;

        var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipWorld == null) return;

        Pair<Double, VsiBlockType> blockInfo = BlockStateInfo.INSTANCE.get(newState);
        if (blockInfo == null) return;

        shipWorld.onSetBlock(
                pos.getX(), pos.getY(), pos.getZ(),
                VSGameUtilsKt.getDimensionId(level),
                blockInfo.getSecond(),
                blockInfo.getSecond(),
                oldMass,
                newMass
        );
    }

    /**
     * Notify VS2 when block is removed.
     * Calculate final mass, then tell VS2 it's now 0.
     */
    public static void onBlockRemoved(Level level, BlockPos pos,
                                      BlockState state,
                                      BlockState copiedBlock) {
        if (level.isClientSide) return;
        if (!(state.getBlock() instanceof ICopyBlock copyBlock)) return;

        var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipWorld == null) return;

        // Calculate the mass that's being removed
        double oldMass = calculateMassWithCopiedBlock(state, copiedBlock, copyBlock);
        double newMass = 10.0; // Block is gone

        Pair<Double, VsiBlockType> blockInfo = BlockStateInfo.INSTANCE.get(state);
        if (blockInfo == null) return;

        shipWorld.onSetBlock(
                pos.getX(), pos.getY(), pos.getZ(),
                VSGameUtilsKt.getDimensionId(level),
                blockInfo.getSecond(),
                blockInfo.getSecond(),
                oldMass,
                newMass
        );
    }

    /**
     * Register with VS2.
     */
    public static void register() {
        Registry.register(
                BlockStateInfo.INSTANCE.getREGISTRY(),
                new ResourceLocation(Imitari.MODID, "copyblock_mass"),
                INSTANCE
        );
    }
}