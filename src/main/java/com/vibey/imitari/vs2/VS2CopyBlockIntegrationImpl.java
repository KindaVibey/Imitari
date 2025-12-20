package com.vibey.imitari.vs2;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import com.vibey.imitari.block.CopyBlockLayer;
import kotlin.Pair;
import kotlin.Triple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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

        // CRITICAL: Check for copied content FIRST, before any multiplier calculations
        Level level = CURRENT_LEVEL.get();
        BlockPos pos = CURRENT_POS.get();

        if (level != null && pos != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                BlockState copiedBlock = copyBE.getCopiedBlock();

                // If empty, ALWAYS return 10kg - don't even look at the blockstate properties
                if (copiedBlock == null || copiedBlock.isAir()) {
                    return EMPTY_COPY_BLOCK_MASS;
                }

                // HAS copied block - NOW we can use dynamic multiplier based on state
                Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedBlock);
                double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
                float effectiveMassMultiplier = getEffectiveMassMultiplier(blockState, copyBlock);
                return copiedMass * effectiveMassMultiplier;
            }
        }

        // NO context - assume empty, always 10kg
        return EMPTY_COPY_BLOCK_MASS;
    }

    /**
     * Get the effective mass multiplier, accounting for dynamic states like layer count.
     *
     * @param state The block state
     * @param copyBlock The ICopyBlock instance
     * @return The effective mass multiplier
     */
    private float getEffectiveMassMultiplier(BlockState state, ICopyBlock copyBlock) {
        // Special handling for CopyBlockLayer - multiply base by layer count

        if (copyBlock instanceof CopyBlockLayer layerBlock) {
            return layerBlock.getEffectiveMassMultiplier(state);
        }

        // Special handling for slabs - check if it's a double slab
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE)) {
            var slabType = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE);
            if (slabType == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE) {
                // Double slab = 2x the base multiplier
                return copyBlock.getMassMultiplier() * 2.0f;
            }
        }

        // Default: just use the base multiplier
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

        // Calculate what the OLD mass was
        double oldMass;
        if (oldCopiedBlock == null || oldCopiedBlock.isAir()) {
            // Was empty - always 10kg regardless of state
            oldMass = EMPTY_COPY_BLOCK_MASS;
        } else {
            // Had copied block - calculate with effective multiplier
            Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(oldCopiedBlock);
            double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
            float effectiveMassMultiplier = INSTANCE.getEffectiveMassMultiplier(copyBlockState, copyBlock);
            oldMass = copiedMass * effectiveMassMultiplier;
        }

        // Calculate what the NEW mass should be
        double newMass;
        if (newCopiedBlock == null || newCopiedBlock.isAir()) {
            // Now empty - always 10kg regardless of state
            newMass = EMPTY_COPY_BLOCK_MASS;
        } else {
            // Now has copied block - calculate with effective multiplier
            Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(newCopiedBlock);
            double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
            float effectiveMassMultiplier = INSTANCE.getEffectiveMassMultiplier(copyBlockState, copyBlock);
            newMass = copiedMass * effectiveMassMultiplier;
        }

        System.out.println("[Imitari VS2] updateCopyBlockMass - Old: " + oldMass + " -> New: " + newMass);

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

        // Calculate the correct mass with effective multiplier
        Pair<Double, BlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
        double copiedMass = (copiedInfo != null && copiedInfo.getFirst() != null) ? copiedInfo.getFirst() : 50.0;
        float effectiveMassMultiplier = INSTANCE.getEffectiveMassMultiplier(state, copyBlock);
        double correctMass = copiedMass * effectiveMassMultiplier;

        System.out.println("[Imitari VS2] BlockEntity loaded with copied data! Updating mass from " +
                EMPTY_COPY_BLOCK_MASS + " to " + correctMass + " (multiplier: " + effectiveMassMultiplier + ")");

        // Get block type
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos);
        try {
            Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(state);
            if (blockInfo == null) return;

            // Tell VS2 to update from empty mass (10kg) to correct mass
            shipObjectWorld.onSetBlock(
                    pos.getX(), pos.getY(), pos.getZ(),
                    VSGameUtilsKt.getDimensionId(level),
                    blockInfo.getSecond(),
                    blockInfo.getSecond(),
                    EMPTY_COPY_BLOCK_MASS,  // What VS2 initially calculated (always 10kg for empty)
                    correctMass              // What it should actually be
            );
        } finally {
            CURRENT_LEVEL.remove();
            CURRENT_POS.remove();
        }
    }

    /**
     * Called when a block state changes (e.g., slab becomes double slab, layer count increases).
     * This notifies VS2 that the mass has changed due to the block state change.
     *
     * IMPORTANT: Only affects blocks WITH copied content. Empty blocks always stay 10kg.
     */
    public static void onBlockStateChanged(Level level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level.isClientSide) return;
        if (!(newState.getBlock() instanceof ICopyBlock copyBlock)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) return;

        BlockState copiedBlock = copyBE.getCopiedBlock();

        // If there's no copied block, both old and new mass are 10kg - no change needed
        if (copiedBlock == null || copiedBlock.isAir()) {
            return;
        }

        // Calculate old and new mass based on state changes
        float oldMultiplier = INSTANCE.getEffectiveMassMultiplier(oldState, copyBlock);
        float newMultiplier = INSTANCE.getEffectiveMassMultiplier(newState, copyBlock);

        // Only update if multiplier actually changed
        if (Math.abs(oldMultiplier - newMultiplier) < 0.001f) return;

        Pair<Double, BlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
        double baseMass = (copiedInfo != null && copiedInfo.getFirst() != null) ? copiedInfo.getFirst() : 50.0;

        double oldMass = baseMass * oldMultiplier;
        double newMass = baseMass * newMultiplier;

        System.out.println("[Imitari VS2] Block state changed! Mass: " + oldMass + " -> " + newMass +
                " (multiplier: " + oldMultiplier + " -> " + newMultiplier + ")");

        // Update VS2
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
     * Called when a CopyBlock is removed/broken.
     * Notifies VS2 to subtract the correct mass.
     */
    public static void onBlockRemoved(Level level, BlockPos pos, BlockState state, BlockState copiedBlock) {
        if (level.isClientSide) return;
        if (!(state.getBlock() instanceof ICopyBlock copyBlock)) return;

        // Calculate the mass that needs to be removed
        double massToRemove;
        if (copiedBlock == null || copiedBlock.isAir()) {
            massToRemove = EMPTY_COPY_BLOCK_MASS;
        } else {
            Pair<Double, BlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
            double copiedMass = (copiedInfo != null && copiedInfo.getFirst() != null) ? copiedInfo.getFirst() : 50.0;
            float effectiveMassMultiplier = INSTANCE.getEffectiveMassMultiplier(state, copyBlock);
            massToRemove = copiedMass * effectiveMassMultiplier;
        }

        System.out.println("[Imitari VS2] Block removed! Subtracting " + massToRemove + "kg");

        // Notify VS2
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos);
        try {
            Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(state);
            if (blockInfo == null) return;

            var shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
            if (shipWorld != null) {
                // When block is removed, we go from current mass to 0
                // VS2 will handle this as a block removal
                shipWorld.onSetBlock(
                        pos.getX(), pos.getY(), pos.getZ(),
                        VSGameUtilsKt.getDimensionId(level),
                        blockInfo.getSecond(),
                        blockInfo.getSecond(),
                        massToRemove - 10,   // Old mass (what we're removing)
                        0.0             // New mass (0 because block is gone)
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