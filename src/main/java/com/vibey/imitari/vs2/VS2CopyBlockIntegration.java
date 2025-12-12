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
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;

import java.util.Collections;
import java.util.List;

/**
 * VS2 Integration - Provider that returns current CopyBlock mass.
 */
public class VS2CopyBlockIntegration implements BlockStateInfoProvider {
    public static final VS2CopyBlockIntegration INSTANCE = new VS2CopyBlockIntegration();

    // ThreadLocal to pass context from updateCopyBlockMass to getBlockStateMass
    private static final ThreadLocal<Level> CURRENT_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> CURRENT_POS = new ThreadLocal<>();

    private VS2CopyBlockIntegration() {}

    @Override
    public int getPriority() {
        return 200; // Higher than default
    }

    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        if (!(blockState.getBlock() instanceof ICopyBlock copyBlock)) {
            return null;
        }

        Level level = CURRENT_LEVEL.get();
        BlockPos pos = CURRENT_POS.get();

        if (level == null || pos == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) {
            return null;
        }

        BlockState copiedState = copyBE.getCopiedBlock();
        if (copiedState.isAir()) {
            return 10.0; // Empty frame
        }

        // Get mass of copied block
        Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(copiedState);
        if (blockInfo != null && blockInfo.getFirst() != null) {
            return blockInfo.getFirst() * copyBlock.getMassMultiplier();
        }

        return 50.0 * copyBlock.getMassMultiplier();
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
     * Call this when CopyBlock's copied block changes.
     * Sets context and triggers VS2's onSetBlock.
     */
    public static void updateCopyBlockMass(Level level, BlockPos pos, BlockState copyBlockState) {
        if (level.isClientSide) return;

        // Set context so getBlockStateMass can access the BlockEntity
        CURRENT_LEVEL.set(level);
        CURRENT_POS.set(pos.immutable());

        try {
            // Call VS2's onSetBlock - it will query our provider for the new mass
            BlockStateInfo.INSTANCE.onSetBlock(level, pos, copyBlockState, copyBlockState);
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