package com.vibey.imitari.vs2;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import kotlin.Pair;
import kotlin.Triple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;

import java.util.Collections;
import java.util.List;

/**
 * ACTUAL implementation with VS2 dependencies.
 * This class is ONLY loaded when VS2 is present via reflection.
 *
 * FIXED ISSUES:
 * 1. Empty blocks are always 10kg (regardless of mass multiplier)
 * 2. Layer blocks update mass when layers property changes
 * 3. Breaking blocks subtracts correct copied mass
 */
public class VS2CopyBlockIntegrationImpl implements BlockStateInfoProvider {
    public static final VS2CopyBlockIntegrationImpl INSTANCE = new VS2CopyBlockIntegrationImpl();

    private static final double EMPTY_COPY_BLOCK_MASS = 10.0;

    // Thread-local context for accessing block entities during getBlockStateMass
    private static final ThreadLocal<Level> CURRENT_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> CURRENT_POS = new ThreadLocal<>();

    @Override
    public int getPriority() {
        return 200;
    }

    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        if (!(blockState.getBlock() instanceof ICopyBlock copyBlock)) {
            return null;
        }

        // Try to get the actual mass from the block entity if we have context
        Level level = CURRENT_LEVEL.get();
        BlockPos pos = CURRENT_POS.get();

        if (level != null && pos != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                BlockState copiedBlock = copyBE.getCopiedBlock();

                // CRITICAL: Empty blocks are ALWAYS 10kg regardless of blockstate!
                if (copiedBlock == null || copiedBlock.isAir()) {
                    return EMPTY_COPY_BLOCK_MASS; // Always 10kg when empty
                }

                // Has copied content - NOW apply the multiplier
                Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedBlock);
                double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;

                // Get the CURRENT mass multiplier (handles layers changing)
                float currentMultiplier = this.getEffectiveMassMultiplier(blockState, copyBlock);

