package com.vibey.copycraft.block;

import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A custom block that can copy the appearance of other full-cube blocks.
 * The CopyBlock stores a reference to another block's state in its BlockEntity.
 */
public class CopyBlock extends Block implements EntityBlock {

    public CopyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Invisible block; rendering handled by BlockEntityRenderer
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopyBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopyBlockEntity copyBlockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);

        // Sneaking with empty hand clears the copied texture
        if (player.isShiftKeyDown() && heldItem.isEmpty()) {
            copyBlockEntity.setCopiedBlock(Blocks.AIR.defaultBlockState());
            player.displayClientMessage(Component.literal("Cleared copied texture"), true);
            return InteractionResult.SUCCESS;
        }

        // If holding a block item, copy its texture
        if (heldItem.getItem() instanceof BlockItem blockItem) {
            Block targetBlock = blockItem.getBlock();

            // Prevent copying itself
            if (targetBlock instanceof CopyBlock) {
                player.displayClientMessage(Component.literal("Cannot copy a Copy Block!"), true);
                return InteractionResult.FAIL;
            }

            BlockState targetState = targetBlock.defaultBlockState();

            // Only allow full cube blocks
            if (!targetState.isCollisionShapeFullBlock(level, pos)) {
                player.displayClientMessage(Component.literal("Can only copy full block textures!"), true);
                return InteractionResult.FAIL;
            }

            copyBlockEntity.setCopiedBlock(targetState);
            level.sendBlockUpdated(pos, state, state, 3);

            player.displayClientMessage(
                    Component.literal("Copied texture from: " + targetBlock.getName().getString()),
                    true
            );

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        // This block has no ticking logic
        return null;
    }
}
