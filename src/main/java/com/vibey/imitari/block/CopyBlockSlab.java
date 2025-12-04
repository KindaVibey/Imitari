package com.vibey.imitari.block;

import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Slab-sized CopyBlock variant (0.5x multiplier).
 * Now uses the simplified delegation system.
 */
public class CopyBlockSlab extends CopyBlockBase {
    protected static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    protected static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

    public CopyBlockSlab(Properties properties) {
        super(properties, 0.5f);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)
                .setValue(MASS_HIGH, 0)
                .setValue(MASS_LOW, 0));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                copyBE.setCopiedBlock(Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                return copiedState.getSoundType(level, pos, entity);
            }
        }
        return SoundType.WOOD;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.SLAB_TYPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);

        if (state.is(this)) {
            // Play sound when completing to double slab
            if (!context.getLevel().isClientSide) {
                BlockEntity be = context.getLevel().getBlockEntity(pos);
                if (be instanceof CopyBlockEntity copyBE) {
                    BlockState copiedState = copyBE.getCopiedBlock();
                    if (!copiedState.isAir()) {
                        playBlockSound(context.getLevel(), pos, copiedState);
                    }
                }
            }
            return state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
        }

        Direction facing = context.getClickedFace();
        if (facing == Direction.DOWN || (facing != Direction.UP && context.getClickLocation().y - pos.getY() > 0.5)) {
            return this.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
        }

        return this.defaultBlockState();
    }

    protected void playBlockSound(Level level, BlockPos pos, BlockState copiedState) {
        if (!level.isClientSide && !copiedState.isAir()) {
            SoundType soundType = copiedState.getSoundType(level, pos, null);
            level.playSound(
                    null,
                    pos,
                    soundType.getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F
            );
        }
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        if (type != SlabType.DOUBLE && context.getItemInHand().getItem() == this.asItem()) {
            if (context.replacingClickedOnBlock()) {
                boolean isTop = context.getClickLocation().y - context.getClickedPos().getY() > 0.5;
                Direction facing = context.getClickedFace();

                if (type == SlabType.BOTTOM) {
                    return facing == Direction.UP || (facing != Direction.DOWN && isTop);
                } else {
                    return facing == Direction.DOWN || (facing != Direction.UP && !isTop);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        return switch (type) {
            case DOUBLE -> Shapes.block();
            case TOP -> TOP_SHAPE;
            default -> BOTTOM_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        return switch (type) {
            case DOUBLE -> Shapes.block();
            case TOP -> TOP_SHAPE;
            default -> BOTTOM_SHAPE;
        };
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getCollisionShape(state, level, pos, context);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        return type != SlabType.DOUBLE;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                try {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player != null && mc.player.isShiftKeyDown()) {
                        return new ItemStack(copiedState.getBlock());
                    }
                } catch (Exception e) {
                    // Server side or error
                }
            }
        }
        return super.getCloneItemStack(level, pos, state);
    }
}