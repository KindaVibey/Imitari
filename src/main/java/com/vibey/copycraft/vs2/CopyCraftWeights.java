package com.vibey.copycraft.vs2;

import com.vibey.copycraft.CopyCraft;
import com.vibey.copycraft.block.CopyBlockVariant;
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
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState;
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState;

import java.util.Collections;
import java.util.List;

/**
 * VS2 integration for CopyBlock dynamic mass.
 *
 * Like Eureka's ballast, this reads mass information directly from BlockState properties,
 * making it thread-safe for VS2's physics calculations.
 *
 * The MASS_HIGH and MASS_LOW properties store encoded mass using piecewise linear encoding
 * for 1kg precision at low masses (0-50kg) while reaching up to 4400kg.
 */
public class CopyCraftWeights implements BlockStateInfoProvider {
    public static final CopyCraftWeights INSTANCE = new CopyCraftWeights();

    private CopyCraftWeights() {}

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

        // Read mass from BlockState (thread-safe, no world access needed)
        double mass = CopyBlockVariant.decodeMass(blockState);

        if (mass == 0) {
            // Empty block - light frame
            return 10.0;
        }

        System.out.println("[CopyCraft VS] Block " + blockState.getBlock().getName().getString() +
                " has mass: " + mass + " kg (multiplier=" + copyBlockVariant.getMassMultiplier() + ")");

        return mass;
    }

    @Nullable
    @Override
    public BlockType getBlockStateType(BlockState blockState) {
        return null; // VS will use default
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

    // No-op methods for compatibility
    public static void invalidateCache(BlockPos pos) {
        // Not needed - BlockState changes trigger VS updates automatically
    }

    public static void cleanCache() {
        // Not needed - no cache
    }
}