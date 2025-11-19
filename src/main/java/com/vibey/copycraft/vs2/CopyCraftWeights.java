package com.vibey.copycraft.vs2;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.block.CopyBlockVariant;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import com.vibey.copycraft.registry.ModBlocks;
import kotlin.Pair;
import kotlin.Triple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.apigame.world.chunks.BlockType;
import org.valkyrienskies.mod.common.BlockStateInfo;
import org.valkyrienskies.mod.common.BlockStateInfoProvider;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;

import java.util.Collections;
import java.util.List;

/**
 * SIMPLE VS2 integration like Eureka.
 * The problem: Eureka's ballast has mass in BlockState (power level).
 * Our CopyBlock has mass in BlockEntity (copied block).
 *
 * Solution: Store it in BlockState too! But we can't, so we need a workaround.
 * Real solution: Just look up the BlockEntity directly in getBlockStateMass.
 */
public class CopyCraftWeights implements BlockStateInfoProvider {
    public static final CopyCraftWeights INSTANCE = new CopyCraftWeights();

    private CopyCraftWeights() {}
    // Thread-local to pass context from game to provider
    private static final ThreadLocal<net.minecraft.world.level.Level> currentLevel = new ThreadLocal<>();
    private static final ThreadLocal<BlockPos> currentPos = new ThreadLocal<>();

    @Override
    public int getPriority() {
        return 200; // Higher than VS default (100)
    }

    @Nullable
    @Override
    public Double getBlockStateMass(BlockState blockState) {
        // Only handle our blocks
        if (!(blockState.getBlock() instanceof CopyBlockVariant copyBlockVariant)) {
            return null;
        }

        System.out.println("[CopyCraft VS] getBlockStateMass called for: " + blockState);

        // We need position context to get the BlockEntity
        // This SHOULD be set by our mixin before VS calls us
        net.minecraft.world.level.Level level = currentLevel.get();
        BlockPos pos = currentPos.get();

        if (level == null || pos == null) {
            System.out.println("[CopyCraft VS] ERROR: No context! Returning null");
            return null;
        }

        System.out.println("[CopyCraft VS] Context available: " + pos);

        // Get the block entity
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CopyBlockEntity copyBE)) {
            System.out.println("[CopyCraft VS] ERROR: Wrong block entity at " + pos);
            return null;
        }

        BlockState copiedState = copyBE.getCopiedBlock();
        System.out.println("[CopyCraft VS] Copied block: " + copiedState);

        if (copiedState.isAir()) {
            System.out.println("[CopyCraft VS] Empty block, using 10kg");
            return 10.0;
        }

        // Get the copied block's mass from VS's system
        Pair<Double, BlockType> blockInfo = BlockStateInfo.INSTANCE.get(copiedState);

        if (blockInfo == null || blockInfo.getFirst() == null) {
            System.out.println("[CopyCraft VS] No VS mass data for " + copiedState + ", using 50kg");
            return 50.0;
        }

        Double copiedMass = blockInfo.getFirst();
        float multiplier = copyBlockVariant.getMassMultiplier();
        double finalMass = copiedMass * multiplier;

        System.out.println("[CopyCraft VS] ✓✓✓ SUCCESS: " +
                copiedState.getBlock().getName().getString() +
                " base=" + copiedMass + " kg x" + multiplier +
                " = " + finalMass + " kg");

        return finalMass;
    }

    @Nullable
    @Override
    public BlockType getBlockStateType(BlockState blockState) {
        return null; // VS will use default
    }

    // Called by mixin to set context
    public static void setContext(net.minecraft.world.level.Level level, BlockPos pos) {
        currentLevel.set(level);
        currentPos.set(pos);
        System.out.println("[CopyCraft VS] Context set: " + pos);
    }

    public static void clearContext() {
        currentLevel.remove();
        currentPos.remove();
        System.out.println("[CopyCraft VS] Context cleared");
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
        try {
            Registry.register(
                    BlockStateInfo.INSTANCE.getREGISTRY(),
                    new ResourceLocation(CopyCraft.MODID, "copycraft_weights"),
                    INSTANCE
            );
            System.out.println("[CopyCraft VS] ✓✓✓ REGISTERED BlockStateInfoProvider with priority 200!");
        } catch (Exception e) {
            System.err.println("[CopyCraft VS] FAILED to register:");
            e.printStackTrace();
        }
    }

    // No-op methods for compatibility with CopyBlockEntity
    public static void invalidateCache(BlockPos pos) {
        // Not needed - we query live data
    }

    public static void cleanCache() {
        // Not needed - no cache
    }
}