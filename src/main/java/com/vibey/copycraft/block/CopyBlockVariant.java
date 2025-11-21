package com.vibey.copycraft.block;

import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.entity.player.Player;

/**
 * Base class for all CopyBlock variants (full block, stairs, slabs, etc.)
 * Each variant has a mass multiplier that scales properties
 *
 * MASS_HIGH and MASS_LOW properties store mass in BlockState for VS2 integration.
 * Uses piecewise linear encoding for 1kg precision at low masses (0-50kg)
 * while still reaching 4400kg for heavy blocks.
 */
public abstract class CopyBlockVariant extends CopyBlock {
    // Two properties give us 16 × 16 = 256 possible mass values
    public static final IntegerProperty MASS_HIGH = IntegerProperty.create("mass_high", 0, 15);
    public static final IntegerProperty MASS_LOW = IntegerProperty.create("mass_low", 0, 15);

    private final float massMultiplier;

    public CopyBlockVariant(Properties properties, float massMultiplier) {
        super(properties);
        this.massMultiplier = massMultiplier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MASS_HIGH, 0)
                .setValue(MASS_LOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MASS_HIGH, MASS_LOW);
    }

    public float getMassMultiplier() {
        return massMultiplier;
    }

    /**
     * Hybrid encoding with variable precision:
     * Values 0-49:    0-49kg     (1kg steps)   - 50 values
     * Values 50-99:   50-149kg   (2kg steps)   - 50 values
     * Values 100-149: 150-399kg  (5kg steps)   - 50 values
     * Values 150-199: 400-899kg  (10kg steps)  - 50 values
     * Values 200-255: 900-4400kg (50kg steps)  - 56 values
     *
     * This gives 1kg precision where it matters most (0-50kg),
     * while still reaching 4400kg for heavy blocks.
     */
    public static int encodeMass(double mass) {
        mass = Math.max(0, Math.min(4400, mass));

        if (mass < 50) {
            // Zone 1: 0-49kg in 1kg steps
            return (int)mass;
        } else if (mass < 150) {
            // Zone 2: 50-149kg in 2kg steps
            return 50 + (int)((mass - 50) / 2);
        } else if (mass < 400) {
            // Zone 3: 150-399kg in 5kg steps
            return 100 + (int)((mass - 150) / 5);
        } else if (mass < 900) {
            // Zone 4: 400-899kg in 10kg steps
            return 150 + (int)((mass - 400) / 10);
        } else {
            // Zone 5: 900-4400kg in 50kg steps
            int value = 200 + (int)((mass - 900) / 50);
            return Math.min(255, value);
        }
    }

    /**
     * Decode mass from encoded value
     */
    public static double decodeMass(int encoded) {
        if (encoded < 50) {
            // Zone 1: 0-49kg
            return encoded;
        } else if (encoded < 100) {
            // Zone 2: 50-149kg (2kg steps)
            return 50 + (encoded - 50) * 2.0;
        } else if (encoded < 150) {
            // Zone 3: 150-399kg (5kg steps)
            return 150 + (encoded - 100) * 5.0;
        } else if (encoded < 200) {
            // Zone 4: 400-899kg (10kg steps)
            return 400 + (encoded - 150) * 10.0;
        } else {
            // Zone 5: 900-4400kg (50kg steps)
            return 900 + (encoded - 200) * 50.0;
        }
    }

    /**
     * Decode mass from BlockState
     */
    public static double decodeMass(BlockState state) {
        int high = state.getValue(MASS_HIGH);
        int low = state.getValue(MASS_LOW);
        int encoded = high * 16 + low;
        return decodeMass(encoded);
    }

    /**
     * Set mass in BlockState
     */
    public static BlockState setMass(BlockState state, double mass) {
        int encoded = encodeMass(mass);
        int high = encoded / 16;
        int low = encoded % 16;

        return state.setValue(MASS_HIGH, high).setValue(MASS_LOW, low);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                return copiedState.getBlock().getExplosionResistance() * massMultiplier;
            }
        }
        return super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseProgress = copiedState.getDestroyProgress(player, level, pos);
                return baseProgress / massMultiplier;
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}