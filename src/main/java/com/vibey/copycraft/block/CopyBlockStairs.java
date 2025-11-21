package com.vibey.copycraft.block;

import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stairs-sized CopyBlock variant (0.75x multiplier)
 */
public class CopyBlockStairs extends CopyBlockVariant {
    // Proper stair shapes for collision
    protected static final VoxelShape BOTTOM_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    protected static final VoxelShape BOTTOM_NORTH = Shapes.or(BOTTOM_AABB, Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 8.0));
    protected static final VoxelShape BOTTOM_SOUTH = Shapes.or(BOTTOM_AABB, Block.box(0.0, 8.0, 8.0, 16.0, 16.0, 16.0));
    protected static final VoxelShape BOTTOM_WEST = Shapes.or(BOTTOM_AABB, Block.box(0.0, 8.0, 0.0, 8.0, 16.0, 16.0));
    protected static final VoxelShape BOTTOM_EAST = Shapes.or(BOTTOM_AABB, Block.box(8.0, 8.0, 0.0, 16.0, 16.0, 16.0));

    protected static final VoxelShape TOP_AABB = Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0);
    protected static final VoxelShape TOP_NORTH = Shapes.or(TOP_AABB, Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 8.0));
    protected static final VoxelShape TOP_SOUTH = Shapes.or(TOP_AABB, Block.box(0.0, 0.0, 8.0, 16.0, 8.0, 16.0));
    protected static final VoxelShape TOP_WEST = Shapes.or(TOP_AABB, Block.box(0.0, 0.0, 0.0, 8.0, 8.0, 16.0));
    protected static final VoxelShape TOP_EAST = Shapes.or(TOP_AABB, Block.box(8.0, 0.0, 0.0, 16.0, 8.0, 16.0));

    public CopyBlockStairs(Properties properties) {
        super(properties, 0.75f);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM)
                .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT)
                .setValue(MASS_HIGH, 0)
                .setValue(MASS_LOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); // This adds MASS_HIGH and MASS_LOW
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.HALF, BlockStateProperties.STAIRS_SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos();

        Half half = (context.getClickedFace() == Direction.DOWN ||
                (context.getClickedFace() != Direction.UP &&
                        context.getClickLocation().y - pos.getY() > 0.5))
                ? Half.TOP : Half.BOTTOM;

        BlockState state = this.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, half);

        return state.setValue(BlockStateProperties.STAIRS_SHAPE,
                getStairsShape(state, context.getLevel(), pos));
    }

    private StairsShape getStairsShape(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Half half = state.getValue(BlockStateProperties.HALF);

        BlockState leftState = level.getBlockState(pos.relative(facing.getCounterClockWise()));
        BlockState rightState = level.getBlockState(pos.relative(facing.getClockWise()));

        if (leftState.getBlock() instanceof CopyBlockStairs && leftState.getValue(BlockStateProperties.HALF) == half) {
            Direction leftFacing = leftState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (leftFacing.getAxis() != facing.getAxis() && canConnectTo(state, leftState, facing.getCounterClockWise())) {
                return leftFacing == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.INNER_LEFT;
            }
        }

        if (rightState.getBlock() instanceof CopyBlockStairs && rightState.getValue(BlockStateProperties.HALF) == half) {
            Direction rightFacing = rightState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (rightFacing.getAxis() != facing.getAxis() && canConnectTo(state, rightState, facing.getClockWise())) {
                return rightFacing == facing.getClockWise() ? StairsShape.OUTER_RIGHT : StairsShape.INNER_RIGHT;
            }
        }

        return StairsShape.STRAIGHT;
    }

    private boolean canConnectTo(BlockState state, BlockState neighbor, Direction direction) {
        return neighbor.getBlock() instanceof CopyBlockStairs;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        StairsShape shape = state.getValue(BlockStateProperties.STAIRS_SHAPE);

        switch (mirror) {
            case LEFT_RIGHT -> {
                if (facing.getAxis() == Direction.Axis.Z) {
                    switch (shape) {
                        case INNER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.INNER_RIGHT);
                        }
                        case INNER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.INNER_LEFT);
                        }
                        case OUTER_LEFT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.OUTER_RIGHT);
                        }
                        case OUTER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.OUTER_LEFT);
                        }
                        default -> {
                            return state.rotate(Rotation.CLOCKWISE_180);
                        }
                    }
                }
                return state;
            }
            case FRONT_BACK -> {
                if (facing.getAxis() == Direction.Axis.X) {
                    switch (shape) {
                        case INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT -> {
                            return state.rotate(Rotation.CLOCKWISE_180)
                                    .setValue(BlockStateProperties.STAIRS_SHAPE, shape);
                        }
                        default -> {
                            return state.rotate(Rotation.CLOCKWISE_180);
                        }
                    }
                }
                return state;
            }
            default -> {
                return state;
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Half half = state.getValue(BlockStateProperties.HALF);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        if (half == Half.BOTTOM) {
            return switch (facing) {
                case NORTH -> BOTTOM_NORTH;
                case SOUTH -> BOTTOM_SOUTH;
                case WEST -> BOTTOM_WEST;
                case EAST -> BOTTOM_EAST;
                default -> BOTTOM_AABB;
            };
        } else {
            return switch (facing) {
                case NORTH -> TOP_NORTH;
                case SOUTH -> TOP_SOUTH;
                case WEST -> TOP_WEST;
                case EAST -> TOP_EAST;
                default -> TOP_AABB;
            };
        }
    }

    // FIX: Forward collision shape to copied block when available, otherwise use proper stair shape
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                // For stairs, we want to use our stair shape for consistent collision
                // The copied block's full collision might cause VS issues
                return getShape(state, level, pos, context);
            }
        }
        return getShape(state, level, pos, context);
    }
}