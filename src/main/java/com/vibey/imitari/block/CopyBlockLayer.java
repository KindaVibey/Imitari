package com.vibey.imitari.block;

import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Layer block that can stack in 8 vertical layers (1/8 block each).
 * Can be placed on any face (like Copycats' layer block).
 */
public class CopyBlockLayer extends Block implements EntityBlock, ICopyBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, 8);
    public static final IntegerProperty MASS_HIGH = IntegerProperty.create("mass_high", 0, 15);
    public static final IntegerProperty MASS_LOW = IntegerProperty.create("mass_low", 0, 15);

    // Shapes for each direction and layer count
    // [direction][layers]
    protected static final VoxelShape[][] SHAPES = new VoxelShape[6][9];

    static {
        // DOWN (layers grow downward from top)
        SHAPES[0][0] = Shapes.empty();
        SHAPES[0][1] = Block.box(0, 14, 0, 16, 16, 16);
        SHAPES[0][2] = Block.box(0, 12, 0, 16, 16, 16);
        SHAPES[0][3] = Block.box(0, 10, 0, 16, 16, 16);
        SHAPES[0][4] = Block.box(0, 8, 0, 16, 16, 16);
        SHAPES[0][5] = Block.box(0, 6, 0, 16, 16, 16);
        SHAPES[0][6] = Block.box(0, 4, 0, 16, 16, 16);
        SHAPES[0][7] = Block.box(0, 2, 0, 16, 16, 16);
        SHAPES[0][8] = Block.box(0, 0, 0, 16, 16, 16);

        // UP (layers grow upward from bottom)
        SHAPES[1][0] = Shapes.empty();
        SHAPES[1][1] = Block.box(0, 0, 0, 16, 2, 16);
        SHAPES[1][2] = Block.box(0, 0, 0, 16, 4, 16);
        SHAPES[1][3] = Block.box(0, 0, 0, 16, 6, 16);
        SHAPES[1][4] = Block.box(0, 0, 0, 16, 8, 16);
        SHAPES[1][5] = Block.box(0, 0, 0, 16, 10, 16);
        SHAPES[1][6] = Block.box(0, 0, 0, 16, 12, 16);
        SHAPES[1][7] = Block.box(0, 0, 0, 16, 14, 16);
        SHAPES[1][8] = Block.box(0, 0, 0, 16, 16, 16);

        // NORTH (layers grow northward from south)
        SHAPES[2][0] = Shapes.empty();
        SHAPES[2][1] = Block.box(0, 0, 14, 16, 16, 16);
        SHAPES[2][2] = Block.box(0, 0, 12, 16, 16, 16);
        SHAPES[2][3] = Block.box(0, 0, 10, 16, 16, 16);
        SHAPES[2][4] = Block.box(0, 0, 8, 16, 16, 16);
        SHAPES[2][5] = Block.box(0, 0, 6, 16, 16, 16);
        SHAPES[2][6] = Block.box(0, 0, 4, 16, 16, 16);
        SHAPES[2][7] = Block.box(0, 0, 2, 16, 16, 16);
        SHAPES[2][8] = Block.box(0, 0, 0, 16, 16, 16);

        // SOUTH (layers grow southward from north)
        SHAPES[3][0] = Shapes.empty();
        SHAPES[3][1] = Block.box(0, 0, 0, 16, 16, 2);
        SHAPES[3][2] = Block.box(0, 0, 0, 16, 16, 4);
        SHAPES[3][3] = Block.box(0, 0, 0, 16, 16, 6);
        SHAPES[3][4] = Block.box(0, 0, 0, 16, 16, 8);
        SHAPES[3][5] = Block.box(0, 0, 0, 16, 16, 10);
        SHAPES[3][6] = Block.box(0, 0, 0, 16, 16, 12);
        SHAPES[3][7] = Block.box(0, 0, 0, 16, 16, 14);
        SHAPES[3][8] = Block.box(0, 0, 0, 16, 16, 16);

        // WEST (layers grow westward from east)
        SHAPES[4][0] = Shapes.empty();
        SHAPES[4][1] = Block.box(14, 0, 0, 16, 16, 16);
        SHAPES[4][2] = Block.box(12, 0, 0, 16, 16, 16);
        SHAPES[4][3] = Block.box(10, 0, 0, 16, 16, 16);
        SHAPES[4][4] = Block.box(8, 0, 0, 16, 16, 16);
        SHAPES[4][5] = Block.box(6, 0, 0, 16, 16, 16);
        SHAPES[4][6] = Block.box(4, 0, 0, 16, 16, 16);
        SHAPES[4][7] = Block.box(2, 0, 0, 16, 16, 16);
        SHAPES[4][8] = Block.box(0, 0, 0, 16, 16, 16);

        // EAST (layers grow eastward from west)
        SHAPES[5][0] = Shapes.empty();
        SHAPES[5][1] = Block.box(0, 0, 0, 2, 16, 16);
        SHAPES[5][2] = Block.box(0, 0, 0, 4, 16, 16);
        SHAPES[5][3] = Block.box(0, 0, 0, 6, 16, 16);
        SHAPES[5][4] = Block.box(0, 0, 0, 8, 16, 16);
        SHAPES[5][5] = Block.box(0, 0, 0, 10, 16, 16);
        SHAPES[5][6] = Block.box(0, 0, 0, 12, 16, 16);
        SHAPES[5][7] = Block.box(0, 0, 0, 14, 16, 16);
        SHAPES[5][8] = Block.box(0, 0, 0, 16, 16, 16);
    }

    private final float massMultiplier;

    public CopyBlockLayer(Properties properties) {
        this(properties, 0.125f); // Each layer is 1/8 of a block
    }

    public CopyBlockLayer(Properties properties, float baseMultiplier) {
        super(properties);
        this.massMultiplier = baseMultiplier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(LAYERS, 1)
                .setValue(MASS_HIGH, 0)
                .setValue(MASS_LOW, 0));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        // Ensure the BlockEntity starts fresh with no copied block
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CopyBlockEntity copyBE) {
                // Force reset to empty state
                copyBE.setCopiedBlock(Blocks.AIR.defaultBlockState());
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LAYERS, MASS_HIGH, MASS_LOW);
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
    public float getMassMultiplier() {
        // Base multiplier times number of layers
        // This will be scaled per-state in getExplosionResistance
        return massMultiplier;
    }

    /**
     * Get the effective mass multiplier for a specific state
     */
    public float getEffectiveMassMultiplier(BlockState state) {
        int layers = state.getValue(LAYERS);
        return massMultiplier * layers;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                float baseResistance = copiedState.getBlock().getExplosionResistance();
                return baseResistance * getEffectiveMassMultiplier(state);
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
                return baseProgress / getEffectiveMassMultiplier(state);
            }
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        int layers = state.getValue(LAYERS);
        return SHAPES[facing.ordinal()][layers];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        int layers = state.getValue(LAYERS);
        return SHAPES[facing.ordinal()][layers];
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        int layers = state.getValue(LAYERS);
        return SHAPES[facing.ordinal()][layers];
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LAYERS) < 8 || state.getValue(FACING) != Direction.UP;
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        if (type == PathComputationType.LAND) {
            return state.getValue(FACING) == Direction.UP && state.getValue(LAYERS) < 5;
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState existingState = context.getLevel().getBlockState(pos);
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();

        // If clicking on an existing layer block with same facing, try to add layers
        if (existingState.is(this)) {
            Direction existingFacing = existingState.getValue(FACING);
            int currentLayers = existingState.getValue(LAYERS);

            // Only stack if same facing and clicked on the layer growth direction
            if (existingFacing == clickedFace && currentLayers < 8) {
                return existingState.setValue(LAYERS, Math.min(8, currentLayers + 1));
            }
        }

        // New placement - Mix of clicked face and player look angle (like Copycats)
        Direction facing = clickedFace;

        if (player != null) {
            float pitch = player.getXRot();
            Direction horizontalFacing = context.getHorizontalDirection();

            if (clickedFace.getAxis().isVertical()) {
                // Clicking top/bottom - check if looking from the side
                if (Math.abs(pitch) < 45) {
                    // Looking horizontally - place vertical layer growing toward player
                    facing = horizontalFacing.getOpposite();
                }
            } else {
                // Clicking a side
                if (Math.abs(pitch) > 45) {
                    // Looking up/down at angle - place horizontal layer growing toward player
                    facing = pitch > 0 ? Direction.DOWN : Direction.UP;
                } else {
                    // Looking horizontally at a side - place based on which axis
                    // Place perpendicular layer growing toward player
                    Direction clickedAxis = clickedFace.getAxis() == Direction.Axis.X ?
                            (horizontalFacing.getAxis() == Direction.Axis.Z ? horizontalFacing.getOpposite() : clickedFace) :
                            (horizontalFacing.getAxis() == Direction.Axis.X ? horizontalFacing.getOpposite() : clickedFace);
                    facing = clickedAxis;
                }
            }
        }

        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        // If player is sneaking, don't allow replacing (forces new placement)
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return false;
        }

        // Allow replacing if not full (8 layers) and using the same item
        if (context.getItemInHand().getItem() == this.asItem()) {
            if (state.getValue(LAYERS) < 8) {
                Direction stateFacing = state.getValue(FACING);
                Direction clickedFace = context.getClickedFace();

                // Can stack if clicking on the same face that the layers are facing
                if (stateFacing == clickedFace) {
                    return context.replacingClickedOnBlock();
                }
            }
        }
        return false;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ========== INTERACTION ==========
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopyBlockEntity copyBlockEntity)) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        BlockState currentCopied = copyBlockEntity.getCopiedBlock();

        // If holding a layer block with shift, don't do anything (let placement happen)
        if (player.isShiftKeyDown() && heldItem.getItem() == this.asItem()) {
            return InteractionResult.PASS;
        }

        // Shift + empty hand (creative only) = remove copied block (no drop)
        if (player.isShiftKeyDown() && heldItem.isEmpty() && !currentCopied.isAir() && player.isCreative()) {
            copyBlockEntity.setCopiedBlock(Blocks.AIR.defaultBlockState());
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, state.getBlock());

            return InteractionResult.SUCCESS;
        }

        // Place block in empty CopyBlock
        if (heldItem.getItem() instanceof BlockItem blockItem) {
            Block targetBlock = blockItem.getBlock();
            if (targetBlock instanceof ICopyBlock) return InteractionResult.FAIL;

            BlockState targetState = targetBlock.defaultBlockState();
            if (!targetState.isCollisionShapeFullBlock(level, pos)) return InteractionResult.FAIL;

            if (!currentCopied.isAir()) {
                if (currentCopied.getBlock() == targetBlock) {
                    copyBlockEntity.setCopiedBlock(targetState);
                    return InteractionResult.SUCCESS;
                } else return InteractionResult.FAIL;
            } else {
                if (!player.isCreative()) heldItem.shrink(1);
                copyBlockEntity.setCopiedBlock(targetState);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        // Creative middle-click with shift: give the copied block
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
                    // Server side or error, just return default
                }
            }
        }
        // Default: give the CopyBlock itself
        return super.getCloneItemStack(level, pos, state);
    }

    // ========== DROPS FIX ==========
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof CopyBlockEntity be) {
                BlockState copiedState = be.getCopiedBlock();
                if (!copiedState.isAir() && !be.wasRemovedByCreative()) {
                    ItemStack droppedItem = new ItemStack(copiedState.getBlock());
                    droppedItem.setTag(null);
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5,
                            pos.getY() + 0.5, pos.getZ() + 0.5, droppedItem));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopyBlockEntity copyBlockEntity) {
            if (player.isCreative()) copyBlockEntity.setRemovedByCreative(true);
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}