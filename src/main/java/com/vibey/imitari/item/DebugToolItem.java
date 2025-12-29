package com.vibey.imitari.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

public class DebugToolItem extends Item {

    public DebugToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);

        // Get explosion resistance the way the game does it
        float explosionResistance = state.getExplosionResistance(
                context.getLevel(),
                pos,
                null  // null explosion
        );

        // Get destroy speed (hardness) the way the game does it
        float destroySpeed = state.getDestroySpeed(context.getLevel(), pos);

        // Send info to player
        context.getPlayer().sendSystemMessage(Component.literal("=== Block Debug Info ==="));
        context.getPlayer().sendSystemMessage(Component.literal("Block: " + state.getBlock().getName().getString()));
        context.getPlayer().sendSystemMessage(Component.literal("Explosion Resistance: " + explosionResistance));
        context.getPlayer().sendSystemMessage(Component.literal("Destroy Speed (hardness): " + destroySpeed));

        // Extra info for CopyBlockLayer
        if (state.hasProperty(com.vibey.imitari.block.CopyBlockLayer.LAYERS)) {
            int layers = state.getValue(com.vibey.imitari.block.CopyBlockLayer.LAYERS);
            context.getPlayer().sendSystemMessage(Component.literal("Layers: " + layers));
        }

        // Check if it has copied block
        var be = context.getLevel().getBlockEntity(pos);
        if (be instanceof com.vibey.imitari.blockentity.CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (!copiedState.isAir()) {
                context.getPlayer().sendSystemMessage(Component.literal("Copied Block: " + copiedState.getBlock().getName().getString()));

                float copiedResistance = copiedState.getBlock().getExplosionResistance();
                float copiedSpeed = copiedState.getDestroySpeed(context.getLevel(), pos);

                context.getPlayer().sendSystemMessage(Component.literal("Copied Explosion Resistance: " + copiedResistance));
                context.getPlayer().sendSystemMessage(Component.literal("Copied Destroy Speed: " + copiedSpeed));
            }
        }

        return InteractionResult.SUCCESS;
    }
}