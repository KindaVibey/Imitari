package com.vibey.imitari.block;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Slab-sized CopyBlock variant (0.5x multiplier).
 */
public class CopyBlockSlab extends Block implements EntityBlock, ICopyBlock {
    protected static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    protected static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

    public CopyBlockSlab(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
    }

    @Override
    public float getMassMultiplier() {
        return 0.5f;
    }

    // ==================== INTERACTION (CRITICAL FIX) ====================

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        // IMPORTANT: Don't delegate if we're about to place a slab (canBeReplaced will handle that)
        if (canBeReplaced(state, new BlockPlaceContext(player, hand, player.getItemInHand(hand), hit))) {
            return InteractionResult.PASS; // Let vanilla placement happen
        }

        // Otherwise, delegate to ICopyBlock for block placement/removal
        return copyblock$use(state, level, pos, player, hand, hit);
    }

    // ==================== REST OF THE CLASS ====================

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @javax.annotation.Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        // DON'T call super.setPlacedBy() because it calls copyblock$setPlacedBy which clears the block
        // We need custom logic for slabs

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);

                // Only clear if this is a newly placed SINGLE slab with no copied block yet
                if (type != SlabType.DOUBLE && copyBE.getCopiedBlock().isAir()) {
                    copyBE.setCopiedBlock(Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopyBlockEntity(pos, state);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        return copyblock$getSoundType(state, level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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

            BlockState newState = state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);

            // CRITICAL: Notify VS2 that slab became double (mass changed!)
            if (!context.getLevel().isClientSide) {
                com.vibey.imitari.vs2.VS2CopyBlockIntegration.onBlockStateChanged(
                        context.getLevel(), pos, state, newState
                );
            }

            return newState;
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
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);

        // If we just changed from single to double slab (or vice versa), refresh the model
        if (!oldState.is(state.getBlock())) {
            return; // Different block, not our concern
        }

        SlabType oldType = oldState.hasProperty(BlockStateProperties.SLAB_TYPE) ?
                oldState.getValue(BlockStateProperties.SLAB_TYPE) : SlabType.BOTTOM;
        SlabType newType = state.getValue(BlockStateProperties.SLAB_TYPE);

        // If we went from single to double (or double to single), refresh textures
        if (oldType != newType) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                if (level.isClientSide) {
                    // Client side - schedule model refresh for next tick
                    net.minecraft.client.Minecraft.getInstance().execute(() -> {
                        BlockEntity stillThere = level.getBlockEntity(pos);
                        if (stillThere instanceof CopyBlockEntity copyBE2) {
                            copyBE2.forceModelRefresh();
                        }
                    });
                } else {
                    // Server side - send update to clients
                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
                }
            }
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
        ItemStack result = copyblock$getCloneItemStack(level, pos, state);
        return result.isEmpty() ? super.getCloneItemStack(level, pos, state) : result;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        copyblock$onRemove(state, level, pos, newState, isMoving);
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        copyblock$playerWillDestroy(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
}