package com.vibey.imitari.vs2;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.block.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
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

public class VS2CopyBlockIntegration implements BlockStateInfoProvider {
    public static final VS2CopyBlockIntegration INSTANCE = new VS2CopyBlockIntegration();

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
                if (copiedBlock != null && !copiedBlock.isAir()) {
                    Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedBlock);
                    double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
                    return copiedMass * copyBlock.getMassMultiplier();
                }
            }
        }

        // Default to empty mass
        return EMPTY_COPY_BLOCK_MASS;
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
            oldMass = EMPTY_COPY_BLOCK_MASS;
        } else {
            Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(oldCopiedBlock);
            double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
            oldMass = copiedMass * copyBlock.getMassMultiplier();
        }

        // Calculate what the NEW mass should be
        double newMass;
        if (newCopiedBlock == null || newCopiedBlock.isAir()) {
            newMass = EMPTY_COPY_BLOCK_MASS;
        } else {
            Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(newCopiedBlock);
            double copiedMass = (info != null && info.getFirst() != null) ? info.getFirst() : 50.0;
            newMass = copiedMass * copyBlock.getMassMultiplier();
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

        // Calculate the correct mass
        Pair<Double, BlockType> copiedInfo = BlockStateInfo.INSTANCE.get(copiedBlock);
        double copiedMass = (copiedInfo != null && copiedInfo.getFirst() != null) ? copiedInfo.getFirst() : 50.0;
        double correctMass = copiedMass * copyBlock.getMassMultiplier();

        System.out.println("[Imitari VS2] BlockEntity loaded with copied data! Updating mass from " +
                EMPTY_COPY_BLOCK_MASS + " to " + correctMass);

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

    public static void register() {
        Registry.register(
                BlockStateInfo.INSTANCE.getREGISTRY(),
                new ResourceLocation(Imitari.MODID, "copyblock_mass"),
                INSTANCE
        );
    }
}