                return copiedMass * currentMultiplier;
            }
        }

        // Default to empty mass (no context or no block entity)
        return EMPTY_COPY_BLOCK_MASS;
    }

    /**
     * Get the effective mass multiplier for a block state.
     * For layer blocks, this multiplies by the layer count.
     * For other blocks, just returns the base multiplier.
     */
    private float getEffectiveMassMultiplier(BlockState state, ICopyBlock copyBlock) {
        // Check if this block has a "layers" property (CopyBlockLayer)
        try {
            if (state.hasProperty(com.vibey.imitari.block.CopyBlockLayer.LAYERS)) {
                int layers = state.getValue(com.vibey.imitari.block.CopyBlockLayer.LAYERS);
                // Base multiplier (0.125) * layer count
                return copyBlock.getMassMultiplier() * layers;
            }
        } catch (Exception e) {
            // Not a layer block or property doesn't exist
        }

        // For slabs, check if it's a double slab
        try {
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)) {
                var type = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE);
                if (type == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE) {
                    // Double slab = full block mass
                    return 1.0f;
                }
            }
        } catch (Exception e) {
            // Not a slab
        }

        // Default: just the base multiplier
        return copyBlock.getMassMultiplier();
    }

    @Nullable
    @Override
    public BlockType getBlockStateType(BlockState blockState) {
        return null;
    }

    @Override
    public List<Lod1SolidBlockState> getSolidBlockStates() {
        return Collections.emptyList();
    }

    @Override
    public List<Lod1LiquidBlockState> getLiquidBlockStates() {
        return Collections.emptyList();
    }

    @Override
    public List<Triple<Integer, Integer, Integer>> getBlockStateData() {
        return Collections.emptyList();
    }

    /**
     * Called when the player manually changes what a CopyBlock is copying.
     * This tells VS2 to change the mass from the old copied block to the new one.
     */
    public static void updateCopyBlockMass(Level level, BlockPos pos, BlockState copyBlockState, BlockState oldCopiedBlock) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) return;
        if (!(copyBlockState.getBlock() instanceof ICopyBlock copyBlock)) return;

        BlockState newCopiedBlock = copyBE.getCopiedBlock();

        // Get effective multiplier from instance method
        float effectiveMultiplier = INSTANCE.getEffectiveMassMultiplier(copyBlockState, copyBlock);

        // Calculate what the OLD mass was
        double oldMass;
        if (oldCopiedBlock == null || oldCopiedBlock.isAir()) {
            // Was empty - always 10kg
            oldMass = EMPTY_COPY_BLOCK_MASS;
        } else {
            Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(oldCopiedBlock);
            double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
            oldMass = copiedMass * effectiveMultiplier;
        }

        // Calculate what the NEW mass should be
        double newMass;
        if (newCopiedBlock == null || newCopiedBlock.isAir()) {
            // Now empty - always 10kg
            newMass = EMPTY_COPY_BLOCK_MASS;
        } else {
            Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(newCopiedBlock);
            double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
            newMass = copiedMass * effectiveMultiplier;
        }

        System.out.println("[Imitari VS2] updateCopyBlockMass - Old: " + oldMass + "kg -> New: " + newMass + "kg");

        // Get block type info - use context to help getBlockStateMass work
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos);
        try {
            Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(copyBlockState);
            if (blockInfo == null) return;

            // Update VS2 - this tells VS2 to change the mass from oldMass to newMass
            var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
            if (shipWorld != null) {
                shipWorld.onSetBlock(
                        pos.getX(), pos.getY(), pos.getZ(),
                        VSGameUtilsKt.getDimensionId(level),
                        blockInfo.getSecond(),
                        blockInfo.getSecond(),
                        oldMass,
                        newMass
                );
            }
        } finally {
            CURRENT_LEVEL.remove();
            CURRENT_POS.remove();
        }
    }

    /**
     * Called when a block state changes (like layers growing).
     * This updates VS2 with the new mass multiplier.
     */
    public static void updateCopyBlockState(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level.isClientSide) return;
        if (!(newState.getBlock() instanceof ICopyBlock copyBlock)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) return;

        BlockState copiedBlock = copyBE.getCopiedBlock();
        if (copiedBlock == null || copiedBlock.isAir()) {
            // Empty - no need to update (always 10kg)
            return;
        }

        // Calculate old and new effective multipliers
        float oldMultiplier = INSTANCE.getEffectiveMassMultiplier(oldState, copyBlock);
        float newMultiplier = INSTANCE.getEffectiveMassMultiplier(newState, copyBlock);

        if (Math.abs(oldMultiplier - newMultiplier) < 0.001f) {
            // No change in multiplier
            return;
        }

        // Calculate masses
        Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedBlock);
        double copiedBaseMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;

        double oldMass = copiedBaseMass * oldMultiplier;
        double newMass = copiedBaseMass * newMultiplier;

        System.out.println("[Imitari VS2] Block state changed - Old multiplier: " + oldMultiplier +
                " -> New multiplier: " + newMultiplier);
        System.out.println("[Imitari VS2] Mass update: " + oldMass + "kg -> " + newMass + "kg");

        // Notify VS2
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos);
        try {
            Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(newState);
            if (blockInfo == null) return;

            var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
            if (shipWorld != null) {
                shipWorld.onSetBlock(
                        pos.getX(), pos.getY(), pos.getZ(),
                        VSGameUtilsKt.getDimensionId(level),
                        blockInfo.getSecond(),
                        blockInfo.getSecond(),
                        oldMass,
                        newMass
                );
            }
        } finally {
            CURRENT_LEVEL.remove();
            CURRENT_POS.remove();
        }
    }

    /**
     * Called from CopyBlockEntity when NBT data is loaded.
     * This is CRITICAL for ship assembly - VS2 loads the block first (getting EMPTY_COPY_BLOCK_MASS),
     * then loads the BlockEntity data from NBT. We need to update VS2 at that point.
     */
    public static void onBlockEntityDataLoaded(Level level, BlockPos pos, BlockState state, BlockState copiedBlock) {
        if (level == null || level.isClientSide) return;
        if (!(state.getBlock() instanceof ICopyBlock copyBlock)) return;

        // Only update if we actually have copied content
        if (copiedBlock == null || copiedBlock.isAir()) return;

        // Check if we're in a shipyard (VS2 dimension)
        var shipObjectWorld = VSGameUtilsKt.getShipObjectWorld(level);
        if (shipObjectWorld == null) return;

        // Calculate the correct mass
        Pair<Double, BlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
        double copiedMass = (copiedInfo != null && copiedInfo.getFirst() != null) ? copiedInfo.getFirst() : 50.0;
        float effectiveMultiplier = INSTANCE.getEffectiveMassMultiplier(state, copyBlock);
        double correctMass = copiedMass * effectiveMultiplier;

        System.out.println("[Imitari VS2] BlockEntity loaded with copied data! Updating mass from " +
                EMPTY_COPY_BLOCK_MASS + "kg to " + correctMass + "kg");

        // Get block type
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos);
        try {
            Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(state);
            if (blockInfo == null) return;

            // Tell VS2 to update from empty mass to correct mass
            shipObjectWorld.onSetBlock(
                    pos.getX(), pos.getY(), pos.getZ(),
                    VSGameUtilsKt.getDimensionId(level),
                    blockInfo.getSecond(),
                    blockInfo.getSecond(),
                    EMPTY_COPY_BLOCK_MASS,  // What VS2 initially calculated
                    correctMass              // What it should actually be
            );
        } finally {
            CURRENT_LEVEL.remove();
            CURRENT_POS.remove();
        }
    }

    /**
     * Called when a CopyBlock is being removed/broken.
     * This tells VS2 to transition from the dynamic mass to the default empty mass.
     *
     * CRITICAL: Must be called BEFORE the block is actually removed!
     * VS2 will then automatically subtract the default mass (10kg) when the block is removed.
     */
    public static void onBlockRemoved(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level.isClientSide) return;
        if (oldState.is(newState.getBlock())) return; // Not actually being removed
        if (!(oldState.getBlock() instanceof ICopyBlock copyBlock)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) return;

        BlockState copiedBlock = copyBE.getCopiedBlock();

        // Only need to notify VS2 if the block has copied content
        // (Empty blocks already have the correct 10kg mass)
        if (copiedBlock == null || copiedBlock.isAir()) {
            return; // Already 10kg, VS2 will handle removal correctly
        }

        // Calculate the current mass (dynamic mass based on copied block)
        Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedBlock);
        double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
        float effectiveMultiplier = INSTANCE.getEffectiveMassMultiplier(oldState, copyBlock);
        double currentMass = copiedMass * effectiveMultiplier;

        System.out.println("[Imitari VS2] Block being removed! Transitioning from " + currentMass + "kg to " + EMPTY_COPY_BLOCK_MASS + "kg");

        // Get block type info
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos);
        try {
            Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(oldState);
            if (blockInfo == null) return;

            var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
            if (shipWorld != null) {
                // Tell VS2: transition from dynamic mass to empty mass
                // Then VS2's automatic removal will subtract the 10kg
                shipWorld.onSetBlock(
                        pos.getX(), pos.getY(), pos.getZ(),
                        VSGameUtilsKt.getDimensionId(level),
                        blockInfo.getSecond(),  // Old block type
                        blockInfo.getSecond(),  // Same block type (not removed yet)
                        currentMass,            // Old mass (dynamic mass with copied block)
                        EMPTY_COPY_BLOCK_MASS   // New mass (empty mass - VS2 will subtract this)
                );
            }
        } finally {
            CURRENT_LEVEL.remove();
            CURRENT_POS.remove();
        }
    }

    public static void register() {
        Registry.register(
                BlockStateInfo.INSTANCE.getREGISTRY(),
                new ResourceLocation(Imitari.MODID, "copyblock_mass"),
                INSTANCE
        );
    }
}