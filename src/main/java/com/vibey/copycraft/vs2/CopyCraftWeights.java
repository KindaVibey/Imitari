package com.vibey.copycraft.vs2;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.block.CopyBlockVariant;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
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

public class CopyCraftWeights implements BlockStateInfoProvider {
    public static final CopyCraftWeights INSTANCE = new CopyCraftWeights();

    private static final ThreadLocal<Level> currentLevel = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> currentPos = new ThreadLocal<>();

    public static void setContext(Level level, BlockPos pos) {
        currentLevel.set(level);
        currentPos.set(pos);
    }

    public static void clearContext() {
        currentLevel.remove();
        currentPos.remove();
    }

    @Override
    public int getPriority() {
        return 200;
    }

    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        if (blockState.getBlock() instanceof CopyBlockVariant copyBlockVariant) {
            Level level = currentLevel.get();
            BlockPos pos = currentPos.get();

            if (level != null && pos != null) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CopyBlockEntity copyBE) {
                    BlockState copiedState = copyBE.getCopiedBlock();
                    if (!copiedState.isAir()) {
                        // Get the mass and type pair from VS
                        Pair<Double, BlockType> info = BlockStateInfo.INSTANCE.get(copiedState);

                        if (info != null) {
                            Double copiedMass = info.getFirst();
                            return copiedMass * copyBlockVariant.getMassMultiplier();
                        }
                    }
                }
            }
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

    public static void register() {
        Registry.register(
                BlockStateInfo.INSTANCE.getREGISTRY(),
                new ResourceLocation(CopyCraft.MODID, "copycraft_weights"),
                INSTANCE
        );
    }
}