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

    @Override
    public int getPriority() {
        return 200;
    }

    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        // If this is a copy block, return its mass based on what it's copying
        if (blockState.getBlock() instanceof ICopyBlock copyBlock) {
            // We can't access the block entity here, so return the empty mass
            // The actual mass will be set via updateCopyBlockMass
            return EMPTY_COPY_BLOCK_MASS;
        }
        return null;
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

        // Get block type info
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
    }

    public static void register() {
        Registry.register(
                BlockStateInfo.INSTANCE.getREGISTRY(),
                new ResourceLocation(Imitari.MODID, "copyblock_mass"),
                INSTANCE
        );
    }
}