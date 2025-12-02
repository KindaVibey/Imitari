package com.vibey.imitari.block;

import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of ICopyBlock for standard full-block behavior.
 * Extend this class for custom CopyBlock variants.
 */
public class CopyBlockBase extends Block implements EntityBlock, ICopyBlock {
    public static final IntegerProperty MASS_HIGH = IntegerProperty.create("mass_high", 0, 15);
    public static final IntegerProperty MASS_LOW = IntegerProperty.create("mass_low", 0, 15);

    private final float massMultiplier;

    public CopyBlockBase(Properties properties) {
        this(properties, 1.0f);
    }

    public CopyBlockBase(Properties properties, float massMultiplier) {
        super(properties);
        this.massMultiplier = massMultiplier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MASS_HIGH, 0)
                .setValue(MASS_LOW, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MASS_HIGH, MASS_LOW);
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
        return massMultiplier;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return ICopyBlock.super.getExplosionResistance(state, level, pos, explosion);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return ICopyBlock.super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public float getDestroySpeed(BlockState state, BlockGetter level, BlockPos pos) {
        return ICopyBlock.super.getDestroySpeed(state, level, pos);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    // ========== HELPER: CLEAN ITEMSTACK ==========
    public static ItemStack cleanStack(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().isEmpty()) {
            stack.setTag(null);
        }

        var nbt = stack.serializeNBT();
        if (nbt.contains("ForgeCaps")) {
            nbt.remove("ForgeCaps");
            stack.deserializeNBT(nbt);
        }

        return stack;
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

        // Shift + empty hand = remove copied block and drop
        if (player.isShiftKeyDown() && heldItem.isEmpty() && !currentCopied.isAir()) {

            ItemStack droppedItem = new ItemStack(currentCopied.getBlock());
            droppedItem.setTag(null);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, droppedItem));

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

    // ==================== MASS ENCODING METHODS ====================
    public static int encodeMass(double mass) {
        mass = Math.max(0, Math.min(4400, mass));
        if (mass < 50) return (int) mass;
        else if (mass < 150) return 50 + (int) ((mass - 50) / 2);
        else if (mass < 400) return 100 + (int) ((mass - 150) / 5);
        else if (mass < 900) return 150 + (int) ((mass - 400) / 10);
        else return Math.min(255, 200 + (int) ((mass - 900) / 50));
    }

    public static double decodeMass(int encoded) {
        if (encoded < 50) return encoded;
        else if (encoded < 100) return 50 + (encoded - 50) * 2.0;
        else if (encoded < 150) return 150 + (encoded - 100) * 5.0;
        else if (encoded < 200) return 400 + (encoded - 150) * 10.0;
        else return 900 + (encoded - 200) * 50.0;
    }

    public static double decodeMass(BlockState state) {
        int high = state.getValue(MASS_HIGH);
        int low = state.getValue(MASS_LOW);
        int encoded = high * 16 + low;
        return decodeMass(encoded);
    }

    public static BlockState setMass(BlockState state, double mass) {
        int encoded = encodeMass(mass);
        int high = encoded / 16;
        int low = encoded % 16;
        return state.setValue(MASS_HIGH, high).setValue(MASS_LOW, low);
    }
}