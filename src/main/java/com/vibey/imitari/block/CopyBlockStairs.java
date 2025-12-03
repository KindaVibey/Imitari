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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Stairs-sized CopyBlock variant (0.75x multiplier)
 * Now properly extends Minecraft's StairBlock for full compatibility.
 */
public class CopyBlockStairs extends StairBlock implements EntityBlock, ICopyBlock {
    public static final IntegerProperty MASS_HIGH = IntegerProperty.create("mass_high", 0, 15);
    public static final IntegerProperty MASS_LOW = IntegerProperty.create("mass_low", 0, 15);

    private final float massMultiplier;

    public CopyBlockStairs(Properties properties) {
        this(properties, 0.75f);
    }

    public CopyBlockStairs(Properties properties, float massMultiplier) {
        // Pass a dummy state supplier - we don't use it for textures anyway
        super(() -> Blocks.OAK_PLANKS.defaultBlockState(), properties);
        this.massMultiplier = massMultiplier;
        // Note: registerDefaultState is called in createBlockStateDefinition
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MASS_HIGH, MASS_LOW);
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

    // ========== INTERACTION ==========
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopyBlockEntity copyBlockEntity)) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        BlockState currentCopied = copyBlockEntity.getCopiedBlock();

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
        // CRITICAL: Call super to properly remove the BlockEntity
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

    // VS2 collision settings
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }
}